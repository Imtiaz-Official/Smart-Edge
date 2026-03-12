package com.originpanel.sidepanel

import android.graphics.Outline
import android.graphics.Path
import android.view.View
import android.view.ViewOutlineProvider

/**
 * Utility to apply consistent shapes to icons (Circle, Squircle, Square, etc.)
 */
object IconShapeHelper {

    fun applyShape(view: View, shape: String) {
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
                        outline.setRect(0, 0, w, h)
                    }
                    PanelPreferences.SHAPE_ROUNDED -> {
                        outline.setRoundRect(0, 0, w, h, minDim * 0.2f)
                    }
                    PanelPreferences.SHAPE_SQUIRCLE -> {
                        // Standard rounded rect with high radius for squircle-like feel
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
