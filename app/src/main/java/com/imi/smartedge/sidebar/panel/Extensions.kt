package com.imi.smartedge.sidebar.panel

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.card.MaterialCardView
import android.graphics.Color
import android.content.res.ColorStateList
import android.app.AppOpsManager
import android.content.Context
import android.os.Binder
import java.lang.reflect.Method

/**
 * Extension to show a modern, very compact "Toast" using Snackbar.
 * Uses Material You dynamic colors and a sleeker mini-pill shape.
 */
fun View.showModernToast(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    val snackbar = Snackbar.make(this, "", duration)
    val snackbarView = snackbar.view as Snackbar.SnackbarLayout
    
    snackbarView.setBackgroundColor(Color.TRANSPARENT)
    snackbarView.removeAllViews()
    snackbarView.setPadding(0, 0, 0, 0)

    val density = context.resources.displayMetrics.density
    val typedValue = android.util.TypedValue()

    // Resolve Material You colors
    context.theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerHighest, typedValue, true)
    val bgColor = typedValue.data
    
    context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
    val textColor = typedValue.data

    // Ultra-Compact Card
    val card = MaterialCardView(context).apply {
        radius = 16f * density // Modern M3 rounded corner
        cardElevation = 2f * density
        useCompatPadding = false
        strokeWidth = (1f * density).toInt()
        strokeColor = (textColor and 0x00FFFFFF) or (0x1A shl 24) // 10% opacity border
        setCardBackgroundColor(ColorStateList.valueOf(bgColor))
        
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
    }

    // Small, readable text
    val textView = TextView(context).apply {
        text = message
        setTextColor(textColor)
        textSize = 13f 
        setPadding((16 * density).toInt(), (6 * density).toInt(), (16 * density).toInt(), (6 * density).toInt())
        gravity = Gravity.CENTER
        maxWidth = (220 * density).toInt() // Very compact max width
    }

    card.addView(textView)
    snackbarView.addView(card)

    val params = snackbarView.layoutParams
    params.width = ViewGroup.LayoutParams.WRAP_CONTENT
    
    val marginPx = (90 * density).toInt() 
    if (params is FrameLayout.LayoutParams) {
        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        params.setMargins(0, 0, 0, marginPx) 
    } else if (params is androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        params.setMargins(0, 0, 0, marginPx)
    }
    
    snackbarView.layoutParams = params
    snackbar.show()
}

object MIUIUtils {
    private const val OP_AUTO_START = 10008

    fun isAutoStartEnabled(context: Context): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return try {
            val method: Method = AppOpsManager::class.java.getMethod(
                "checkOpNoThrow",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            val result = method.invoke(
                ops,
                OP_AUTO_START,
                Binder.getCallingUid(),
                context.packageName
            ) as Int
            result == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun isMIUI(): Boolean {
        return try {
            val manufacturer = android.os.Build.MANUFACTURER.lowercase()
            val property = Class.forName("android.os.SystemProperties")
                .getMethod("get", String::class.java)
                .invoke(null, "ro.miui.ui.version.name")
                .toString()
            manufacturer.contains("xiaomi") || property.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}

object VivoUtils {
    private const val OP_AUTO_START = 10002

    fun isAutoStartEnabled(context: Context): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return try {
            val method: Method = AppOpsManager::class.java.getMethod(
                "checkOpNoThrow",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            val result = method.invoke(
                ops,
                OP_AUTO_START,
                Binder.getCallingUid(),
                context.packageName
            ) as Int
            result == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun isVivo(): Boolean {
        val manufacturer = android.os.Build.MANUFACTURER.lowercase()
        return manufacturer.contains("vivo") || manufacturer.contains("iqoo")
    }
}
