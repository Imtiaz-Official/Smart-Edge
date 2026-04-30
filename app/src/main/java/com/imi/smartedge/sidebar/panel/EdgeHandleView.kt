package com.imi.smartedge.sidebar.panel

import android.content.Context
import android.content.Intent
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
import android.view.WindowManager

class EdgeHandleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onTrigger: (() -> Unit)? = null
    var onSideChanged: ((newSide: String) -> Unit)? = null
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

    private val density = resources.displayMetrics.density
    private val triggerThreshold = 16 * density
    private val holdDurationMs = 250L

    // ── Drag-to-reposition state ──────────────────────────────────────────────
    private var isDragMode = false
    private var dragStartRawY = 0f
    private var dragStartWindowY = 0f    // WindowManager params.y at drag start
    private var dragStartRawX = 0f

    /** Long-press runnable: enters drag-repositioning mode */
    private val dragModeRunnable = Runnable {
        isDragMode = true
        vibrateHaptic(40)
        // Grow the pill slightly to signal drag mode
        animate().scaleX(1.15f).scaleY(1.15f).setDuration(120).start()
    }

    private val holdRunnable = Runnable {
        if (!hasPassedThreshold) return@Runnable
        isTriggered = true
        vibrateHaptic()
        onTrigger?.invoke()
        if (showPill) {
            animate().scaleX(1f).scaleY(1f).setDuration(150).start()
        }
    }

    // ── Tap Detection ─────────────────────────────────────────────────────────
    private var tapCount = 0
    private val tapTimeoutMs = ViewConfiguration.getDoubleTapTimeout().toLong()
    private val tapRunnable = Runnable {
        when (tapCount) {
            1 -> performAction(panelPrefs.tapAction)
            2 -> performAction(panelPrefs.doubleTapAction)
            else -> if (tapCount >= 3) performAction(panelPrefs.tripleTapAction)
        }
        tapCount = 0
    }

    private fun performAction(actionId: Int) {
        when (actionId) {
            PanelPreferences.ACTION_OPEN_LAUNCHER -> triggerPanel()
            PanelPreferences.ACTION_SCREENSHOT -> triggerScreenshot()
            PanelPreferences.ACTION_PREVIOUS_APP -> triggerPreviousApp()
        }
    }

    private fun triggerPanel() {
        vibrateHaptic()
        onTrigger?.invoke()
    }

    private fun triggerScreenshot() {
        vibrateHaptic()
        val intent = Intent(context, PanelAccessibilityService::class.java).apply {
            action = PanelAccessibilityService.ACTION_TAKE_SCREENSHOT
        }
        context.startService(intent)
    }

    private fun triggerPreviousApp() {
        vibrateHaptic()
        val intent = Intent(context, PanelAccessibilityService::class.java).apply {
            action = PanelAccessibilityService.ACTION_PREVIOUS_APP
        }
        context.startService(intent)
    }

    private fun handleTap() {
        tapCount++
        handler.removeCallbacks(tapRunnable)

        // If user reached triple tap, trigger immediately if configured
        if (tapCount >= 3) {
            if (panelPrefs.tripleTapAction != PanelPreferences.ACTION_NONE) {
                performAction(panelPrefs.tripleTapAction)
                tapCount = 0
                return
            }
        }
        
        handler.postDelayed(tapRunnable, tapTimeoutMs)
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setWillNotDraw(false)
        post { 
            updatePill()
            setupImeListener()
        }
    }

    private fun setupImeListener() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val imeVisible = insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom
            
            val params = layoutParams as? WindowManager.LayoutParams
            if (params != null) {
                val screenHeight = resources.displayMetrics.heightPixels
                if (imeVisible && imeHeight > 0) {
                    val keyboardTop = screenHeight - imeHeight
                    val h = height
                    val handleCenterAbsY = (screenHeight / 2) + params.y
                    val handleBottomAbsY = handleCenterAbsY + (h / 2)

                    if (handleBottomAbsY > keyboardTop) {
                        val overlap = handleBottomAbsY - keyboardTop
                        val margin = (32 * density).toInt()
                        val newY = params.y - overlap - margin
                        
                        val animator = android.animation.ValueAnimator.ofInt(params.y, newY.toInt())
                        animator.duration = 200
                        animator.addUpdateListener { animation ->
                            params.y = animation.animatedValue as Int
                            updateLayoutSafely(params)
                        }
                        animator.start()
                    }
                } else {
                    val h = if (showPill) (panelPrefs.handleHeight * density).toInt()
                            else (panelPrefs.handleHeight * 1.5f * density).toInt()
                    val safeMargin = (10 * density).toInt()
                    val maxOffset = (screenHeight / 2) - (h / 2) - safeMargin
                    val targetY = (panelPrefs.handleVerticalOffset * density).toInt().coerceIn(-maxOffset, maxOffset)
                    
                    if (params.y != targetY) {
                        val animator = android.animation.ValueAnimator.ofInt(params.y, targetY)
                        animator.duration = 200
                        animator.addUpdateListener { animation ->
                            params.y = animation.animatedValue as Int
                            updateLayoutSafely(params)
                        }
                        animator.start()
                    }
                }
            }
            insets
        }
    }

    private fun updateLayoutSafely(params: WindowManager.LayoutParams) {
        if (isAttachedToWindow) {
            try {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                wm.updateViewLayout(this, params)
            } catch (e: Exception) {}
        }
    }

    fun updatePill() {
        if (showPill) {
            val cornerRadius = 12 * density
            val shape = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                if (isRightSide) {
                    cornerRadii = floatArrayOf(cornerRadius, cornerRadius, 0f, 0f, 0f, 0f, cornerRadius, cornerRadius)
                } else {
                    cornerRadii = floatArrayOf(0f, 0f, cornerRadius, cornerRadius, cornerRadius, cornerRadius, 0f, 0f)
                }
                
                try {
                    val color = Color.parseColor(panelPrefs.pillColor)
                    setColor(color)
                } catch (e: Exception) {
                    setColor(Color.WHITE)
                }
                
                setStroke((1 * density).toInt(), Color.parseColor("#4DFFFFFF"))
            }

            val triggerWidthDp = panelPrefs.handleWidth
            val pillWidthDp = panelPrefs.pillWidth
            val insetDp = (triggerWidthDp - pillWidthDp).coerceAtLeast(0)
            val insetPx = (insetDp * density).toInt()

            val newInset = if (isRightSide) {
                android.graphics.drawable.InsetDrawable(shape, insetPx, 0, 0, 0)
            } else {
                android.graphics.drawable.InsetDrawable(shape, 0, 0, insetPx, 0)
            }
            background = newInset
            
            alpha = panelPrefs.panelOpacity / 100f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                post { systemGestureExclusionRects = listOf(Rect(0, 0, width, height)) }
            }
        } else {
            background = null
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
                dragStartRawY = event.rawY
                downTime = System.currentTimeMillis()
                hasPassedThreshold = false
                isTriggered = false
                isDragMode = false

                // Schedule long-press → drag mode
                handler.postDelayed(dragModeRunnable, ViewConfiguration.getLongPressTimeout().toLong())

                if (showPill && panelPrefs.gesturesEnabled) {
                    animate().scaleX(0.85f).scaleY(0.95f).setDuration(100).start()
                }

                // Record current window Y for drag baseline
                val params = layoutParams as? WindowManager.LayoutParams
                dragStartWindowY = params?.y?.toFloat() ?: 0f

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val totalDy = event.rawY - dragStartRawY

                // ── Drag-reposition mode ──────────────────────────────────────
                if (isDragMode) {
                    val params = layoutParams as? WindowManager.LayoutParams
                    if (params != null) {
                        val screenH = resources.displayMetrics.heightPixels
                        val safeMargin = (10 * density).toInt()
                        val maxOffset = (screenH / 2) - (height / 2) - safeMargin

                        val newY = (dragStartWindowY + totalDy).toInt().coerceIn(-maxOffset, maxOffset)
                        
                        // Only send updates to WM if the value actually changed to prevent stuttering
                        if (params.y != newY) {
                            params.y = newY
                            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                            if (isAttachedToWindow) {
                                try { wm.updateViewLayout(this, params) } catch (e: Exception) {}
                            }
                        }
                    }

                    // Flip side based on absolute screen position to prevent ping-pong glitching
                    val screenW = resources.displayMetrics.widthPixels
                    val leftThreshold = screenW * 0.3f
                    val rightThreshold = screenW * 0.7f
                    
                    if (isRightSide && event.rawX < leftThreshold) {
                        flipSide(PanelPreferences.SIDE_LEFT)
                    } else if (!isRightSide && event.rawX > rightThreshold) {
                        flipSide(PanelPreferences.SIDE_RIGHT)
                    }
                    return true
                }

                // ── Normal panel-open gesture ─────────────────────────────────
                if (!panelPrefs.gesturesEnabled || isTriggered) return true
                val dx = if (isRightSide) (startX - event.rawX) else (event.rawX - startX)

                // Cancel long-press/drag timer if user clearly moving inward
                if (dx > triggerThreshold) {
                    handler.removeCallbacks(dragModeRunnable)
                }
                // Cancel if moving vertically primarily (user is quickly trying to scroll app behind)
                if (!hasPassedThreshold && Math.abs(totalDy) > triggerThreshold && Math.abs(totalDy) > Math.abs(event.rawX - startX) * 1.5f) {
                    handler.removeCallbacks(dragModeRunnable)
                }

                if (!hasPassedThreshold && dx > triggerThreshold) {
                    hasPassedThreshold = true
                    handler.removeCallbacks(dragModeRunnable)
                    handler.postDelayed(holdRunnable, holdDurationMs)
                    if (showPill) {
                        animate().scaleX(0.7f).scaleY(0.9f).setDuration(holdDurationMs).start()
                    }
                }

                if (hasPassedThreshold && dx < 4 * density) {
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
                handler.removeCallbacks(dragModeRunnable)

                if (isDragMode) {
                    // Save final position to prefs
                    saveFinalPosition()
                    isDragMode = false
                    animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                    return true
                }

                if (showPill && !isTriggered && panelPrefs.gesturesEnabled) {
                    animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                }

                if (!hasPassedThreshold && !isTriggered && event.action == MotionEvent.ACTION_UP) {
                    val duration = System.currentTimeMillis() - downTime
                    if (duration < ViewConfiguration.getLongPressTimeout()) {
                        handleTap()
                    }
                }

                hasPassedThreshold = false
                return true
            }
        }
        return true
    }
    /** Flips the pill to the given side with a smooth animation. */
    private fun flipSide(newSide: String) {
        if ((newSide == PanelPreferences.SIDE_RIGHT) == isRightSide) return

        vibrateHaptic(30)
        isRightSide = newSide == PanelPreferences.SIDE_RIGHT

        // Update the WindowManager gravity immediately
        val params = layoutParams as? WindowManager.LayoutParams ?: return
        params.gravity = if (isRightSide) {
            android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
        } else {
            android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
        }

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (isAttachedToWindow) {
            try { wm.updateViewLayout(this, params) } catch (e: Exception) {}
        }

        // Flip the pill visual (left/right rounded shape)
        updatePill()
    }

    /** Persists the current window Y position and side to preferences. */
    private fun saveFinalPosition() {
        val params = layoutParams as? WindowManager.LayoutParams ?: return
        val offsetDp = (params.y / density).toInt()
        panelPrefs.handleVerticalOffset = offsetDp

        // Safely notify the service that the side changed, keeping layout passes out of the drag loop
        val newSide = if (isRightSide) PanelPreferences.SIDE_RIGHT else PanelPreferences.SIDE_LEFT
        if (panelPrefs.panelSide != newSide) {
            panelPrefs.panelSide = newSide
            onSideChanged?.invoke(newSide)
        }
    }

    private fun vibrateHaptic(durationMs: Long = 25) {
        if (!panelPrefs.hapticEnabled) return
        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(durationMs)
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

        val params = layoutParams as? WindowManager.LayoutParams
        if (params != null) {
            val screenH = resources.displayMetrics.heightPixels
            val safeMargin = (10 * density).toInt()

            val h = if (showPill) (prefs.handleHeight * density).toInt()
                    else (screenH * 0.60f).toInt()

            val maxOffset = (screenH / 2) - (h / 2) - safeMargin
            val requestedOffset = (prefs.handleVerticalOffset * density).toInt()

            params.y = requestedOffset.coerceIn(-maxOffset, maxOffset)
            params.width = (prefs.handleWidth * density).toInt()
            params.height = h

            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            if (isAttachedToWindow) {
                wm.updateViewLayout(this, params)
            }
        }
        updatePill()
        invalidate()
    }
}
