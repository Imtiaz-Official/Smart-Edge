package com.originpanel.sidepanel

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration

class EdgeHandleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    /** Fired when a valid inward swipe AND hold is detected. */
    var onTrigger: (() -> Unit)? = null

    /** Whether the handle is on the right side of the screen. */
    var isRightSide: Boolean = true

    /** Whether to show a visible pill handle at the edge. */
    var showPill: Boolean = true
        set(value) {
            field = value
            updatePill()
        }

    private val panelPrefs = PanelPreferences(context)

    // How long the user must hold the swipe before the panel opens (ms)
    private val holdDurationMs = 300L

    private var startX = 0f
    private var startY = 0f
    private var hasPassedThreshold = false
    private var isTriggered = false

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    // Threshold: user must drag at least 60dp inward to "arm" the hold timer
    private val triggerThreshold = 60 * resources.displayMetrics.density

    private val handler = Handler(Looper.getMainLooper())
    private val holdRunnable = Runnable {
        // Timer fired — user held long enough. Open the panel!
        isTriggered = true
        vibrateHaptic()
        onTrigger?.invoke()
        if (showPill) {
            animate().scaleX(1f).scaleY(1f).setDuration(150).start()
        }
    }

    init {
        // Hardware layer for smooth touch response
        setLayerType(LAYER_TYPE_HARDWARE, null)
        updatePill()
    }

    fun updatePill() {
        if (showPill) {
            setBackgroundResource(
                if (isRightSide) R.drawable.bg_pill_handle_right
                else R.drawable.bg_pill_handle_left
            )
            
            // Apply accent color if premium
            if (panelPrefs.isPremium) {
                try {
                    val color = Color.parseColor(panelPrefs.accentColor)
                    backgroundTintList = ColorStateList.valueOf(color)
                } catch (e: Exception) {}
            } else {
                backgroundTintList = null
            }
            
            alpha = (panelPrefs.panelOpacity / 100f) * 0.8f
        } else {
            setBackgroundResource(0)
            alpha = 1f
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (onTrigger == null) return false // No interactions in preview mode

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                hasPassedThreshold = false
                isTriggered = false
                if (showPill) {
                    animate().scaleX(0.85f).scaleY(0.95f).setDuration(100).start()
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isTriggered) return true

                val dx = if (isRightSide) (startX - event.rawX) else (event.rawX - startX)

                // Arm the hold timer once the user has swiped past the threshold
                if (!hasPassedThreshold && dx > triggerThreshold) {
                    hasPassedThreshold = true
                    // Start the hold countdown
                    handler.postDelayed(holdRunnable, holdDurationMs)

                    // Visual feedback: subtle pulse while holding
                    if (showPill) {
                        animate().scaleX(0.75f).scaleY(0.9f).setDuration(holdDurationMs).start()
                    }
                }

                // If user swipes back (cancels intent), disarm
                if (hasPassedThreshold && dx < triggerThreshold / 2) {
                    hasPassedThreshold = false
                    handler.removeCallbacks(holdRunnable)
                    if (showPill) {
                        animate().scaleX(0.85f).scaleY(0.95f).setDuration(80).start()
                    }
                }

                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Always cancel the hold timer on finger lift
                handler.removeCallbacks(holdRunnable)

                if (showPill) {
                    animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                }

                hasPassedThreshold = false
                return !isTriggered 
            }
        }
        return super.onTouchEvent(event)
    }

    private fun vibrateHaptic() {
        if (!panelPrefs.hapticEnabled) return
        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(25)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            systemGestureExclusionRects = listOf(Rect(0, 0, width, height))
        }
    }
}
