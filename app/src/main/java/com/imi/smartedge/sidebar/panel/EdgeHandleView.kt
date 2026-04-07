package com.imi.smartedge.sidebar.panel

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

    private val triggerThreshold = 16 * resources.displayMetrics.density
    private val holdDurationMs = 250L

    private val holdRunnable = Runnable {
        if (!hasPassedThreshold) return@Runnable
        isTriggered = true
        vibrateHaptic()
        onTrigger?.invoke()
        if (showPill) {
            animate().scaleX(1f).scaleY(1f).setDuration(150).start()
        }
    }

    // Tap Detection
    private var tapCount = 0
    private val tapTimeoutMs = android.view.ViewConfiguration.getDoubleTapTimeout().toLong()
    private val tapRunnable = Runnable {
        if (tapCount == 1 && panelPrefs.tapToOpen) {
            triggerPanel()
        } else if (tapCount == 2 && panelPrefs.doubleTapToOpen) {
            triggerPanel()
        } else if (tapCount >= 3 && panelPrefs.tripleTapToOpen) {
            triggerPanel()
        }
        tapCount = 0
    }

    private fun triggerPanel() {
        vibrateHaptic()
        onTrigger?.invoke()
    }

    private fun handleTap() {
        tapCount++
        handler.removeCallbacks(tapRunnable)

        val maxEnabled = when {
            panelPrefs.tripleTapToOpen -> 3
            panelPrefs.doubleTapToOpen -> 2
            panelPrefs.tapToOpen -> 1
            else -> 0
        }

        if (maxEnabled == 0) {
            tapCount = 0
            return
        }

        if (tapCount >= maxEnabled) {
            triggerPanel()
            tapCount = 0
        } else {
            handler.postDelayed(tapRunnable, tapTimeoutMs)
        }
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setWillNotDraw(false)
        post { updatePill() }
    }

    fun updatePill() {
        if (showPill) {
            val drawableRes = if (isRightSide) R.drawable.bg_pill_handle_right
                             else R.drawable.bg_pill_handle_left
            val insetDrawable = context.getDrawable(drawableRes)?.mutate() as? android.graphics.drawable.InsetDrawable

            if (insetDrawable != null) {
                val density = resources.displayMetrics.density
                val triggerWidthDp = panelPrefs.handleWidth 
                val pillWidthDp = panelPrefs.pillWidth
                val insetDp = (triggerWidthDp - pillWidthDp).coerceAtLeast(0)
                val insetPx = (insetDp * density).toInt()

                // Update insets programmatically
                val baseShape = insetDrawable.drawable?.mutate() ?: return
                val newInset = if (isRightSide) {
                    android.graphics.drawable.InsetDrawable(baseShape, insetPx, 0, 0, 0)
                } else {
                    android.graphics.drawable.InsetDrawable(baseShape, 0, 0, insetPx, 0)
                }
                background = newInset
            } else {
                setBackgroundResource(drawableRes)
            }

            try {
                val color = Color.parseColor(panelPrefs.pillColor)
                backgroundTintList = ColorStateList.valueOf(color)
            } catch (e: Exception) {
                backgroundTintList = null
            }
            alpha = panelPrefs.panelOpacity / 100f
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
        invalidate()
    }

    private var downTime = 0L

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (onTrigger == null) return false 

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                downTime = System.currentTimeMillis()
                hasPassedThreshold = false
                isTriggered = false
                if (showPill && panelPrefs.gesturesEnabled) {
                    animate().scaleX(0.85f).scaleY(0.95f).setDuration(100).start()
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!panelPrefs.gesturesEnabled || isTriggered) return true
                val dx = if (isRightSide) (startX - event.rawX) else (event.rawX - startX)

                if (!hasPassedThreshold && dx > triggerThreshold) {
                    hasPassedThreshold = true
                    handler.postDelayed(holdRunnable, holdDurationMs)
                    if (showPill) {
                        animate().scaleX(0.7f).scaleY(0.9f).setDuration(holdDurationMs).start()
                    }
                }

                // Only cancel if they swipe significantly back towards the edge (less than 4dp)
                if (hasPassedThreshold && dx < 4 * resources.displayMetrics.density) {
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
                if (showPill && !isTriggered && panelPrefs.gesturesEnabled) {
                    animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                }
                
                if (!hasPassedThreshold && !isTriggered && event.action == MotionEvent.ACTION_UP) {
                    val duration = System.currentTimeMillis() - downTime
                    if (duration < android.view.ViewConfiguration.getTapTimeout()) {
                        handleTap()
                    }
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
