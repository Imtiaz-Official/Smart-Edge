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

    private val adaptiveProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            if (view is ImageView && view.drawable is AdaptiveIconDrawable) {
                // Let the AdaptiveIconDrawable provide its native path (e.g. OriginOS Squircle)
                val drawable = view.drawable as AdaptiveIconDrawable
                // We must use the drawable's path or fallback to a high-quality round rect
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Try to get the path from the drawable if possible, or fallback to standard squircle
                    val minDim = Math.min(view.width, view.height).toFloat()
                    outline.setRoundRect(0, 0, view.width, view.height, minDim * 0.22f)
                }
            } else {
                // Fallback for legacy icons: nice rounded corners
                val minDim = Math.min(view.width, view.height).toFloat()
                outline.setRoundRect(0, 0, view.width, view.height, minDim * 0.22f)
            }
        }
    }

    private val circleProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setOval(0, 0, view.width, view.height)
        }
    }

    private val squareProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            val radius = 8 * view.context.resources.displayMetrics.density
            outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
    }

    private val roundedProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            val minDim = Math.min(view.width, view.height).toFloat()
            outline.setRoundRect(0, 0, view.width, view.height, minDim * 0.2f)
        }
    }

    private val squircleProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            val minDim = Math.min(view.width, view.height).toFloat()
            outline.setRoundRect(0, 0, view.width, view.height, minDim * 0.35f)
        }
    }

    fun applyShape(view: ImageView, shape: String) {
        val provider = when (shape) {
            PanelPreferences.SHAPE_SYSTEM -> adaptiveProvider
            PanelPreferences.SHAPE_CIRCLE -> circleProvider
            PanelPreferences.SHAPE_SQUARE -> squareProvider
            PanelPreferences.SHAPE_ROUNDED -> roundedProvider
            PanelPreferences.SHAPE_SQUIRCLE -> squircleProvider
            else -> adaptiveProvider
        }

        if (view.outlineProvider != provider) {
            view.scaleType = ImageView.ScaleType.CENTER_CROP
            view.outlineProvider = provider
            view.clipToOutline = true
        }
    }
}
