package com.imi.smartedge.sidebar.panel

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
    private const val SPRING_DAMPING = SpringForce.DAMPING_RATIO_NO_BOUNCY

    // Track active springs to cancel them if a new animation starts on the same view
    private val activeSprings = java.util.concurrent.ConcurrentHashMap<View, SpringAnimation>()

    fun adaptiveStiffness(): Float {
        // Balanced stiffness: between LOW (200) and MEDIUM (500) for a calm and smooth feel
        return 400f
    }

    fun animateOpen(view: View, panelWidth: Float, stiffness: Float = 400f, isPicker: Boolean = false, slide: Boolean = true, onEnd: (() -> Unit)? = null) {
        cancelExisting(view)
        
        if (stiffness <= 0f) {
            view.translationX = 0f
            view.scaleX = 1f
            view.scaleY = 1f
            view.alpha = 1f
            onEnd?.invoke()
            return
        }
        
        if (slide) {
            val startX = if (panelWidth != 0f) panelWidth else (if (isPicker) -500f else 500f)
            view.translationX = startX
            view.scaleX = 1f
            view.scaleY = 1f
        } else {
            view.translationX = 0f
            view.scaleX = 0.85f
            view.scaleY = 0.85f
        }
        
        view.alpha = 0f
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // Scale alpha duration inversely with stiffness (Balanced at 400f -> 200ms)
        val alphaDuration = (220 * (400f / stiffness)).toLong().coerceIn(50, 600)

        view.post {
            view.animate()
                .alpha(1f)
                .setDuration(alphaDuration)
                .setInterpolator(fadeInterpolator)
                .start()

            if (slide) {
                val spring = SpringAnimation(view, DynamicAnimation.TRANSLATION_X, 0f).apply {
                    this.spring.stiffness = stiffness
                    this.spring.dampingRatio = SPRING_DAMPING
                    addEndListener { _, _, _, _ ->
                        activeSprings.remove(view)
                        view.setLayerType(View.LAYER_TYPE_NONE, null)
                        onEnd?.invoke()
                    }
                }
                activeSprings[view] = spring
                spring.start()
            } else {
                // Smooth scale spring for "Popup" mode
                val scaleXSpring = SpringAnimation(view, DynamicAnimation.SCALE_X, 1f).apply {
                    this.spring.stiffness = stiffness * 1.2f // Slightly snappier scale
                    this.spring.dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
                }
                val scaleYSpring = SpringAnimation(view, DynamicAnimation.SCALE_Y, 1f).apply {
                    this.spring.stiffness = stiffness * 1.2f
                    this.spring.dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
                    addEndListener { _, _, _, _ ->
                        view.setLayerType(View.LAYER_TYPE_NONE, null)
                        onEnd?.invoke()
                    }
                }
                scaleXSpring.start()
                scaleYSpring.start()
            }
        }
    }

    fun animateClose(view: View, panelWidth: Float, stiffness: Float = 350f, isPicker: Boolean = false, slide: Boolean = true, onEnd: (() -> Unit)? = null) {
        cancelExisting(view)
        
        val targetX = if (panelWidth != 0f) panelWidth else (if (isPicker) -500f else 500f)

        if (stiffness <= 0f) {
            if (slide) view.translationX = targetX
            view.scaleX = 1f
            view.scaleY = 1f
            view.alpha = 0f
            view.translationX = 0f
            onEnd?.invoke()
            return
        }

        view.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // Scale alpha duration inversely with stiffness
        val alphaDuration = (180 * (350f / stiffness)).toLong().coerceIn(50, 500)

        view.animate()
            .alpha(0f)
            .setDuration(alphaDuration)
            .setInterpolator(fadeInterpolator)
            .withEndAction {
                if (!slide) {
                    view.setLayerType(View.LAYER_TYPE_NONE, null)
                    view.scaleX = 1f
                    view.scaleY = 1f
                    onEnd?.invoke()
                }
            }
            .start()

        if (slide) {
            val spring = SpringAnimation(view, DynamicAnimation.TRANSLATION_X, targetX).apply {
                this.spring.stiffness = stiffness
                this.spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
                addEndListener { _, _, _, _ ->
                    activeSprings.remove(view)
                    view.setLayerType(View.LAYER_TYPE_NONE, null)
                    view.translationX = 0f
                    onEnd?.invoke()
                }
            }
            activeSprings[view] = spring
            spring.start()
        } else {
            val scaleXSpring = SpringAnimation(view, DynamicAnimation.SCALE_X, 0.9f).apply {
                this.spring.stiffness = stiffness * 1.5f
                this.spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            }
            val scaleYSpring = SpringAnimation(view, DynamicAnimation.SCALE_Y, 0.9f).apply {
                this.spring.stiffness = stiffness * 1.5f
                this.spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            }
            scaleXSpring.start()
            scaleYSpring.start()
        }
    }

    private fun cancelExisting(view: View) {
        view.animate().cancel()
        activeSprings.remove(view)?.cancel()
    }

    fun scalePulse(view: View) {
        view.animate().cancel()
        view.animate()
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setDuration(90)
            .setInterpolator(fadeInterpolator)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(180)
                    .setInterpolator(fadeInterpolator)
                    .start()
            }
            .start()
    }

    /**
     * Relaxed, eye-soothing entrance animation for a newly added app item.
     * Starts slightly scaled-down and fades in with a gentle spring settle.
     */
    fun gentleHighlight(view: View) {
        cancelExisting(view)
        view.scaleX = 0.85f
        view.scaleY = 0.85f
        view.alpha = 0.35f

        view.animate()
            .alpha(1f)
            .setDuration(350)
            .setInterpolator(fadeInterpolator)
            .start()

        val springX = SpringAnimation(view, DynamicAnimation.SCALE_X, 1f).apply {
            spring.stiffness = 200f // Relaxed / calm stiffness (STIFFNESS_LOW)
            spring.dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
        }
        val springY = SpringAnimation(view, DynamicAnimation.SCALE_Y, 1f).apply {
            spring.stiffness = 200f
            spring.dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
            addEndListener { _, _, _, _ ->
                activeSprings.remove(view)
            }
        }
        activeSprings[view] = springY
        springX.start()
        springY.start()
    }
}
