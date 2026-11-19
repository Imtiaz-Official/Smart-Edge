package com.imi.smartedge.sidebar.panel

import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator

object ActionDispatcher {

    fun performAction(
        context: Context,
        actionId: Int,
        panelPrefs: PanelPreferences,
        onTriggerPanel: (() -> Unit)? = null,
        onDragHandle: (() -> Unit)? = null
    ) {
        if (actionId == PanelPreferences.ACTION_NONE) return

        vibrateHaptic(context, panelPrefs)

        when (actionId) {
            PanelPreferences.ACTION_OPEN_LAUNCHER -> onTriggerPanel?.invoke()
            PanelPreferences.ACTION_MOVE_HANDLE -> onDragHandle?.invoke()

            // Service Actions
            PanelPreferences.ACTION_FLASHLIGHT -> triggerServiceAction(context, FloatingPanelService.ACTION_TOGGLE_FLASHLIGHT)
            PanelPreferences.ACTION_CAMERA -> triggerServiceAction(context, FloatingPanelService.ACTION_LAUNCH_CAMERA)
            PanelPreferences.ACTION_AUTO_ROTATION -> triggerServiceAction(context, FloatingPanelService.ACTION_TOGGLE_ROTATION)
            PanelPreferences.ACTION_OPEN_FAVORITE_APP -> triggerServiceAction(context, FloatingPanelService.ACTION_OPEN_FAV_APP)

            // System/Navigation Gestures
            PanelPreferences.ACTION_SCREENSHOT -> executeSystemAction(context, panelPrefs, PanelAccessibilityService.ACTION_TAKE_SCREENSHOT)
            PanelPreferences.ACTION_PREVIOUS_APP -> executeSystemAction(context, panelPrefs, PanelAccessibilityService.ACTION_PREVIOUS_APP)
            PanelPreferences.ACTION_BACK -> executeSystemAction(context, panelPrefs, PanelAccessibilityService.ACTION_BACK)
            PanelPreferences.ACTION_HOME -> executeSystemAction(context, panelPrefs, PanelAccessibilityService.ACTION_HOME)
            PanelPreferences.ACTION_RECENTS -> executeSystemAction(context, panelPrefs, PanelAccessibilityService.ACTION_RECENTS)
            PanelPreferences.ACTION_NOTIFICATIONS -> executeSystemAction(context, panelPrefs, PanelAccessibilityService.ACTION_NOTIFICATIONS)
            PanelPreferences.ACTION_QUICK_SETTINGS -> executeSystemAction(context, panelPrefs, PanelAccessibilityService.ACTION_QUICK_SETTINGS)
            PanelPreferences.ACTION_LOCK_SCREEN -> executeSystemAction(context, panelPrefs, PanelAccessibilityService.ACTION_LOCK_SCREEN)
            PanelPreferences.ACTION_POWER_MENU -> executeSystemAction(context, panelPrefs, PanelAccessibilityService.ACTION_SHOW_POWER_MENU)

            // Media Controls (borrowed from QuickBall keyevent pattern)
            PanelPreferences.ACTION_MEDIA_PLAY_PAUSE -> dispatchMediaKey(context, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            PanelPreferences.ACTION_MEDIA_NEXT -> dispatchMediaKey(context, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
            PanelPreferences.ACTION_MEDIA_PREV -> dispatchMediaKey(context, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)

            // DND / Silent Toggle (borrowed from QuickBall)
            PanelPreferences.ACTION_DND_TOGGLE -> toggleDnd(context)
        }
    }

    private fun toggleDnd(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (nm.isNotificationPolicyAccessGranted) {
                val current = nm.currentInterruptionFilter
                val target = if (current == android.app.NotificationManager.INTERRUPTION_FILTER_ALL) {
                    android.app.NotificationManager.INTERRUPTION_FILTER_NONE
                } else {
                    android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                }
                nm.setInterruptionFilter(target)
            } else {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {}
            }
        }
    }

    private fun dispatchMediaKey(context: Context, keyCode: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager ?: return
        audioManager.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode))
    }

    private fun triggerServiceAction(context: Context, action: String) {
        val intent = Intent(context, FloatingPanelService::class.java).apply {
            this.action = action
        }
        context.startService(intent)
    }

    private fun executeSystemAction(context: Context, panelPrefs: PanelPreferences, accAction: String) {
        if (panelPrefs.useAutomationForGestures && AutomationManager.isAutomationPossible()) {
            val success = when (accAction) {
                PanelAccessibilityService.ACTION_BACK -> AutomationManager.performBack()
                PanelAccessibilityService.ACTION_HOME -> AutomationManager.performHome()
                PanelAccessibilityService.ACTION_RECENTS -> AutomationManager.performRecents()
                PanelAccessibilityService.ACTION_NOTIFICATIONS -> AutomationManager.performNotifications()
                PanelAccessibilityService.ACTION_QUICK_SETTINGS -> AutomationManager.performQuickSettings()
                PanelAccessibilityService.ACTION_SPLIT_SCREEN -> AutomationManager.performSplitScreen()
                PanelAccessibilityService.ACTION_LOCK_SCREEN -> { AutomationManager.performLockScreen(); true }
                PanelAccessibilityService.ACTION_SHOW_POWER_MENU -> AutomationManager.performPowerMenu()
                PanelAccessibilityService.ACTION_TAKE_SCREENSHOT -> { AutomationManager.takeScreenshot(); true }
                PanelAccessibilityService.ACTION_PREVIOUS_APP -> { AutomationManager.performPreviousApp(); true }
                else -> false
            }
            if (success) return
        }

        val intent = Intent(context, PanelAccessibilityService::class.java).apply {
            this.action = accAction
        }
        context.startService(intent)
    }

    fun vibrateHaptic(context: Context, panelPrefs: PanelPreferences, customDuration: Long? = null) {
        if (!panelPrefs.hapticEnabled) return
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (!vibrator.hasVibrator()) return

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val effect = when (panelPrefs.hapticIntensity) {
                    PanelPreferences.HAPTIC_LIGHT -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    PanelPreferences.HAPTIC_STRONG -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                    else -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                }
                vibrator.vibrate(effect)
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val duration = customDuration ?: when (panelPrefs.hapticIntensity) {
                    PanelPreferences.HAPTIC_LIGHT -> 10L
                    PanelPreferences.HAPTIC_STRONG -> 40L
                    else -> 20L
                }
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val duration = customDuration ?: when (panelPrefs.hapticIntensity) {
                    PanelPreferences.HAPTIC_LIGHT -> 10L
                    PanelPreferences.HAPTIC_STRONG -> 40L
                    else -> 20L
                }
                vibrator.vibrate(duration)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}