package com.imi.smartedge.sidebar.panel

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass

object SplitScreenHelper {
    private const val TAG = "SplitScreenHelper"

    // Windowing modes from Android Source
    private const val WINDOWING_MODE_FULLSCREEN = 1
    private const val WINDOWING_MODE_PINNED = 2
    private const val WINDOWING_MODE_SPLIT_SCREEN_PRIMARY = 3
    private const val WINDOWING_MODE_SPLIT_SCREEN_SECONDARY = 4
    private const val WINDOWING_MODE_FREEFORM = 5

    fun launchApp(context: Context, packageName: String, mode: Int) {
        val prefs = PanelPreferences(context)
        
        // Use Shell Launch engine if enabled and possible for freeform
        if (mode == WINDOWING_MODE_FREEFORM && prefs.useShellForFreeform && AutomationManager.isAutomationPossible()) {
            val launched = AutomationManager.launchApp(context, packageName, prefs.freeformWindowingModeInt)
            if (launched) return
        }

        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName) ?: return
        
        // 1. Critical Flags for Multi-Window
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        // Some OEMs (like Vivo) glitch if MULTIPLE_TASK is used with their native modes
        if (mode == WINDOWING_MODE_FREEFORM && PanelPreferences(context).freeformWindowingModeInt == 5) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        
        val isSplit = mode == WINDOWING_MODE_SPLIT_SCREEN_PRIMARY ||
                      mode == WINDOWING_MODE_SPLIT_SCREEN_SECONDARY
        
        if (isSplit) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }

        val options = ActivityOptions.makeBasic()

        val finalMode = if (mode == WINDOWING_MODE_FREEFORM) {
            PanelPreferences(context).freeformWindowingModeInt
        } else {
            mode
        }

        try {
            // 2. Force Windowing Mode via Hidden API
            HiddenApiBypass.invoke(
                ActivityOptions::class.java,
                options,
                "setLaunchWindowingMode",
                finalMode
            )

            // 3. Force via Intent Extras (Commonly respected by many OEMs and AOSP)
            launchIntent.putExtra("android.intent.extra.WINDOWING_MODE", finalMode)
            launchIntent.putExtra("android.intent.extra.LAUNCH_WINDOWING_MODE", finalMode)
            
            // Vivo/OriginOS specific extras
            if (finalMode == 106) {
                launchIntent.putExtra("vivo_small_window", true)
                launchIntent.putExtra("is_vivo_freeform", true)
            }
            
            // 4. Force specific Screen Bounds
            val dm = context.resources.displayMetrics
            val w = dm.widthPixels
            val h = dm.heightPixels
            
            val rect = when (mode) {
                WINDOWING_MODE_SPLIT_SCREEN_PRIMARY   -> android.graphics.Rect(0, 0, w, h / 2)
                WINDOWING_MODE_SPLIT_SCREEN_SECONDARY -> android.graphics.Rect(0, h / 2, w, h)
                WINDOWING_MODE_FREEFORM               -> {
                    // Provide bounds but use a slightly smaller, centered Rect for custom modes
                    // to help the OEM engine snap it correctly.
                    val factor = if (finalMode == 5) 0.8 else 0.7 
                    val winW = (w * factor).toInt()
                    val winH = (h * factor).toInt()
                    val left = (w - winW) / 2
                    val top = (h - winH) / 2
                    android.graphics.Rect(left, top, left + winW, top + winH)
                }
                else -> null
            }
            
            if (rect != null) {
                options.launchBounds = rect
            }

            Log.d(TAG, "Launching $packageName: mode=$finalMode (requested=$mode), bounds=$rect")
            context.startActivity(launchIntent, options.toBundle())
            
        } catch (e: Exception) {
            Log.e(TAG, "Split launch failed: ${e.message}")
            try {
                context.startActivity(launchIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback launch also failed: ${e2.message}")
            }
        }
    }

    const val MODE_TOP = WINDOWING_MODE_SPLIT_SCREEN_PRIMARY
    const val MODE_BOTTOM = WINDOWING_MODE_SPLIT_SCREEN_SECONDARY
    const val MODE_FREEFORM = WINDOWING_MODE_FREEFORM
}
