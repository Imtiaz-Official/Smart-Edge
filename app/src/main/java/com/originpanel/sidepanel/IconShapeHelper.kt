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
            PanelPreferences.SHAPE_CIRCLE -> circleProvider
            PanelPreferences.SHAPE_SQUARE -> squareProvider
            PanelPreferences.SHAPE_ROUNDED -> roundedProvider
            PanelPreferences.SHAPE_SQUIRCLE -> squircleProvider
            else -> circleProvider
        }

        if (view.outlineProvider != provider) {
            view.scaleType = ImageView.ScaleType.CENTER_CROP
            view.outlineProvider = provider
            view.clipToOutline = true
        }
    }
}
