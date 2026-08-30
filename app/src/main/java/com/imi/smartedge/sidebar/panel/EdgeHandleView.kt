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
import android.view.Gravity

class EdgeHandleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onTrigger: (() -> Unit)? = null
    var onAdjustBrightness: ((delta: Int) -> Unit)? = null
    var onAdjustVolume: ((delta: Int) -> Unit)? = null
    var onSideChanged: ((newSide: String) -> Unit)? = null
    
    var isRightSide: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                updatePill()
            }
        }
    var showPill: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                updatePill()
            }
        }
    var isImmersiveMode: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updatePill()
            }
        }
    var isGameActive: Boolean = false

    private val panelPrefs = PanelPreferences(context)
    private val handler = Handler(Looper.getMainLooper())

    private var startX = 0f
    private var startY = 0f
    private var lastSlideY = 0f
    private var accumulatedDy = 0f
    private var isSlidingSeek = false
    private var isSlidingVolume = false
    private var isTopHalf = false
    private var hasPassedThreshold = false
    private var isTriggered = false

    private var isTempHighAlpha = false
    private var lastPillState: String? = null

    private val density = resources.displayMetrics.density
    private val triggerThreshold = 16 * density
    private val holdDurationMs = 250L

    // ── Drag-to-reposition state ──────────────────────────────────────────────
    private var isDragMode = false
    private var dragStartRawY = 0f
    private var dragStartWindowY = 0f    // WindowManager params.y at drag start
    private var dragStartRawX = 0f
    private var lastMoveRawY = 0f
    private var lastMoveRawX = 0f

    /** Long-press runnable: performs action */
    private val longPressRunnable = Runnable {
        if (panelPrefs.longPressAction != PanelPreferences.ACTION_NONE) {
            performAction(panelPrefs.longPressAction)
            isTriggered = true
            // If we didn't enter drag mode, vibrate and reset scale
            if (!isDragMode) {
                vibrateHaptic(40)
                animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }
        }
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

    private val resetAlphaRunnable = Runnable {
        isTempHighAlpha = false
        alpha = panelPrefs.panelOpacity / 100f
        updatePill()
    }

    fun showTemporarily() {
        isTempHighAlpha = true
        alpha = 1.0f
        handler.removeCallbacks(resetAlphaRunnable)
        handler.postDelayed(resetAlphaRunnable, 3000)
    }

    private fun performAction(actionId: Int) {
        ActionDispatcher.performAction(
            context = context,
            actionId = actionId,
            panelPrefs = panelPrefs,
            onTriggerPanel = { triggerPanel() },
            onDragHandle = { enterDragMode() }
        )
    }

    private fun enterDragMode() {
        isDragMode = true
        vibrateHaptic(40)

        val params = layoutParams as? WindowManager.LayoutParams
        if (params != null) {
            dragStartWindowY = params.y.toFloat()
            dragStartRawY = lastMoveRawY
            dragStartRawX = lastMoveRawX
        }

        // Grow the pill slightly to signal drag mode
        animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).start()
    }

    private fun triggerPanel() {
        vibrateHaptic()
        onTrigger?.invoke()
    }

    private fun handleTap() {        tapCount++
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
            if (isDragMode) return@setOnApplyWindowInsetsListener insets
            
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
                    val maxOffset = ((screenHeight / 2) - (h / 2) - safeMargin).coerceAtLeast(0)
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

    fun updateState(isRight: Boolean, isPill: Boolean, immersive: Boolean, opacity: Int) {
        isRightSide = isRight
        showPill = isPill
        isImmersiveMode = immersive
        if (!isTempHighAlpha) {
            alpha = opacity / 100f
        }
        updatePill()
    }

    fun updatePill() {
        val currentPkg = panelPrefs.currentForegroundPackage
        val hidePillInCurrentApp = panelPrefs.autoHideInFullscreen && panelPrefs.isWhitelistedFromAutoHide(currentPkg)
        
        // Build a unique key for the current visual state to prevent redundant updates
        val stateKey = "${isRightSide}_${showPill}_${hidePillInCurrentApp}_${panelPrefs.pillColor}_${panelPrefs.handleWidth}_${panelPrefs.pillWidth}_${panelPrefs.panelOpacity}_${isImmersiveMode}"
        if (stateKey == lastPillState) return
        lastPillState = stateKey

        if (showPill && !hidePillInCurrentApp) {
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
            
            if (!isTempHighAlpha) {
                alpha = panelPrefs.panelOpacity / 100f
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                post { systemGestureExclusionRects = listOf(Rect(0, 0, width, height)) }
            }
        } else {
            background = null
            alpha = 0f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                systemGestureExclusionRects = listOf(Rect(0, 0, width, height))
            }
        }
        invalidate()
    }

    private var downTime = 0L

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (onTrigger == null) return false
        
        val hidePillInCurrentApp = panelPrefs.autoHideInFullscreen && panelPrefs.isWhitelistedFromAutoHide(panelPrefs.currentForegroundPackage)
        if (hidePillInCurrentApp && !isDragMode) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                lastMoveRawX = event.rawX
                lastMoveRawY = event.rawY
                lastSlideY = event.rawY
                accumulatedDy = 0f
                isSlidingSeek = false
                isSlidingVolume = false
                isTopHalf = event.y < height / 2
                dragStartRawY = event.rawY
                downTime = System.currentTimeMillis()
                hasPassedThreshold = false
                isTriggered = false
                isDragMode = false

                // Schedule long-press → perform action
                handler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())

                if (showPill && panelPrefs.gesturesEnabled) {
                    animate().scaleX(0.85f).scaleY(0.95f).setDuration(100).start()
                }

                // Record current window Y for drag baseline (fallback)
                val params = layoutParams as? WindowManager.LayoutParams
                dragStartWindowY = params?.y?.toFloat() ?: 0f

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                lastMoveRawX = event.rawX
                lastMoveRawY = event.rawY
                val totalDy = event.rawY - dragStartRawY
                val currentY = event.rawY
                val dySinceLast = currentY - lastSlideY
                lastSlideY = currentY

                // ── Drag-reposition mode ──────────────────────────────────────
                if (isDragMode) {
                    val params = layoutParams as? WindowManager.LayoutParams
                    if (params != null) {
                        val screenH = resources.displayMetrics.heightPixels
                        val safeMargin = (10 * density).toInt()
                        
                        // Relative movement: NewPos = StartPos + (CurrentFinger - StartFinger)
                        val dy = event.rawY - dragStartRawY
                        val maxOffset = ((screenH / 2f) - (height / 2f) - safeMargin).coerceAtLeast(0f)
                        
                        val newY = (dragStartWindowY + dy).toInt()
                            .coerceIn(-maxOffset.toInt(), maxOffset.toInt())
                        
                        if (params.y != newY) {
                            params.y = newY
                            updateLayoutSafely(params)
                        }
                    }

                    // Flip side based on absolute screen position
                    val screenW = resources.displayMetrics.widthPixels
                    val leftThreshold = screenW * 0.35f
                    val rightThreshold = screenW * 0.65f
                    
                    if (isRightSide && event.rawX < leftThreshold) {
                        flipSide(PanelPreferences.SIDE_LEFT)
                        // Reset drag start X when flipping to prevent immediate flip back
                        dragStartRawX = event.rawX 
                    } else if (!isRightSide && event.rawX > rightThreshold) {
                        flipSide(PanelPreferences.SIDE_RIGHT)
                        dragStartRawX = event.rawX
                    }
                    return true
                }

                // ── Slide Seek Gesture (Volume/Brightness) ────────────────────
                val slideEnabled = panelPrefs.slideBrightnessEnabled || panelPrefs.slideVolumeEnabled
                if (slideEnabled && !hasPassedThreshold && !isTriggered) {
                    val absDx = Math.abs(event.rawX - startX)
                    val absDyFromStart = Math.abs(currentY - startY)

                    if (!isSlidingSeek && absDyFromStart > triggerThreshold / 2 && absDyFromStart > absDx * 2) {
                        // Volume/Brightness takes priority OVER the open-panel gesture
                        val volumeOn = panelPrefs.slideVolumeEnabled
                        val brightnessOn = panelPrefs.slideBrightnessEnabled
                        
                        isSlidingVolume = when {
                            volumeOn && brightnessOn -> isTopHalf 
                            volumeOn -> true
                            else -> false
                        }
                        
                        isSlidingSeek = true
                        accumulatedDy = 0f 
                        handler.removeCallbacks(longPressRunnable)
                        handler.removeCallbacks(holdRunnable)
                        vibrateHaptic(10)
                    }

                    if (isSlidingSeek) {
                        accumulatedDy += dySinceLast
                        val sensitivity = panelPrefs.slideSensitivity.coerceIn(1, 200)
                        val multiplier = 100f / sensitivity
                        
                        if (isSlidingVolume) {
                            val pixelsPerUnit = (25f * density * multiplier).coerceAtLeast(1f)
                            if (Math.abs(accumulatedDy) >= pixelsPerUnit) {
                                val units = (accumulatedDy / pixelsPerUnit).toInt()
                                onAdjustVolume?.invoke(-units)
                                accumulatedDy -= units * pixelsPerUnit
                            }
                        } else {
                            val pixelsPerUnit = (3f * density * multiplier).coerceAtLeast(1f)
                            if (Math.abs(accumulatedDy) >= pixelsPerUnit) {
                                val units = (accumulatedDy / pixelsPerUnit).toInt()
                                onAdjustBrightness?.invoke(-units)
                                accumulatedDy -= units * pixelsPerUnit
                            }
                        }
                        return true
                    }
                }

                // ── Normal panel-open gesture ─────────────────────────────────
                if (!panelPrefs.gesturesEnabled || isTriggered) return true
                val dx = if (isRightSide) (startX - event.rawX) else (event.rawX - startX)
                
                // Sensitivity scaling: 100% = base (16dp), 200% = 8dp, 50% = 32dp
                val sensitivity = panelPrefs.swipeSensitivity.coerceIn(10, 300)
                val baseThreshold = 16 * density
                val scaledThreshold = baseThreshold * (100f / sensitivity)
                
                val effectiveThreshold = if (isGameActive && panelPrefs.deliberateGestureInGames) {
                    scaledThreshold * 2.5f 
                } else {
                    scaledThreshold
                }

                // Tolerance for horizontal movement before cancelling long-press
                // We double it to allow for more natural finger roll/wobble when trying to move handle
                if (dx > triggerThreshold * 2.5f) {
                    handler.removeCallbacks(longPressRunnable)
                }
                
                // Tolerance for vertical movement before cancelling long-press
                // Very generous vertical tolerance (4x) so users can start moving without cancellation
                if (!hasPassedThreshold && Math.abs(totalDy) > triggerThreshold * 4f && Math.abs(totalDy) > Math.abs(event.rawX - startX) * 2f) {
                    handler.removeCallbacks(longPressRunnable)
                }

                if (!hasPassedThreshold && dx > effectiveThreshold) {
                    hasPassedThreshold = true
                    handler.removeCallbacks(longPressRunnable)
                    
                    val effectiveHoldTime = if (isGameActive && panelPrefs.deliberateGestureInGames) {
                        holdDurationMs * 2 
                    } else {
                        holdDurationMs
                    }
                    
                    handler.postDelayed(holdRunnable, effectiveHoldTime)
                    if (showPill) {
                        animate().scaleX(0.7f).scaleY(0.9f).setDuration(effectiveHoldTime).start()
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
                handler.removeCallbacks(longPressRunnable)

                if (isDragMode) {
                    saveFinalPosition()
                    isDragMode = false
                    animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                    return true
                }

                if (showPill && !isTriggered && panelPrefs.gesturesEnabled) {
                    animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                }

                if (hasPassedThreshold && !isTriggered) {
                    triggerPanel()
                    isTriggered = true
                }

                if (!hasPassedThreshold && !isTriggered && !isSlidingSeek && event.action == MotionEvent.ACTION_UP) {
                    val duration = System.currentTimeMillis() - downTime
                    if (duration < ViewConfiguration.getLongPressTimeout()) {
                        handleTap()
                    }
                }

                isSlidingSeek = false
                isSlidingVolume = false
                hasPassedThreshold = false
                return true
            }
        }
        return true
    }

    private fun flipSide(newSide: String) {
        if ((newSide == PanelPreferences.SIDE_RIGHT) == isRightSide) return

        vibrateHaptic(30)
        isRightSide = newSide == PanelPreferences.SIDE_RIGHT

        val params = layoutParams as? WindowManager.LayoutParams ?: return
        params.gravity = if (isRightSide) Gravity.END or Gravity.CENTER_VERTICAL
                        else Gravity.START or Gravity.CENTER_VERTICAL

        updateLayoutSafely(params)
        
        // If we are dragging, re-snapshot coordinates because the gravity change
        // might have shifted the relative center of the Y axis on some devices.
        if (isDragMode) {
            dragStartWindowY = params.y.toFloat()
            dragStartRawY = lastMoveRawY
            dragStartRawX = lastMoveRawX
        }

        updatePill()
    }

    private fun saveFinalPosition() {
        val params = layoutParams as? WindowManager.LayoutParams ?: return
        val offsetDp = (params.y / density).toInt()
        panelPrefs.handleVerticalOffset = offsetDp

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
        
        if (!isTempHighAlpha) {
            alpha = prefs.panelOpacity / 100f
        }

        val params = layoutParams as? WindowManager.LayoutParams
        if (params != null) {
            val screenH = resources.displayMetrics.heightPixels
            val safeMargin = (10 * density).toInt()

            val h = if (showPill) (prefs.handleHeight * density).toInt()
                    else (prefs.handleHeight * 1.5f * density).toInt()

            val maxOffset = ((screenH / 2) - (h / 2) - safeMargin).coerceAtLeast(0)
            val requestedOffset = (prefs.handleVerticalOffset * density).toInt()

            params.y = requestedOffset.coerceIn(-maxOffset, maxOffset)
            params.width = (prefs.handleWidth * density).toInt()
            params.height = h

            updateLayoutSafely(params)
        }
        updatePill()
    }
}
