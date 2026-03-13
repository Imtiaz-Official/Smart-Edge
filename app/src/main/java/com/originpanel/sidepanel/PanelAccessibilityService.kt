package com.originpanel.sidepanel

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class PanelAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "PanelAccessibility"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            val className = event.className?.toString() ?: ""
            
            // Log.d(TAG, "Window State Changed. Package: $packageName, Class: $className")
            
            // If the system transitions to ANY window that is not our app
            // (e.g. Launcher, Recents, Notification shade, another app), instantly close the panel.
            // This achieves 0-latency auto-close for all system gestures.
            if (packageName != "com.originpanel.sidepanel") {
                val closeIntent = Intent(this, FloatingPanelService::class.java).apply {
                    action = FloatingPanelService.ACTION_CLOSE_PANEL
                }
                startService(closeIntent)
            }
        }
    }

    override fun onInterrupt() {
        // Required override, but nothing to teardown for our simple usecase
    }
}
