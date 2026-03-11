package com.originpanel.sidepanel

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

/**
 * Utility to create spring-physics animations for the side panel.
 * Uses DynamicAnimation for natural, physically-based motion.
 */
object SpringAnimator {

    private const val SPRING_STIFFNESS = SpringForce.STIFFNESS_MEDIUM
    private const val SPRING_DAMPING = SpringForce.DAMPING_RATIO_LOW_BOUNCY

    /**
     * Animates [view] translationX from off-screen to 0 (open panel).
     * @param panelWidth Width of the panel in pixels (used for starting position).
     * @param onEnd Called when animation completes.
     */
    fun animateOpen(view: View, panelWidth: Float, onEnd: (() -> Unit)? = null) {
        view.visibility = View.VISIBLE
        view.translationX = panelWidth
        view.alpha = 0f

        // Fade in
        view.animate()
            .alpha(1f)
            .setDuration(200)
            .start()

        // Spring slide in
        SpringAnimation(view, DynamicAnimation.TRANSLATION_X, 0f).apply {
            spring.stiffness = SPRING_STIFFNESS
            spring.dampingRatio = SPRING_DAMPING
            addEndListener { _, _, _, _ -> onEnd?.invoke() }
            start()
        }
    }

    /**
     * Animates [view] translationX back to [panelWidth] (close panel)
     * then hides the view.
     */
    fun animateClose(view: View, panelWidth: Float, onEnd: (() -> Unit)? = null) {
        view.animate()
            .alpha(0f)
            .setDuration(180)
            .start()

        SpringAnimation(view, DynamicAnimation.TRANSLATION_X, panelWidth).apply {
            spring.stiffness = SpringForce.STIFFNESS_LOW
            spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            addEndListener { _, _, _, _ ->
                view.visibility = View.INVISIBLE
                view.translationX = 0f
                onEnd?.invoke()
            }
            start()
        }
    }

    /**
     * Simple scale pulse effect for button press feedback.
     */
    fun scalePulse(view: View) {
        view.animate()
            .scaleX(0.88f)
            .scaleY(0.88f)
            .setDuration(80)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }
}
