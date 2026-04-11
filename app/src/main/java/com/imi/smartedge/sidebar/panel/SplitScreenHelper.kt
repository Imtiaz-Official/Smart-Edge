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
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName) ?: return
        
        // 1. Critical Flags for Multi-Window
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        
        val isSplit = mode == WINDOWING_MODE_SPLIT_SCREEN_PRIMARY ||
                      mode == WINDOWING_MODE_SPLIT_SCREEN_SECONDARY
        if (isSplit) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
        }

        val options = ActivityOptions.makeBasic()
        val isVivo = VivoUtils.isVivo()

        try {
            // 2. Force Windowing Mode via Hidden API
            HiddenApiBypass.invoke(
                ActivityOptions::class.java,
                options,
                "setLaunchWindowingMode",
                mode
            )

            // Legacy API fallback (Oreo/Pie logic still present in some OEM frameworks)
            try {
                if (isSplit) {
                    val createMode = if (mode == WINDOWING_MODE_SPLIT_SCREEN_PRIMARY) 0 else 1
                    HiddenApiBypass.invoke(
                        ActivityOptions::class.java,
                        options,
                        "setSplitScreenCreateMode",
                        createMode
                    )
                }
            } catch (e: Exception) {}
            
            // 3. Force via Intent Extras (Some OEMs like Vivo/iQOO/OriginOS look for these)
            launchIntent.putExtra("android.intent.extra.WINDOWING_MODE", mode)
            launchIntent.putExtra("android.intent.extra.LAUNCH_WINDOWING_MODE", mode)
            
            // 4. Force Activity Type (Standard)
            HiddenApiBypass.invoke(
                ActivityOptions::class.java,
                options,
                "setLaunchActivityType",
                1 // ACTIVITY_TYPE_STANDARD
            )

            // 5. Force specific Screen Bounds (Crucial for split-screen on some OEMs)
            //    On Vivo/OriginOS, forcing explicit bounds can conflict with their own
            //    window manager, so we skip it and rely on mode + adjacent flag instead.
            val dm = context.resources.displayMetrics
            val w = dm.widthPixels
            val h = dm.heightPixels
            
            val rect = if (isVivo) {
                // OriginOS handles split bounds itself — don't override
                null
            } else {
                when (mode) {
                    WINDOWING_MODE_SPLIT_SCREEN_PRIMARY   -> android.graphics.Rect(0, 0, w, h / 2)
                    WINDOWING_MODE_SPLIT_SCREEN_SECONDARY -> android.graphics.Rect(0, h / 2, w, h)
                    WINDOWING_MODE_FREEFORM               -> android.graphics.Rect(w / 10, h / 10, w * 9 / 10, h * 9 / 10)
                    else -> null
                }
            }
            
            if (rect != null) {
                options.launchBounds = rect
            }

            Log.d(TAG, "Launching $packageName: mode=$mode, bounds=$rect, isVivo=$isVivo")
            context.startActivity(launchIntent, options.toBundle())
            
        } catch (e: Exception) {
            Log.e(TAG, "Split launch failed: ${e.message}")
            // Final fallback: try with just adjacent flag (works on some OEMs)
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
