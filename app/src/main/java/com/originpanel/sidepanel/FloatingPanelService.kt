package com.originpanel.sidepanel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FloatingPanelService : Service() {

    private lateinit var windowManager: WindowManager
    private var edgeHandleView: EdgeHandleView? = null
    private var sidePanelView: SidePanelView? = null
    private var pickerPanelView: AppPickerPanelView? = null

    private var isPanelOpen = false
    private var isPickerOpen = false
    private lateinit var panelPrefs: PanelPreferences
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

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
        startForeground(NOTIFICATION_ID, buildNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)

        // ── Pre-warm Views for Zero Latency ──────────────────────────────────
        initSidePanel()
        initPickerPanel()
        addEdgeHandle()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_OPEN -> {
                android.util.Log.d("FloatingPanelService", "onStartCommand: ACTION_OPEN received")
                refreshApps()
                openPanel()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        removeView(edgeHandleView)
        removeView(sidePanelView)
        removeView(pickerPanelView)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun removeView(view: View?) {
        view?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
    }

    // ── Edge Handle ──────────────────────────────────────────────────────────

    private fun addEdgeHandle() {
        if (!panelPrefs.gesturesEnabled) return

        val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
        val isPillVisible = panelPrefs.showPill
        
        edgeHandleView = EdgeHandleView(this).apply {
            onTrigger = { openPanel() }
            isRightSide = isRight
            showPill = isPillVisible
            alpha = panelPrefs.panelOpacity / 100f
        }

        val handleWidth = panelPrefs.handleWidth
        val handleHeight = if (isPillVisible) dpToPx(panelPrefs.handleHeight) 
                           else (resources.displayMetrics.heightPixels * 0.60f).toInt()

        val params = WindowManager.LayoutParams(
            dpToPx(handleWidth),
            handleHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (isRight) Gravity.END or Gravity.CENTER_VERTICAL
                      else Gravity.START or Gravity.CENTER_VERTICAL
            y = dpToPx(panelPrefs.handleVerticalOffset)
        }

        windowManager.addView(edgeHandleView, params)
    }

    // ── Side Panel (Pre-loaded) ───────────────────────────────────────────────

    private fun initSidePanel() {
        sidePanelView = SidePanelView(this).apply {
            onClose = { closePanel() }
            onAppsChanged = { refreshApps() }
            onAddClick = { togglePicker() }
            visibility = View.GONE 
        }

        refreshApps()

        val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (isRight) Gravity.END or Gravity.CENTER_VERTICAL
                      else Gravity.START or Gravity.CENTER_VERTICAL
        }

        windowManager.addView(sidePanelView, params)
        
        sidePanelView?.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_OUTSIDE) {
                if (isPickerOpen) closePicker() else closePanel()
                true
            } else false
        }
    }

    private fun openPanel() {
        if (isPanelOpen) return
        isPanelOpen = true

        android.util.Log.d("FloatingPanelService", "openPanel: refreshing apps")
        refreshApps()
        sidePanelView?.scrollToTop() 
        sidePanelView?.let { panel ->
            panel.visibility = View.VISIBLE
            panel.post {
                val panelWidth = panel.width.toFloat()
                SpringAnimator.animateOpen(panel, panelWidth)
            }
        }

        edgeHandleView?.visibility = View.GONE
    }

    fun closePanel() {
        if (!isPanelOpen) return
        isPanelOpen = false

        if (isPickerOpen) closePicker()

        sidePanelView?.let { panel ->
            val panelWidth = panel.width.toFloat()
            SpringAnimator.animateClose(panel, panelWidth) {
                panel.visibility = View.GONE
                edgeHandleView?.visibility = View.VISIBLE
                panel.animatePickerToggle(false) 
            }
        }
    }

    private fun refreshApps() {
        serviceScope.launch {
            val apps = AppRepository(this@FloatingPanelService).getPanelApps()
            sidePanelView?.setApps(apps)
        }
    }

    // ── Picker Panel (Pre-loaded) ─────────────────────────────────────────────

    private fun initPickerPanel() {
        pickerPanelView = AppPickerPanelView(this).apply {
            onClose = { closePicker() }
            onToggleApp = { app, isSelected ->
                if (isSelected) panelPrefs.addApp(app.packageName)
                else panelPrefs.removeApp(app.packageName)
                refreshApps()
            }
            visibility = View.GONE 
        }

        val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (isRight) Gravity.CENTER_VERTICAL or Gravity.END
                      else Gravity.CENTER_VERTICAL or Gravity.START

            val sidePanelWidthPx = (104 * resources.displayMetrics.density).toInt()
            x = sidePanelWidthPx
        }

        windowManager.addView(pickerPanelView, params)
    }

    private fun togglePicker() {
        if (isPickerOpen) closePicker() else openPicker()
    }

    private fun openPicker() {
        if (isPickerOpen) return
        isPickerOpen = true

        // Squeeze side panel to 1 column to save space
        sidePanelView?.setColumns(1)
        sidePanelView?.animatePickerToggle(true)

        pickerPanelView?.let { picker ->
            picker.visibility = View.VISIBLE
            picker.post {
                val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
                val pickerWidth = picker.width.toFloat()
                SpringAnimator.animateOpen(picker, if (isRight) pickerWidth else -pickerWidth, isPicker = true)
            }
        }
    }

    private fun closePicker() {
        if (!isPickerOpen) return
        isPickerOpen = false

        // Restore original columns (respecting user preference)
        val originalCols = if (panelPrefs.isPremium) panelPrefs.panelColumns else 1
        sidePanelView?.setColumns(originalCols)
        sidePanelView?.animatePickerToggle(false)

        pickerPanelView?.let { picker ->
            val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
            val pickerWidth = picker.width.toFloat()
            SpringAnimator.animateClose(picker, if (isRight) pickerWidth else -pickerWidth, isPicker = true) {
                picker.visibility = View.GONE
            }
        }
    }

    // ── Utilities ────────────────────────────────────────────────────────────

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
