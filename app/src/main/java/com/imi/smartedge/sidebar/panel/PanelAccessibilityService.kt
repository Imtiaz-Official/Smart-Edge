package com.imi.smartedge.sidebar.panel

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class PanelAccessibilityService : AccessibilityService() {

    private lateinit var panelPrefs: PanelPreferences

    override fun onCreate() {
        super.onCreate()
        panelPrefs = PanelPreferences(this)
    }

    companion object {
        private const val TAG = "PanelAccessibility"
        const val ACTION_TAKE_SCREENSHOT = "com.imi.smartedge.sidebar.panel.ACTION_TAKE_SCREENSHOT"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TAKE_SCREENSHOT) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            }
        }
        return START_NOT_STICKY
    }

    // We intentionally do NOT override onServiceConnected to auto-start the Side Panel UI
    // because the user should have to explicitly press the "Start" button in the MainActivity.

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            val className = event.className?.toString() ?: ""
            
            // Get the current active keyboard package
            val defaultIme = android.provider.Settings.Secure.getString(
                contentResolver,
                android.provider.Settings.Secure.DEFAULT_INPUT_METHOD
            )
            val imePackage = defaultIme?.substringBefore("/") ?: ""
            
            // Log.d(TAG, "Window State Changed. Package: $packageName, Class: $className")
            
            // If the system transitions to ANY window that is not our app OR the soft keyboard
            // (e.g. Launcher, Recents, Notification shade, another app), instantly close the panel.
            // This achieves 0-latency auto-close for all system gestures.
            if (packageName != "com.imi.smartedge.sidebar.panel" && packageName != imePackage) {
                if (panelPrefs.serviceEnabled) {
                    val closeIntent = Intent(this, FloatingPanelService::class.java).apply {
                        action = FloatingPanelService.ACTION_CLOSE_PANEL
                    }
                    startService(closeIntent)
                }
            }
        }
    }

    override fun onInterrupt() {
        // Required override, but nothing to teardown for our simple usecase
    }

    override fun onUnbind(intent: Intent?): Boolean {
        // When the user toggles Accessibility OFF, Android unbinds this service.
        // We should immediately stop the floating panel if it is running.
        val stopIntent = Intent(this, FloatingPanelService::class.java).apply {
            action = FloatingPanelService.ACTION_STOP
        }
        startService(stopIntent)
        return super.onUnbind(intent)
    }
}
