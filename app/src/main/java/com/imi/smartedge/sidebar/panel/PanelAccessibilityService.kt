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
        const val ACTION_SHOW_POWER_MENU = "com.imi.smartedge.sidebar.panel.ACTION_SHOW_POWER_MENU"
        const val ACTION_SPLIT_SCREEN = "com.imi.smartedge.sidebar.panel.ACTION_SPLIT_SCREEN"
        
        const val EXTRA_PKG = "pkg"
        const val EXTRA_MODE = "mode"
        
        @Volatile
        var isRunning = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TAKE_SCREENSHOT -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                }
            }
            ACTION_SHOW_POWER_MENU -> {
                performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            }
            ACTION_SPLIT_SCREEN -> {
                val pkg = intent.getStringExtra(EXTRA_PKG)
                val mode = intent.getIntExtra(EXTRA_MODE, 1)
                if (pkg != null) {
                    if (mode == SplitScreenHelper.MODE_TOP || mode == SplitScreenHelper.MODE_BOTTOM) {
                        triggerSplitScreen(pkg, mode)
                    } else {
                        // Freeform launch doesn't need the toggle action
                        SplitScreenHelper.launchApp(this, pkg, mode)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    /**
     * Triggers split-screen for the given package and mode.
     *
     * Strategy 1 (AOSP): Use GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN to pin the foreground app,
     *   then launch the second app adjacent to it.
     * Strategy 2 (Origin OS / Vivo / OEMs that block the toggle): Skip the toggle and
     *   launch the second app directly with split-screen windowing mode flags. The OEM's
     *   own window manager handles placing it in split.
     */
    private fun triggerSplitScreen(pkg: String, mode: Int) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val isVivo = VivoUtils.isVivo()

        if (isVivo) {
            // Origin OS / Funtouch OS: GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN is unreliable.
            // Directly launch with the split-screen windowing mode and adjacent flag.
            // SplitScreenHelper already sets Vivo-specific intent extras.
            Log.d(TAG, "Origin OS detected — using direct split launch for $pkg mode=$mode")
            SplitScreenHelper.launchApp(this, pkg, mode)
        } else {
            // Standard AOSP path: toggle split, wait for animation, then launch second app
            val toggled = performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
            Log.d(TAG, "GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN result=$toggled")

            val delay = if (toggled) 700L else 300L
            handler.postDelayed({
                SplitScreenHelper.launchApp(this, pkg, mode)
            }, delay)
        }
    }

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

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        isRunning = false
        val stopIntent = Intent(this, FloatingPanelService::class.java).apply {
            action = FloatingPanelService.ACTION_STOP
        }
        startService(stopIntent)
        return super.onUnbind(intent)
    }
}
