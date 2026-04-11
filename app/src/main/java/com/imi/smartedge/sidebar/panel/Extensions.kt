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
 * Highly visible modern highlight animation for search results.
 * Uses a prominent rounded foreground pill that fades out, plus a gentle bounce.
 */
fun View.highlightView() {
    val typedValue = android.util.TypedValue()
    // Use colorPrimary for maximum contrast and brand consistency
    context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
    val highlightColor = typedValue.data
    
    // 1. Attention-grabbing Bounce
    this.animate()
        .scaleX(1.05f)
        .scaleY(1.05f)
        .setDuration(200)
        .setInterpolator(android.view.animation.OvershootInterpolator())
        .withEndAction {
            this.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
        .start()

    // 2. High-visibility rounded flash overlay
    val flashDrawable = android.graphics.drawable.GradientDrawable().apply {
        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
        cornerRadius = 12f * resources.displayMetrics.density // Modern 12dp rounded corners
        setColor(highlightColor)
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        val originalForeground = this.foreground
        this.foreground = flashDrawable
        
        android.animation.ValueAnimator.ofInt(120, 0).apply { // ~45% opacity
            duration = 1800
            startDelay = 200 // Hold peak color for a moment
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { animator ->
                flashDrawable.alpha = (animator.animatedValue as? Int) ?: 0
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    this@highlightView.foreground = originalForeground
                }
            })
            start()
        }
    } else {
        val originalBackground = this.background
        val layerDrawable = if (originalBackground != null) {
            android.graphics.drawable.LayerDrawable(arrayOf(originalBackground, flashDrawable))
        } else {
            flashDrawable
        }
        this.background = layerDrawable

        android.animation.ValueAnimator.ofInt(120, 0).apply {
            duration = 1800
            startDelay = 200
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { animator ->
                flashDrawable.alpha = (animator.animatedValue as? Int) ?: 0
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    this@highlightView.background = originalBackground
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

/**
 * Opens a modern color picker with manual Hex input support and visual sliders.
 * Integrated into a seamless Material 3 Dialog using skydoves ColorPickerView.
 */
fun Context.openColorPicker(initialColor: Int, onPick: (Int) -> Unit) {
    val inflater = android.view.LayoutInflater.from(this)
    val rootLayout = android.widget.LinearLayout(this).apply {
        orientation = android.widget.LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // 1. Hex Input and Preview
    val hexInputLayout = com.google.android.material.textfield.TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
        hint = "Hex Color (#AARRGGBB)"
        endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_CLEAR_TEXT
        setBoxCornerRadii(dpToPx(12).toFloat(), dpToPx(12).toFloat(), dpToPx(12).toFloat(), dpToPx(12).toFloat())
        layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            val p = dpToPx(20)
            setMargins(p, dpToPx(8), p, 0)
        }
    }

    val hexEditText = com.google.android.material.textfield.TextInputEditText(hexInputLayout.context).apply {
        setText(String.format("#%08X", initialColor))
        filters = arrayOf(android.text.InputFilter.LengthFilter(9))
    }
    hexInputLayout.addView(hexEditText)

    val previewCard = com.google.android.material.card.MaterialCardView(this).apply {
        layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            dpToPx(60)
        ).apply {
            val p = dpToPx(20)
            setMargins(p, dpToPx(12), p, dpToPx(16))
        }
        radius = dpToPx(16).toFloat()
        setCardBackgroundColor(initialColor)
        strokeWidth = dpToPx(1)
        strokeColor = Color.parseColor("#33FFFFFF")
    }

    // 2. Skydoves ColorPickerView
    val colorPickerView = com.skydoves.colorpickerview.ColorPickerView.Builder(this)
        .setInitialColor(initialColor)
        .build()
        .apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(200) // Beautiful large circle
            ).apply {
                val p = dpToPx(20)
                setMargins(p, 0, p, 0)
            }
        }

    val alphaSlideBar = com.skydoves.colorpickerview.sliders.AlphaSlideBar(this).apply {
        layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            dpToPx(24)
        ).apply {
            val p = dpToPx(20)
            setMargins(p, dpToPx(16), p, dpToPx(8))
        }
    }

    val brightnessSlideBar = com.skydoves.colorpickerview.sliders.BrightnessSlideBar(this).apply {
        layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            dpToPx(24)
        ).apply {
            val p = dpToPx(20)
            setMargins(p, 0, p, dpToPx(8))
        }
    }

    // Attach sliders to picker
    colorPickerView.attachAlphaSlider(alphaSlideBar)
    colorPickerView.attachBrightnessSlider(brightnessSlideBar)

    // Assemble UI
    rootLayout.addView(hexInputLayout)
    rootLayout.addView(previewCard)
    rootLayout.addView(colorPickerView)
    rootLayout.addView(alphaSlideBar)
    rootLayout.addView(brightnessSlideBar)

    var isUpdatingFromPicker = false
    var isUpdatingFromHex = false
    var currentColor = initialColor

    // Setup Listeners
    colorPickerView.setColorListener(object : com.skydoves.colorpickerview.listeners.ColorEnvelopeListener {
        override fun onColorSelected(envelope: com.skydoves.colorpickerview.ColorEnvelope, fromUser: Boolean) {
            currentColor = envelope.color
            previewCard.setCardBackgroundColor(currentColor)
            
            if (fromUser && !isUpdatingFromHex) {
                isUpdatingFromPicker = true
                val hexStr = "#" + envelope.hexCode
                if (hexEditText.text.toString().uppercase() != hexStr) {
                    hexEditText.setText(hexStr)
                }
                isUpdatingFromPicker = false
            }
        }
    })

    hexEditText.addTextChangedListener(object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) {
            if (isUpdatingFromPicker) return
            try {
                val colorStr = s.toString()
                val c = Color.parseColor(if (colorStr.startsWith("#")) colorStr else "#$colorStr")
                currentColor = c
                previewCard.setCardBackgroundColor(c)
                
                isUpdatingFromHex = true
                colorPickerView.setInitialColor(c) // This updates the cursor position in skydoves
                isUpdatingFromHex = false
            } catch (e: Exception) {
                // Ignore invalid hex while typing
            }
        }
    })

    com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
        .setTitle("Choose Color")
        .setView(rootLayout)
        .setPositiveButton("Select") { _, _ ->
            onPick(currentColor)
        }
        .setNegativeButton("Cancel", null)
        .show()
}
