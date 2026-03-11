package com.originpanel.sidepanel

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat

/**
 * An INVISIBLE touch strip at the screen edge that detects a quick inward swipe.
 *
 * OriginOS-style behavior:
 * - No visible pill or handle at rest — the screen looks completely normal
 * - A very thin (4dp) transparent strip sits at the edge at all times
 * - A quick inward fling from the strip opens the panel
 * - No long-press required (that's the Xiaomi/MIUI behavior)
 * - setSystemGestureExclusionRects() prevents conflict with Android 10+ back gesture
 *
 * WindowManager should give this view:
 *   width  = 4dp  (just enough to be a reliable touch target from edge)
 *   height = ~60% of screen height (mid section, avoids status/nav bar conflicts)
 *   TYPE_APPLICATION_OVERLAY, FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL
 */
class EdgeHandleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    /** Fired when a valid inward swipe is detected. */
    var onTrigger: (() -> Unit)? = null

    /** Whether the handle is on the right side of the screen. */
    var isRightSide: Boolean = true

    /** Whether to show a visible pill handle at the edge. */
    var showPill: Boolean = true
        set(value) {
            field = value
            updatePill()
        }

    private fun updatePill() {
        if (showPill) {
            setBackgroundResource(if (isRightSide) R.drawable.bg_pill_handle_right 
                                  else R.drawable.bg_pill_handle_left)
            alpha = 0.6f
        } else {
            setBackgroundResource(0)
            alpha = 1f
        }
    }

    // Minimum horizontal swipe velocity to qualify as "intentional"
    private val MIN_FLING_VELOCITY = 400f   // dp/s
    // Minimum horizontal distance (dp) to confirm directional intent
    private val MIN_SWIPE_DISTANCE_DP = 25f
    private val minSwipePx = MIN_SWIPE_DISTANCE_DP * context.resources.displayMetrics.density

    private val gestureDetector = GestureDetectorCompat(context,
        object : GestureDetector.SimpleOnGestureListener() {

            override fun onDown(e: MotionEvent): Boolean {
                if (showPill) {
                    animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start()
                }
                return true
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                triggerPanel()
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val startX = e1?.rawX ?: return false
                val endX   = e2.rawX
                
                // Use raw coordinates to avoid issues with local coordinates in a 24dp window
                val dx = if (isRightSide) (startX - endX) else (endX - startX)

                val isFastEnough = Math.abs(velocityX) >= MIN_FLING_VELOCITY
                val isFarEnough  = dx >= minSwipePx
                val isHorizontal = Math.abs(velocityX) > Math.abs(velocityY) * 1.5f

                if (isFastEnough && isFarEnough && isHorizontal) {
                    triggerPanel()
                    return true
                }
                return false
            }
        }
    ).also {
        it.setIsLongpressEnabled(false)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val result = gestureDetector.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            if (showPill) {
                animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }
        }
        return result
    }

    /**
     * Exclude this strip from system gesture regions (Android 10+).
     * Without this, the Android system "back" gesture swallows our swipe.
     */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            systemGestureExclusionRects = listOf(Rect(0, 0, width, height))
        }
    }

    private fun triggerPanel() {
        vibrateHaptic()
        onTrigger?.invoke()
    }

    private fun vibrateHaptic() {
        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        v.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
