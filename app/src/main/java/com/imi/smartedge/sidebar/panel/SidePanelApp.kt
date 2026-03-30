package com.imi.smartedge.sidebar.panel

import android.app.Application
import android.graphics.drawable.Drawable
import android.util.Log
import com.bumptech.glide.Glide

class SidePanelApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize HiddenApiBypass to allow calling ActivityOptions.setLaunchWindowingMode
        // and other hidden APIs needed for freeform window launching.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            var bypassSuccess = false
            
            // PRIMARY: Use the LSPosed HiddenApiBypass library
            // The correct method name is "addHiddenApiExemptions" (NOT "addExemptions")
            try {
                val bypassClass = Class.forName("org.lsposed.hiddenapibypass.HiddenApiBypass")
                // Exempt ALL hidden APIs using the "L" prefix (all classes in the android namespace)
                // This is the most reliable approach.
                val addExemptionsMethod = bypassClass.getDeclaredMethod(
                    "addHiddenApiExemptions",
                    arrayOf<String>()::class.java
                )
                addExemptionsMethod.invoke(null, arrayOf("L") as Any)
                bypassSuccess = true
                Log.d("SidePanelApp", "HiddenApiBypass: LSPosed library initialized successfully")
            } catch (e: Exception) {
                Log.w("SidePanelApp", "HiddenApiBypass: LSPosed library failed: ${e.message}")
            }
            
            // FALLBACK: Meta-Reflection bypass via VMRuntime.setHiddenApiExemptions
            if (!bypassSuccess) {
                try {
                    val forName = Class::class.java.getDeclaredMethod("forName", String::class.java)
                    val getDeclaredMethod = Class::class.java.getDeclaredMethod(
                        "getDeclaredMethod",
                        String::class.java,
                        arrayOf<Class<*>>()::class.java
                    )
                    val vmRuntimeClass = forName.invoke(null, "dalvik.system.VMRuntime") as Class<*>
                    val getRuntime = getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", null) as java.lang.reflect.Method
                    val setHiddenApiExemptions = getDeclaredMethod.invoke(
                        vmRuntimeClass,
                        "setHiddenApiExemptions",
                        arrayOf(arrayOf<String>()::class.java)
                    ) as java.lang.reflect.Method
                    val vmRuntime = getRuntime.invoke(null)
                    // Exempt all classes starting with "L" (all Android SDK)
                    setHiddenApiExemptions.invoke(vmRuntime, arrayOf(arrayOf("L")))
                    Log.d("SidePanelApp", "HiddenApiBypass: Meta-reflection fallback initialized successfully")
                } catch (e2: Exception) {
                    Log.e("SidePanelApp", "HiddenApiBypass: All bypass methods failed: ${e2.message}")
                }
            }
        }
        
        // Register the custom AppIconRequest loader with Glide
        Glide.get(this).registry.append(
            AppIconRequest::class.java,
            Drawable::class.java,
            AppIconModelLoader.Factory(this)
        )
    }
}