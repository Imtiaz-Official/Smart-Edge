package com.originpanel.sidepanel

import android.app.ActivityManager
import android.content.Context
import android.view.Choreographer
import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * Utility to create spring-physics animations for the side panel.
 *
 * Improvements:
 *  1. FastOutSlowInInterpolator on alpha fades — matches Material Design motion spec.
 *  2. Adaptive spring stiffness — auto-tunes to device RAM class (flagship/mid/budget).
 *  3. Choreographer-synced start — first frame always aligns to VSYNC, eliminating the
 *     "first-frame flicker" seen on high refresh rate displays (90Hz / 120Hz).
 */
object SpringAnimator {

    private val fadeInterpolator = FastOutSlowInInterpolator()

    // Damping is the same for all devices — only stiffness adapts
    private const val SPRING_DAMPING = SpringForce.DAMPING_RATIO_LOW_BOUNCY

    /**
     * Returns spring stiffness tuned to the device's RAM class.
     *   >= 8 GB RAM → STIFFNESS_HIGH    (flagship — fast & snappy)
     *   >= 4 GB RAM → STIFFNESS_MEDIUM  (mid-range — current default)
     *   <  4 GB RAM → STIFFNESS_MEDIUM_LOW (budget — softer, less demanding)
     */
    fun adaptiveStiffness(context: Context): Float {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val ramGb = memInfo.totalMem / (1024f * 1024f * 1024f)
        return when {
            ramGb >= 8f -> SpringForce.STIFFNESS_HIGH
            ramGb >= 4f -> SpringForce.STIFFNESS_MEDIUM
            else        -> SpringForce.STIFFNESS_LOW
        }
    }

    /**
     * Animates [view] translationX from off-screen to 0 (open panel).
     * Synced to display VSYNC via Choreographer to eliminate first-frame flicker.
     */
    fun animateOpen(view: View, panelWidth: Float, isPicker: Boolean = false, onEnd: (() -> Unit)? = null) {
        // Picker animates opposite to the main panel since it sits on the inner side
        view.translationX = if (isPicker)
            (if (panelWidth > 0) -panelWidth else panelWidth)
        else panelWidth
        
        view.alpha = 0f

        // GPU-backed layer during animation
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // Choreographer ensures the very first frame is VSYNC-aligned
        Choreographer.getInstance().postFrameCallback {
            // Fade in with Material interpolator
            view.animate()
                .alpha(1f)
                .setDuration(160)
                .setInterpolator(fadeInterpolator)
                .start()

            // Spring slide in
            SpringAnimation(view, DynamicAnimation.TRANSLATION_X, 0f).apply {
                spring.stiffness = adaptiveStiffness(view.context)
                spring.dampingRatio = SPRING_DAMPING
                addEndListener { _, _, _, _ ->
                    view.setLayerType(View.LAYER_TYPE_NONE, null)
                    onEnd?.invoke()
                }
                start()
            }
        }
    }

    /**
     * Animates [view] translationX back off-screen (close panel).
     */
    fun animateClose(view: View, panelWidth: Float, isPicker: Boolean = false, onEnd: (() -> Unit)? = null) {
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        view.animate()
            .alpha(0f)
            .setDuration(140)
            .setInterpolator(fadeInterpolator)
            .start()

        val targetX = if (isPicker)
            (if (panelWidth > 0) -panelWidth else panelWidth)
        else panelWidth

        SpringAnimation(view, DynamicAnimation.TRANSLATION_X, targetX).apply {
            spring.stiffness = SpringForce.STIFFNESS_LOW
            spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            addEndListener { _, _, _, _ ->
                view.setLayerType(View.LAYER_TYPE_NONE, null)
                view.translationX = 0f
                onEnd?.invoke()
            }
            start()
        }
    }

    /**
     * Optimized scale pulse for button feedback.
     */
    fun scalePulse(view: View) {
        view.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(60)
            .setInterpolator(fadeInterpolator)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .setInterpolator(fadeInterpolator)
                    .start()
            }
            .start()
    }
}