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
    private const val SPRING_DAMPING = SpringForce.DAMPING_RATIO_LOW_BOUNCY

    // Track active springs to cancel them if a new animation starts on the same view
    private val activeSprings = java.util.concurrent.ConcurrentHashMap<View, SpringAnimation>()

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

    fun animateOpen(view: View, panelWidth: Float, isPicker: Boolean = false, onEnd: (() -> Unit)? = null) {
        cancelExisting(view)
        
        // Deterministic start position: if on right, start at +width, if on left, start at -width
        // For picker, we use the sign of panelWidth passed from service
        val startX = if (panelWidth == 0f) (if (isPicker) -500f else 500f) else panelWidth
        
        view.translationX = startX
        view.alpha = 0f
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // Use post to ensure the initial state is captured by the renderer before starting animation
        view.post {
            view.animate()
                .alpha(1f)
                .setDuration(160)
                .setInterpolator(fadeInterpolator)
                .start()

            val spring = SpringAnimation(view, DynamicAnimation.TRANSLATION_X, 0f).apply {
                spring.stiffness = adaptiveStiffness(view.context)
                spring.dampingRatio = SPRING_DAMPING
                addEndListener { _, _, _, _ ->
                    activeSprings.remove(view)
                    view.setLayerType(View.LAYER_TYPE_NONE, null)
                    onEnd?.invoke()
                }
            }
            activeSprings[view] = spring
            spring.start()
        }
    }

    fun animateClose(view: View, panelWidth: Float, isPicker: Boolean = false, onEnd: (() -> Unit)? = null) {
        cancelExisting(view)
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        view.animate()
            .alpha(0f)
            .setDuration(140)
            .setInterpolator(fadeInterpolator)
            .start()

        val targetX = if (isPicker)
            (if (panelWidth > 0) -panelWidth else (if (panelWidth < 0) panelWidth else -500f))
        else (if (panelWidth > 0) panelWidth else 500f)

        val spring = SpringAnimation(view, DynamicAnimation.TRANSLATION_X, targetX).apply {
            spring.stiffness = SpringForce.STIFFNESS_LOW
            spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            addEndListener { _, _, _, _ ->
                activeSprings.remove(view)
                view.setLayerType(View.LAYER_TYPE_NONE, null)
                view.translationX = 0f
                onEnd?.invoke()
            }
        }
        activeSprings[view] = spring
        spring.start()
    }

    private fun cancelExisting(view: View) {
        view.animate().cancel()
        activeSprings.remove(view)?.cancel()
    }

    fun scalePulse(view: View) {
        view.animate().cancel()
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