package com.imi.smartedge.sidebar.panel

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Intent
import android.util.Log

class NotificationTrackingService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationTracker"
        
        // List of unique package names that currently have active notifications
        private val notificationPackages = mutableSetOf<String>()
        
        fun getActiveNotificationPackages(): List<String> {
            return notificationPackages.toList()
        }
        
        // Callback for UI updates
        var onNotificationsChanged: (() -> Unit)? = null
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateActiveNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.packageName?.let { pkg ->
            Log.d(TAG, "Notification posted: $pkg")
            if (notificationPackages.add(pkg)) {
                onNotificationsChanged?.invoke()
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn?.packageName?.let { pkg ->
            Log.d(TAG, "Notification removed: $pkg")
        }
        updateActiveNotifications()
    }

    private fun updateActiveNotifications() {
        try {
            val current = mutableSetOf<String>()
            val sbns = try { getActiveNotifications() } catch (e: Exception) { null }
            
            if (sbns != null) {
                for (sbn in sbns) {
                    sbn.packageName?.let { current.add(it) }
                }
            }
            
            Log.d(TAG, "Updating active notifications. Count: ${current.size}")
            
            if (notificationPackages != current) {
                notificationPackages.clear()
                notificationPackages.addAll(current)
                onNotificationsChanged?.invoke()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notifications", e)
        }
    }
}
