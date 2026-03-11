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
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class FloatingPanelService : Service() {

    private lateinit var windowManager: WindowManager
    private var edgeHandleView: EdgeHandleView? = null
    private var sidePanelView: SidePanelView? = null
    private var pickerPanelView: AppPickerPanelView? = null

    private var isPanelOpen = false
    private var isPickerOpen = false
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
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        removeEdgeHandle()
        removeSidePanel()
        removePickerPanel()
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

        val handleWidth = 24
        val screenHeight = resources.displayMetrics.heightPixels
        val stripHeight  = if (isPillVisible) dpToPx(80) else (screenHeight * 0.60f).toInt()

        val params = WindowManager.LayoutParams(
            dpToPx(handleWidth),
            stripHeight,
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
            onAddClick = { togglePicker() }
        }

        val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
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

        sidePanelView?.let { panel ->
            panel.post {
                val panelWidth = panel.width.toFloat()
                SpringAnimator.animateOpen(panel, panelWidth)
            }
        }

        edgeHandleView?.visibility = View.GONE

        sidePanelView?.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_OUTSIDE) {
                if (isPickerOpen) closePicker() else closePanel()
                true
            } else false
        }

        refreshApps()
    }

    fun closePanel() {
        if (!isPanelOpen) return
        isPanelOpen = false

        if (isPickerOpen) closePicker()

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

    // ── Picker Panel ─────────────────────────────────────────────────────────

    private fun togglePicker() {
        if (isPickerOpen) closePicker() else openPicker()
    }

    private fun openPicker() {
        if (isPickerOpen) return
        isPickerOpen = true

        pickerPanelView = AppPickerPanelView(this).apply {
            onClose = { closePicker() }
            onToggleApp = { app, isSelected ->
                if (isSelected) panelPrefs.addApp(app.packageName)
                else panelPrefs.removeApp(app.packageName)
                refreshApps()
            }
        }

        val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Gravity CENTER to make it look like a large card in the middle-ish
            gravity = if (isRight) Gravity.CENTER_VERTICAL or Gravity.END
                      else Gravity.CENTER_VERTICAL or Gravity.START
            
            // Large offset to the side of the bar
            val offsetPx = (100 * resources.displayMetrics.density).toInt()
            x = offsetPx
        }

        windowManager.addView(pickerPanelView, params)

        pickerPanelView?.let { picker ->
            picker.post {
                val pickerWidth = picker.width.toFloat()
                SpringAnimator.animateOpen(picker, if (isRight) pickerWidth else -pickerWidth)
            }
        }
    }

    private fun closePicker() {
        if (!isPickerOpen) return
        isPickerOpen = false

        pickerPanelView?.let { picker ->
            val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
            val pickerWidth = picker.width.toFloat()
            SpringAnimator.animateClose(picker, if (isRight) pickerWidth else -pickerWidth) {
                removePickerPanel()
            }
        }
    }

    private fun removePickerPanel() {
        pickerPanelView?.let {
            if (it.isAttachedToWindow) windowManager.removeView(it)
        }
        pickerPanelView = null
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.panel_notification_channel),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = getString(R.string.panel_notification_desc)
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): android.app.Notification {
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

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}