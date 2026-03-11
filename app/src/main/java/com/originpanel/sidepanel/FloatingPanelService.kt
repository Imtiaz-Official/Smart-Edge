package com.originpanel.sidepanel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

/**
 * The core foreground service that manages all overlay windows.
 *
 * Research-backed WindowManager flags:
 * - TYPE_APPLICATION_OVERLAY  : correct type for Android 8+ (replaces deprecated TYPE_SYSTEM_OVERLAY)
 * - FLAG_NOT_FOCUSABLE        : prevents stealing keyboard/IME focus from foreground app
 * - FLAG_NOT_TOUCH_MODAL      : only intercept touches within the view's bounds
 * - FLAG_LAYOUT_IN_SCREEN     : draw within the full screen including status bar area
 * - FLAG_WATCH_OUTSIDE_TOUCH  : panel gets ACTION_OUTSIDE so we can auto-dismiss on outside tap
 *
 * Android 14 note: The service is started only after the edge handle overlay is already visible,
 * satisfying the requirement that a SYSTEM_ALERT_WINDOW app must have a visible overlay before
 * starting a foreground service from the background.
 */
class FloatingPanelService : Service() {

    private lateinit var windowManager: WindowManager
    private var edgeHandleView: EdgeHandleView? = null
    private var sidePanelView: SidePanelView? = null

    private var isPanelOpen = false
    private lateinit var panelPrefs: PanelPreferences

    companion object {
        const val CHANNEL_ID = "side_panel_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.originpanel.sidepanel.STOP"
        const val ACTION_OPEN = "com.originpanel.sidepanel.OPEN"
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        panelPrefs = PanelPreferences(this)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        addEdgeHandle()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_OPEN -> openPanel()
        }
        return START_STICKY // Auto-restart if killed by system
    }

    override fun onDestroy() {
        super.onDestroy()
        removeEdgeHandle()
        removeSidePanel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Edge Handle ──────────────────────────────────────────────────────────

    private fun addEdgeHandle() {
        val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
        val isPillVisible = panelPrefs.showPill
        
        edgeHandleView = EdgeHandleView(this).apply {
            onTrigger = { openPanel() }
            isRightSide = isRight
            showPill = isPillVisible
        }

        // 8dp for invisible, maybe 12dp if visible pill to make it more obvious
        val handleWidth = if (isPillVisible) 10 else 8

        val screenHeight = resources.displayMetrics.heightPixels
        val stripHeight  = if (isPillVisible) dpToPx(80) else (screenHeight * 0.60f).toInt()

        val params = WindowManager.LayoutParams(
            dpToPx(handleWidth),
            stripHeight,        // Fixed height for pill, 60% for invisible
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (isRight) Gravity.END or Gravity.CENTER_VERTICAL
                      else Gravity.START or Gravity.CENTER_VERTICAL
            x = 0
            y = 0
        }

        windowManager.addView(edgeHandleView, params)
    }

    private fun removeEdgeHandle() {
        edgeHandleView?.let {
            if (it.isAttachedToWindow) windowManager.removeView(it)
        }
        edgeHandleView = null
    }

    // ── Side Panel ───────────────────────────────────────────────────────────

    private fun openPanel() {
        if (isPanelOpen) return
        isPanelOpen = true

        sidePanelView = SidePanelView(this).apply {
            onClose = { closePanel() }
            onAppsChanged = { refreshApps() }
        }

        val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // FLAG_NOT_TOUCH_MODAL: taps outside the panel's actual bounds → dismissed
            // FLAG_WATCH_OUTSIDE_TOUCH: gives us ACTION_OUTSIDE events for backdrop dismiss
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (isRight) Gravity.END or Gravity.CENTER_VERTICAL
                      else Gravity.START or Gravity.CENTER_VERTICAL
        }

        windowManager.addView(sidePanelView, params)

        // Slide in with spring animation
        sidePanelView?.let { panel ->
            panel.post {
                val panelWidth = panel.width.toFloat()
                SpringAnimator.animateOpen(panel, panelWidth)
            }
        }

        // Hide edge handle while panel is open (cleaner UX)
        edgeHandleView?.visibility = View.GONE

        // Intercept outside taps to close panel
        sidePanelView?.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_OUTSIDE) {
                closePanel()
                true
            } else false
        }

        // Load apps into the panel
        refreshApps()
    }

    fun closePanel() {
        if (!isPanelOpen) return
        isPanelOpen = false

        sidePanelView?.let { panel ->
            val panelWidth = panel.width.toFloat()
            SpringAnimator.animateClose(panel, panelWidth) {
                removeSidePanel()
                edgeHandleView?.visibility = View.VISIBLE
            }
        }
    }

    private fun removeSidePanel() {
        sidePanelView?.let {
            if (it.isAttachedToWindow) windowManager.removeView(it)
        }
        sidePanelView = null
    }

    private fun refreshApps() {
        val apps = AppRepository(this).getPanelApps()
        sidePanelView?.setApps(apps)
    }

    // ── Notification (required for Android 8+ foreground service) ────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.panel_notification_channel),
            NotificationManager.IMPORTANCE_MIN  // Silent, collapsed
        ).apply {
            description = getString(R.string.panel_notification_desc)
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, FloatingPanelService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val openMainIntent = Intent(this, MainActivity::class.java)
        val openMainPending = PendingIntent.getActivity(
            this, 0, openMainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.panel_running))
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(openMainPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.stop_panel), stopPending)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}
