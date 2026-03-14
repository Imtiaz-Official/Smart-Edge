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
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration

class EdgeHandleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onTrigger: (() -> Unit)? = null
    var isRightSide: Boolean = true
    var showPill: Boolean = true
        set(value) {
            field = value
            updatePill()
        }

    private val panelPrefs = PanelPreferences(context)
    private val handler = Handler(Looper.getMainLooper())

    private var startX = 0f
    private var startY = 0f
    private var hasPassedThreshold = false
    private var isTriggered = false

    private val triggerThreshold = 24 * resources.displayMetrics.density
    private val holdDurationMs = 300L

    private val holdRunnable = Runnable {
        // Guard: if the user pulled back before the delay expired, don't open
        if (!hasPassedThreshold) return@Runnable
        isTriggered = true
        vibrateHaptic()
        onTrigger?.invoke()
        if (showPill) {
            animate().scaleX(1f).scaleY(1f).setDuration(150).start()
        }
    }

    // Tap Detection
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (panelPrefs.tapToOpen) {
                vibrateHaptic()
                onTrigger?.invoke()
                return true
            }
            return false
        }
    })

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        post { updatePill() }
    }

    fun updatePill() {
        if (showPill) {
            setBackgroundResource(
                if (isRightSide) R.drawable.bg_pill_handle_right
                else R.drawable.bg_pill_handle_left
            )
            val density = resources.displayMetrics.density
            layoutParams?.width = (panelPrefs.pillWidth * density).toInt()
            try {
                val color = Color.parseColor(panelPrefs.pillColor)
                backgroundTintList = ColorStateList.valueOf(color)
            } catch (e: Exception) {
                backgroundTintList = null
            }
            alpha = (panelPrefs.panelOpacity / 100f) * 0.8f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                post { systemGestureExclusionRects = listOf(Rect(0, 0, width, height)) }
            }
        } else {
            setBackgroundResource(0)
            alpha = 1f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                systemGestureExclusionRects = listOf(Rect(0, 0, width, height))
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (onTrigger == null) return false 

        // Let GestureDetector handle taps
        gestureDetector.onTouchEvent(event)

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
                if (!hasPassedThreshold && dx > triggerThreshold) {
                    hasPassedThreshold = true
                    handler.postDelayed(holdRunnable, holdDurationMs)
                    if (showPill) {
                        animate().scaleX(0.75f).scaleY(0.9f).setDuration(holdDurationMs).start()
                    }
                }
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
                handler.removeCallbacks(holdRunnable)
                if (showPill) {
                    animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                }
                hasPassedThreshold = false
                return true
            }
        }
        return true
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

    fun updateFromPrefs() {
        val prefs = PanelPreferences(context)
        isRightSide = prefs.panelSide == PanelPreferences.SIDE_RIGHT
        showPill = prefs.showPill
        alpha = prefs.panelOpacity / 100f
        
        // Refresh the handle's WindowManager parameters (for vertical offset/width/height)
        val params = layoutParams as? android.view.WindowManager.LayoutParams
        if (params != null) {
            params.y = (prefs.handleVerticalOffset * resources.displayMetrics.density).toInt()
            params.width = (prefs.handleWidth * resources.displayMetrics.density).toInt()
            params.height = if (showPill) (prefs.handleHeight * resources.displayMetrics.density).toInt()
                            else (resources.displayMetrics.heightPixels * 0.60f).toInt()
            
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
            if (isAttachedToWindow) {
                wm.updateViewLayout(this, params)
            }
        }
        updatePill()
        invalidate()
    }
}
