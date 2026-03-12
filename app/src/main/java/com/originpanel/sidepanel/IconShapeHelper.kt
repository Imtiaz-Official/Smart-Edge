package com.originpanel.sidepanel

import android.graphics.Outline
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView

/**
 * Utility to apply consistent shapes to icons (Circle, Squircle, Square, etc.)
 * UNIVERSAL: Force-masks all icons (including system/legacy) for a unified UI.
 */
object IconShapeHelper {

    fun applyShape(view: ImageView, shape: String) {
        // Ensure the icon fills the mask area
        view.scaleType = ImageView.ScaleType.CENTER_CROP

        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val w = view.width
                val h = view.height
                val minDim = Math.min(w, h).toFloat()

                when (shape) {
                    PanelPreferences.SHAPE_CIRCLE -> {
                        outline.setOval(0, 0, w, h)
                    }
                    PanelPreferences.SHAPE_SQUARE -> {
                        // Polished Square: Not sharp, but with subtle 8dp rounded corners
                        val radius = 8 * view.context.resources.displayMetrics.density
                        outline.setRoundRect(0, 0, w, h, radius)
                    }
                    PanelPreferences.SHAPE_ROUNDED -> {
                        outline.setRoundRect(0, 0, w, h, minDim * 0.2f)
                    }
                    PanelPreferences.SHAPE_SQUIRCLE -> {
                        // Advanced corner radius for that premium "Apple/OneUI" squircle feel
                        outline.setRoundRect(0, 0, w, h, minDim * 0.35f)
                    }
                    else -> {
                        outline.setOval(0, 0, w, h)
                    }
                }
            }
        }
        view.clipToOutline = true
    }
}
