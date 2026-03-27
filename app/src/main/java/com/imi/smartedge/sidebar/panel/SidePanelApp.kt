package com.imi.smartedge.sidebar.panel

import android.app.Application
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide

class SidePanelApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Register the custom AppIconRequest loader with Glide
        Glide.get(this).registry.append(
            AppIconRequest::class.java,
            Drawable::class.java,
            AppIconModelLoader.Factory(this)
        )
    }
}
