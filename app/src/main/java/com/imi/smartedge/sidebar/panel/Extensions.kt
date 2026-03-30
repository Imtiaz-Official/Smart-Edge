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
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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

/**
 * Modern highlight animation for search results.
 * Uses a foreground overlay and scale pulse for a professional feel.
 */
fun View.highlightView() {
    val typedValue = android.util.TypedValue()
    context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValue, true)
    val highlightColor = typedValue.data
    
    // 1. Gentle Scale Pulse
    this.animate()
        .scaleX(1.025f)
        .scaleY(1.025f)
        .setDuration(400)
        .setInterpolator(android.view.animation.CycleInterpolator(1f))
        .start()

    // 2. Sophisticated Foreground Flash (Android M+)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        val originalForeground = foreground
        val highlightDrawable = android.graphics.drawable.ColorDrawable(highlightColor)
        foreground = highlightDrawable
        highlightDrawable.alpha = 0
        
        android.animation.ValueAnimator.ofInt(0, 120, 0).apply {
            duration = 1000
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                highlightDrawable.alpha = animator.animatedValue as Int
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    foreground = originalForeground
                }
            })
            start()
        }
    } else {
        // Fallback for older versions
        val originalBackground = background
        android.animation.ValueAnimator.ofArgb(Color.TRANSPARENT, highlightColor, Color.TRANSPARENT).apply {
            duration = 800
            addUpdateListener { setBackgroundColor(it.animatedValue as Int) }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    background = originalBackground
                }
            })
            start()
        }
    }
}

    /**
    * Opens the accessibility settings. On Android 12+, it deep-links directly to 
 * this app's specific service toggle. On older versions, it opens the general
 * list and attempts to highlight this app if the OEM supports it.
 */
fun Context.openAccessibilitySettings() {
    val componentName = android.content.ComponentName(this, PanelAccessibilityService::class.java)
    val flattenedName = componentName.flattenToString()
    
    // We'll try the most specific action first (Android 12+)
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").apply {
            putExtra(Intent.EXTRA_COMPONENT_NAME, flattenedName)
            // System highlight/flash tricks for modern Android
            putExtra(":settings:fragment_args_key", flattenedName)
            putExtra(":settings:show_fragment_args", Bundle().apply {
                putString(":settings:fragment_args_key", flattenedName)
            })
            // Force highlighting the specific preference
            putExtra("highlight_key", flattenedName)
        }
    } else {
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            putExtra(":settings:fragment_args_key", flattenedName)
            putExtra(":settings:show_fragment_args", Bundle().apply {
                putString(":settings:fragment_args_key", flattenedName)
            })
            putExtra("EXTRA_FRAGMENT_ARG_KEY", flattenedName)
            putExtra("EXTRA_SHOW_FRAGMENT_ARGUMENTS", Bundle().apply {
                putString(":settings:fragment_args_key", flattenedName)
            })
            // Legacy highlight key
            putExtra("highlight_key", flattenedName)
        }
    }
    
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    
    try {
        startActivity(intent)
    } catch (e: Exception) {
        // Fallback 1: Specifically for Xiaomi/MIUI/HyperOS to open the "Downloaded Apps" section
        if (MIUIUtils.isMIUI()) {
            try {
                val miuiIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(":settings:show_fragment", "com.android.settings.accessibility.AccessibilitySettingsForUnderO")
                }
                startActivity(miuiIntent)
                return
            } catch (e2: Exception) {
                // Ignore and fall through to other fallbacks
            }
        }

        // Fallback 2: Specifically for Vivo/OriginOS/Chinese ROMs which often 
        // have a dedicated "Downloaded Services" or "Installed Apps" list.
        try {
            val vivoIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // This extra often forces the "Downloaded Apps" section to open on Funtouch/OriginOS
                putExtra(":settings:show_fragment_args", Bundle().apply {
                    putString(":settings:fragment_args_key", flattenedName)
                })
            }
            startActivity(vivoIntent)
        } catch (e2: Exception) {
            // Ultimate fallback to general settings
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e3: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
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

/**
 * Checks if 'Enable freeform windows' is turned on in Developer Options.
 */
fun Context.isFreeformEnabled(): Boolean {
    val freeformPref = android.provider.Settings.Global.getInt(contentResolver, "freeform_window_management", 0) != 0
    val freeformSupport = android.provider.Settings.Global.getInt(contentResolver, "enable_freeform_support", 0) != 0
    val forceResizable = android.provider.Settings.Global.getInt(contentResolver, "force_resizable_activities", 0) != 0
    
    val result = freeformPref || freeformSupport || forceResizable
    android.util.Log.d("SmartEdgeExt", "Freeform Check: pref=$freeformPref, support=$freeformSupport, resizable=$forceResizable -> Result=$result")
    return result
}

/**
 * Attempts to open Developer Options and highlight the Freeform windows toggle.
 */
fun Context.openFreeformDeveloperSettings() {
    val highlightKey = "freeform_window_management"
    val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Modern Android highlight keys
        putExtra(":settings:fragment_args_key", highlightKey)
        putExtra(":settings:show_fragment_args", Bundle().apply {
            putString(":settings:fragment_args_key", highlightKey)
        })
        putExtra("highlight_key", highlightKey)
        putExtra("EXTRA_FRAGMENT_ARG_KEY", highlightKey)
        putExtra("EXTRA_SHOW_FRAGMENT_ARGUMENTS", Bundle().apply {
            putString(":settings:fragment_args_key", highlightKey)
        })
    }
    
    try {
        startActivity(intent)
    } catch (e: Exception) {
        // Fallback to general developer settings if deep-link fails
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e2: Exception) {
            android.widget.Toast.makeText(this, "Developer Options not found", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Attempts to write a Global system setting. Requires WRITE_SECURE_SETTINGS.
 */
fun Context.putGlobalSetting(setting: String, value: Int): Boolean {
    return try {
        android.provider.Settings.Global.putInt(contentResolver, setting, value)
        true
    } catch (e: SecurityException) {
        false
    }
}

/**
 * Calculates an automatic scaling factor based on screen size and orientation.
 * Returns 1.0f for all devices to keep original sizes as requested.
 */
fun Context.getAutoScalingFactor(): Float {
    return 1.0f
}

/**
 * Detects if an app's primary activity prefers landscape orientation.
 */
fun Context.isLandscapeApp(packageName: String): Boolean {
    return try {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        val componentName = intent.component ?: return false
        val activityInfo = packageManager.getActivityInfo(componentName, 0)
        activityInfo.screenOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ||
                activityInfo.screenOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE ||
                activityInfo.screenOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    } catch (e: Exception) {
        false
    }
}

/**
 * Extension to convert DP to PX.
 */
fun Context.dpToPx(dp: Int): Int {
    return (dp * resources.displayMetrics.density).toInt()
}
