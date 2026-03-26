package com.imi.smartedge.sidebar.panel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
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
    
    private var rootLayout: android.widget.FrameLayout? = null
    private var rootParams: WindowManager.LayoutParams? = null

    private var isPanelOpen = false
    private var isPickerOpen = false
    private lateinit var panelPrefs: PanelPreferences
    private var lastPickerToggleTime = 0L
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private val systemDialogsReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS) {
                val reason = intent.getStringExtra("reason")
                if (reason == "homekey" || reason == "recentapps") {
                    closePanel()
                }
            }
        }
    }

    companion object {
        const val TAG = "FloatingPanelService"
        var isRunning = false
            private set
            
        const val CHANNEL_ID = "side_panel_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.imi.smartedge.sidebar.panel.STOP"
        const val ACTION_OPEN = "com.imi.smartedge.sidebar.panel.OPEN"
        const val ACTION_REFRESH = "com.imi.smartedge.sidebar.panel.REFRESH"
        const val ACTION_CLOSE_PANEL = "com.imi.smartedge.sidebar.panel.CLOSE_PANEL"
        const val ACTION_SHOW_TEMP = "com.imi.smartedge.sidebar.panel.SHOW_TEMP"
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        panelPrefs = PanelPreferences(this)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)

        initSidePanel()
        initPickerPanel()
        addEdgeHandle()

        val filter = android.content.IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(systemDialogsReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(systemDialogsReceiver, filter)
        }

        serviceScope.launch {
            if (panelPrefs.getPanelApps().isEmpty()) {
                val topApps = AppRepository(this@FloatingPanelService).getTop5Apps()
                panelPrefs.setPanelApps(topApps)
                refreshApps()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_OPEN -> {
                refreshApps {
                    openPanel()
                }
            }
            ACTION_REFRESH -> {
                edgeHandleView?.updateFromPrefs()
                sidePanelView?.updateStyles()
                pickerPanelView?.applyTheme()
                updateBlur(isPanelOpen)
                if (isPickerOpen) {
                    pickerPanelView?.loadApps() 
                }
                refreshApps()
            }
            ACTION_CLOSE_PANEL -> closePanel(immediate = false)
            ACTION_SHOW_TEMP -> {
                refreshApps {
                    openPanel()
                }
            }
        }
        return if (panelPrefs.serviceEnabled) START_STICKY else START_NOT_STICKY
    }

    fun triggerScreenshot() {
        closePanel()
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, PanelAccessibilityService::class.java).apply {
                action = PanelAccessibilityService.ACTION_TAKE_SCREENSHOT
            }
            startService(intent)
        }, 300)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
        try {
            unregisterReceiver(systemDialogsReceiver)
        } catch (e: Exception) {}
        removeView(edgeHandleView)
        removeView(sidePanelView)
        removeView(pickerPanelView)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun removeView(view: View?) {
        if (view == null) return
        try {
            if (view.isAttachedToWindow) {
                windowManager.removeView(view)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing view: ${e.message}")
        }
    }

    private fun addEdgeHandle() {
        if (!panelPrefs.gesturesEnabled) return
        removeView(edgeHandleView)
        edgeHandleView = null

        val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
        val isPillVisible = panelPrefs.showPill
        
        edgeHandleView = EdgeHandleView(this).apply {
            onTrigger = { 
                refreshApps {
                    openPanel() 
                }
            }
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

    private fun initSidePanel() {
        sidePanelView = SidePanelView(this).apply {
            onClose = { closePanel() }
            onAppsChanged = { refreshApps() }
            onAddClick = { isEdit -> togglePicker(isEdit) }
            onScreenshot = { 
                closePanel()
                Handler(Looper.getMainLooper()).postDelayed({
                    triggerScreenshot()
                }, 300)
            }
            visibility = View.GONE 
        }
        refreshApps()
    }

    private fun initPickerPanel() {
        pickerPanelView = AppPickerPanelView(this).apply {
            onClose = { closePicker() }
            onAppLaunched = { closePanel() }
            onToggleApp = { app, isSelected ->
                if (isSelected) {
                    panelPrefs.addApp(app.packageName)
                    refreshApps {
                        if (isPickerOpen) {
                            sidePanelView?.scrollToApp(app.packageName)
                        }
                    }
                } else {
                    panelPrefs.removeApp(app.packageName)
                    refreshApps()
                }
            }
            visibility = View.GONE 
        }
    }

    private val sideRect = android.graphics.Rect()
    private val pickerRect = android.graphics.Rect()

    private fun initRootLayout() {
        if (rootLayout != null) return
        
        rootLayout = android.widget.FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#01000000")) 
            setOnTouchListener { _, event ->
                val x = event.rawX.toInt()
                val y = event.rawY.toInt()
                sidePanelView?.getPanelCardRect(sideRect)
                val insidePicker = if (isPickerOpen) {
                    pickerPanelView?.getPickerCardRect(pickerRect)
                    pickerRect.contains(x, y)
                } else false
                val insideSide = sideRect.contains(x, y)
                if (insideSide || insidePicker) {
                    return@setOnTouchListener false
                }
                if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                    closePanel()
                }
                true
            }
        }

        rootParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )
        
        rootLayout?.addView(sidePanelView)
        rootLayout?.addView(pickerPanelView)
    }

    private fun openPanel() {
        if (isPanelOpen) return
        isPanelOpen = true
        initRootLayout()
        if (rootLayout?.parent == null) {
            windowManager.addView(rootLayout, rootParams)
        }
        updateBlur(true)
        sidePanelView?.updateSideLayout() // Force layout update based on side
        sidePanelView?.scrollToTop() 
        sidePanelView?.let { panel ->
            val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
            val lp = panel.layoutParams as android.widget.FrameLayout.LayoutParams
            lp.width = android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            lp.height = android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            lp.gravity = if (isRight) Gravity.END or Gravity.CENTER_VERTICAL
                         else Gravity.START or Gravity.CENTER_VERTICAL
            panel.layoutParams = lp
            panel.alpha = 0f
            panel.translationX = if (isRight) 1000f else -1000f
            panel.visibility = View.VISIBLE
            panel.post {
                val panelWidth = panel.width.toFloat()
                val stiffness = panelPrefs.animSpeed.toFloat()
                SpringAnimator.animateOpen(panel, if (isRight) panelWidth else -panelWidth, stiffness = stiffness)
            }
        }
        edgeHandleView?.visibility = View.GONE
    }

    private fun updateBlur(enabled: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val shouldBlur = enabled && panelPrefs.blurEnabled
        val blurRadius = panelPrefs.blurAmount
        rootParams?.let { params ->
            if (shouldBlur) {
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                params.blurBehindRadius = blurRadius
            } else {
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
                params.blurBehindRadius = 0
            }
            if (rootLayout?.parent != null) {
                windowManager.updateViewLayout(rootLayout, params)
            }
        }
    }

    fun closePanel(immediate: Boolean = false) {
        if (!isPanelOpen) return
        isPanelOpen = false
        if (immediate) {
            if (isPickerOpen) {
                isPickerOpen = false
                pickerPanelView?.visibility = View.GONE
            }
            sidePanelView?.visibility = View.GONE
            updateBlur(false)
            if (rootLayout?.parent != null) windowManager.removeView(rootLayout)
            edgeHandleView?.visibility = View.VISIBLE
            sidePanelView?.animatePickerToggle(false)
            return
        }
        if (isPickerOpen) closePicker()
        sidePanelView?.let { panel ->
            val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
            val panelWidth = panel.width.toFloat()
            val stiffness = panelPrefs.animSpeed.toFloat()
            SpringAnimator.animateClose(panel, if (isRight) panelWidth else -panelWidth, stiffness = stiffness) {
                panel.visibility = View.GONE
                updateBlur(false)
                if (rootLayout?.parent != null) windowManager.removeView(rootLayout)
                edgeHandleView?.visibility = View.VISIBLE
                panel.animatePickerToggle(false) 
            }
        }
    }

    private fun togglePicker(enableEditMode: Boolean = true) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPickerToggleTime < 600) return
        lastPickerToggleTime = currentTime
        if (isPickerOpen) {
            val currentModeIsEdit = pickerPanelView?.isEditMode ?: false
            if (enableEditMode && !currentModeIsEdit) {
                pickerPanelView?.setEditMode(true)
            } else {
                closePicker()
            }
        } else {
            openPicker(enableEditMode = enableEditMode)
        }
    }

    private fun openPicker(enableEditMode: Boolean = false) {
        if (isPickerOpen) return
        isPickerOpen = true
        sidePanelView?.setColumns(1)
        sidePanelView?.setEditButtonVisible(true)
        sidePanelView?.scrollToBottom()
        sidePanelView?.animatePickerToggle(true)
        pickerPanelView?.let { picker ->
            picker.setEditMode(enableEditMode)
            picker.resetSearch()
            picker.loadApps()
            picker.setOnClickListener { }
            val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
            val density = resources.displayMetrics.density
            val sidePanelWidthDp = 72
            val lp = android.widget.FrameLayout.LayoutParams(dpToPx(240), android.widget.FrameLayout.LayoutParams.WRAP_CONTENT)
            lp.gravity = if (isRight) Gravity.CENTER_VERTICAL or Gravity.END
                         else Gravity.CENTER_VERTICAL or Gravity.START
            val gapPx = ((sidePanelWidthDp + panelPrefs.pickerGap) * density).toInt()
            if (isRight) lp.marginEnd = gapPx else lp.marginStart = gapPx
            picker.layoutParams = lp
            picker.alpha = 0f
            picker.visibility = View.VISIBLE
            picker.post {
                val pickerWidth = picker.width.toFloat()
                val startX = if (isRight) -pickerWidth else pickerWidth
                val stiffness = panelPrefs.animSpeed.toFloat()
                SpringAnimator.animateOpen(picker, startX, isPicker = true, stiffness = stiffness)
            }
        }
    }

    private fun closePicker() {
        if (!isPickerOpen) return
        isPickerOpen = false
        sidePanelView?.animatePickerToggle(false)
        sidePanelView?.scrollToTop()
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isPickerOpen) {
                val originalCols = if (panelPrefs.isUnlocked) panelPrefs.panelColumns else 1
                sidePanelView?.setEditButtonVisible(false) 
                sidePanelView?.setColumns(originalCols)
            }
        }, 250)
        pickerPanelView?.let { picker ->
            picker.setEditMode(false)
            picker.invalidateAppList()
            val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
            val pickerWidth = picker.width.toFloat()
            val stiffness = panelPrefs.animSpeed.toFloat()
            SpringAnimator.animateClose(picker, if (isRight) pickerWidth else -pickerWidth, isPicker = true, stiffness = stiffness) {
                if (!isPickerOpen) {
                    picker.visibility = View.GONE
                }
            }
        }
    }

    private fun refreshApps(onComplete: (() -> Unit)? = null) {
        serviceScope.launch {
            val apps = AppRepository(this@FloatingPanelService).getPanelApps()
            sidePanelView?.setApps(apps, onComplete)
        }
    }

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
