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
 * Intelligent: Only masks if the icon is an AdaptiveIconDrawable or on Android 8+.
 * Legacy icons are left untouched to prevent double-clipping.
 */
object IconShapeHelper {

    fun applyShape(view: ImageView, shape: String) {
        val drawable = view.drawable
        
        // Android 8.0+ Adaptive Icons are designed to be masked.
        // If it's NOT an adaptive icon, masking it manually often looks bad (double-border or clipping).
        val isAdaptive = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable

        if (!isAdaptive) {
            // Return to system default (no manual mask)
            view.outlineProvider = ViewOutlineProvider.BACKGROUND
            view.clipToOutline = false
            return
        }

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
