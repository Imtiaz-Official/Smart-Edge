package com.originpanel.sidepanel

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    
    // Cache for MediaProjection permission data to allow "one-tap" screenshots
    private var savedResultCode: Int = 0
    private var savedData: Intent? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    // Standard Android system broadcast to detect Home / Recent Apps presses
    // This works WITHOUT accessibility permission.
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
        const val ACTION_STOP = "com.originpanel.sidepanel.STOP"
        const val ACTION_OPEN = "com.originpanel.sidepanel.OPEN"
        const val ACTION_REFRESH = "com.originpanel.sidepanel.REFRESH"
        const val ACTION_SCREENSHOT = "com.originpanel.sidepanel.SCREENSHOT"
        const val ACTION_CLOSE_PANEL = "com.originpanel.sidepanel.CLOSE_PANEL"
        // Sent by SettingsActivity after restart to re-open the panel with new settings applied
        const val ACTION_SHOW_TEMP = "com.originpanel.sidepanel.SHOW_TEMP"
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        Log.d(TAG, "onCreate")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        panelPrefs = PanelPreferences(this)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)

        initSidePanel()
        initPickerPanel()
        addEdgeHandle()

        // Register receiver for Home / Recent Apps presses and Open Picker requests
        val filter = android.content.IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS).apply {
            addAction("com.originpanel.sidepanel.ACTION_OPEN_PICKER")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(systemDialogsReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(systemDialogsReceiver, filter)
        }



        // Smart setup: Populates if empty
        serviceScope.launch {
            if (panelPrefs.getPanelApps().isEmpty()) {
                val topApps = AppRepository(this@FloatingPanelService).getTop5Apps()
                panelPrefs.setPanelApps(topApps)
                refreshApps()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action: ${intent?.action}")
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_OPEN -> {
                refreshApps()
                openPanel()
            }
            ACTION_REFRESH -> {
                pickerPanelView?.invalidateAppList() // Force full reload of metadata
                pickerPanelView?.loadApps(true)      // Trigger immediate reload
                refreshApps()
            }
            ACTION_CLOSE_PANEL -> {
                closePanel(immediate = false)
            }
            // Sent by SettingsActivity after service restart — open panel so user sees new settings
            ACTION_SHOW_TEMP -> {
                refreshApps()
                openPanel()
            }
            ACTION_SCREENSHOT -> {
                val resultCode = intent.getIntExtra("RESULT_CODE", 0)
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra("DATA", Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra("DATA")
                }
                
                if (resultCode == Activity.RESULT_OK && data != null) {
                    savedResultCode = resultCode
                    savedData = data
                    performScreenshot(resultCode, data)
                }
            }
        }
        return START_STICKY
    }

    fun triggerScreenshot() {
        val resultCode = savedResultCode
        val data = savedData
        
        if (resultCode == Activity.RESULT_OK && data != null) {
            // We have cached permission, capture immediately
            closePanel()
            // Small delay to let the panel disappear
            Handler(Looper.getMainLooper()).postDelayed({
                performScreenshot(resultCode, data)
            }, 300)
        } else {
            // Need to ask for permission
            // No need to closePanel yet, ScreenshotActivity will handle it or we can do it now
            closePanel()
            val intent = Intent(this, ScreenshotActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    private fun performScreenshot(resultCode: Int, data: Intent) {
        showCaptureFlash {
            // This runs at the peak of the flash
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, buildNotification(), 
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or 
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                }

                val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val metrics = resources.displayMetrics
                val width = metrics.widthPixels
                val height = metrics.heightPixels
                val density = metrics.densityDpi

                val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
                val projection = projectionManager.getMediaProjection(resultCode, data)
                
                projection.registerCallback(object : MediaProjection.Callback() {}, Handler(Looper.getMainLooper()))
                
                val virtualDisplay = projection.createVirtualDisplay(
                    "Screenshot", width, height, density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    imageReader.surface, null, null
                )

                // Give it a tiny moment to actually render the frame
                Handler(Looper.getMainLooper()).postDelayed({
                    val image = imageReader.acquireLatestImage()
                    if (image != null) {
                        val planes = image.planes
                        val buffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride
                        val rowPadding = rowStride - pixelStride * width

                        val bitmap = Bitmap.createBitmap(
                            width + rowPadding / pixelStride,
                            height, Bitmap.Config.ARGB_8888
                        )
                        bitmap.copyPixelsFromBuffer(buffer)
                        
                        val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                        saveBitmap(croppedBitmap)
                        
                        image.close()
                        virtualDisplay.release()
                        projection.stop()
                        
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(applicationContext, "Screenshot Saved to DCIM/SidePanel", Toast.LENGTH_LONG).show()
                        }
                        
                        startForeground(NOTIFICATION_ID, buildNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                    }
                }, 150)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showCaptureFlash(onPeak: () -> Unit) {
        Handler(Looper.getMainLooper()).post {
            // Re-use the existing rootLayout for the flash to avoid WindowManager addView jitters
            if (rootLayout == null) initRootLayout()
            
            val root = rootLayout ?: return@post
            if (root.parent == null) {
                windowManager.addView(root, rootParams)
            }

            // Create a dedicated flash overlay inside root to keep background logic clean
            val flashOverlay = View(this).apply {
                setBackgroundColor(android.graphics.Color.WHITE)
                alpha = 0f
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }
            root.addView(flashOverlay, android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT)

            // Native-smooth animation using hardware layer
            flashOverlay.animate()
                .alpha(0.7f)
                .setDuration(120)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .withEndAction {
                    onPeak() // Capture at peak
                    flashOverlay.animate()
                        .alpha(0f)
                        .setDuration(350)
                        .setInterpolator(android.view.animation.AccelerateInterpolator())
                        .withEndAction {
                            root.removeView(flashOverlay)
                            // If panel isn't open, remove the root layout from WM to release resources
                            if (!isPanelOpen && root.parent != null) {
                                windowManager.removeView(root)
                            }
                        }
                        .start()
                }
                .start()
        }
    }

    private fun saveBitmap(bitmap: Bitmap) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Screenshot_$timeStamp.png"
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "SidePanel")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        android.media.MediaScannerConnection.scanFile(this, arrayOf(file.absolutePath), null, null)
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
        view?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
    }

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

    private fun initSidePanel() {
        sidePanelView = SidePanelView(this).apply {
            onClose = { closePanel() }
            onAppsChanged = { refreshApps() }
            // Clicking the Arrow (btnClose) toggles All Apps
            onAddClick = { isEdit -> togglePicker(isEdit) }
            onScreenshot = { 
                // Close panel first
                closePanel()
                // Small delay to let the animation complete
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
                        // Only scroll if we are currently in picker mode
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

    private fun initRootLayout() {
        if (rootLayout != null) return
        
        rootLayout = android.widget.FrameLayout(this).apply {
            // Invisible but intercepts touches
            setBackgroundColor(android.graphics.Color.parseColor("#01000000")) 
            setOnTouchListener { _, event ->
                val x = event.rawX.toInt()
                val y = event.rawY.toInt()

                // 1. Check if the touch is inside the Side Panel
                val sideRect = android.graphics.Rect()
                sidePanelView?.getPanelCardRect(sideRect)
                
                // 2. Check if the touch is inside the App Picker
                val pickerRect = android.graphics.Rect()
                if (isPickerOpen) {
                    pickerPanelView?.getPickerCardRect(pickerRect)
                }

                val insideSide = sideRect.contains(x, y)
                val insidePicker = isPickerOpen && pickerRect.contains(x, y)

                if (insideSide || insidePicker) {
                    // Let the touch pass through to the panel (for scrolling/clicking)
                    return@setOnTouchListener false
                }

                // 3. Touch is in the "Void" (empty space)
                // Close immediately on touch start for maximum responsiveness
                if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                    closePanel()
                }
                true // Consume the touch sequence in the void
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
        
        // Add children to root
        rootLayout?.addView(sidePanelView)
        rootLayout?.addView(pickerPanelView)
    }

    private fun openPanel() {
        if (isPanelOpen) return
        isPanelOpen = true
        
        initRootLayout()
        
        // Add the root layout to WindowManager (Full Screen Invisible Overlay)
        if (rootLayout?.parent == null) {
            windowManager.addView(rootLayout, rootParams)
        }
        
        updateBlur(true)
        
        refreshApps()
        sidePanelView?.scrollToTop() 
        sidePanelView?.let { panel ->
            // Update its internal layout based on side
            val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
            val lp = panel.layoutParams as android.widget.FrameLayout.LayoutParams
            lp.width = android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            lp.height = android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            lp.gravity = if (isRight) Gravity.END or Gravity.CENTER_VERTICAL
                         else Gravity.START or Gravity.CENTER_VERTICAL
            panel.layoutParams = lp
            
            // PRE-CONDITION: Set state before making visible
            panel.alpha = 0f
            panel.translationX = if (isRight) 1000f else -1000f
            panel.visibility = View.VISIBLE
            
            panel.post {
                val panelWidth = panel.width.toFloat()
                SpringAnimator.animateOpen(panel, if (isRight) panelWidth else -panelWidth)
            }
        }
        edgeHandleView?.visibility = View.GONE
    }

    private fun updateBlur(enabled: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val shouldBlur = enabled && panelPrefs.blurEnabled
        
        rootParams?.let { params ->
            if (shouldBlur) {
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                params.blurBehindRadius = 50
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
            SpringAnimator.animateClose(panel, if (isRight) panelWidth else -panelWidth) {
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
        if (currentTime - lastPickerToggleTime < 600) return // Physics-aligned debounce
        lastPickerToggleTime = currentTime

        if (isPickerOpen) {
            val currentModeIsEdit = pickerPanelView?.isEditMode ?: false
            
            if (enableEditMode && !currentModeIsEdit) {
                // Switch to Edit Mode if requested from Add button
                pickerPanelView?.setEditMode(true)
            } else {
                // Otherwise close (clicking same button or clicking arrow while open)
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
        sidePanelView?.setEditButtonVisible(true) // Show Edit Button when picker is open
        sidePanelView?.scrollToBottom() // Focus on the new button
        sidePanelView?.animatePickerToggle(true)
        pickerPanelView?.let { picker ->
            picker.setEditMode(enableEditMode)
            picker.resetSearch()        // Fix #8: clear stale search on each open
            picker.loadApps()           // uses cached data unless invalidated
            
            // Consume touches so they don't hit rootLayout's close listener
            picker.setOnClickListener { }

            // Position it correctly relative to side panel
            val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
            val density = resources.displayMetrics.density
            val cols = if (panelPrefs.isPremium) panelPrefs.panelColumns else 1
            val sidePanelWidthDp = if (cols == 2) 150 else 72
            
            val lp = android.widget.FrameLayout.LayoutParams(dpToPx(240), android.widget.FrameLayout.LayoutParams.WRAP_CONTENT)
            lp.gravity = if (isRight) Gravity.CENTER_VERTICAL or Gravity.END
                         else Gravity.CENTER_VERTICAL or Gravity.START
            
            val gapPx = ((sidePanelWidthDp + 24) * density).toInt()
            if (isRight) lp.marginEnd = gapPx else lp.marginStart = gapPx
            picker.layoutParams = lp

            // PRE-CONDITION: Set alpha before making visible
            picker.alpha = 0f
            picker.visibility = View.VISIBLE
            picker.post {
                val pickerWidth = picker.width.toFloat()
                // Picker on right moves LEFT from panel (-width), on left moves RIGHT (+width)
                val startX = if (isRight) -pickerWidth else pickerWidth
                SpringAnimator.animateOpen(picker, startX, isPicker = true)
            }
        }
    }

    private fun closePicker() {
        if (!isPickerOpen) return
        isPickerOpen = false
        
        // 1. Start animations and smooth scroll immediately
        sidePanelView?.animatePickerToggle(false)
        sidePanelView?.scrollToTop()
        
        // 2. Delay structural layout changes slightly to let the scroll settle
        // This prevents "hiccups" where columns change mid-scroll
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isPickerOpen) { // Double check we didn't re-open
                val originalCols = if (panelPrefs.isPremium) panelPrefs.panelColumns else 1
                sidePanelView?.setEditButtonVisible(false) 
                sidePanelView?.setColumns(originalCols)
            }
        }, 250)
        
        pickerPanelView?.let { picker ->
            picker.setEditMode(false)       // Reset edit mode
            picker.invalidateAppList()      // Fix #9: force refresh next open (catches new installs)
            val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
            val pickerWidth = picker.width.toFloat()
            SpringAnimator.animateClose(picker, if (isRight) pickerWidth else -pickerWidth, isPicker = true) {
                // Defensive check: only hide if we are still supposed to be closed
                if (!isPickerOpen) {
                    picker.visibility = View.GONE
                }
            }
        }
    }

    private fun refreshApps(onComplete: (() -> Unit)? = null) {
        serviceScope.launch {
            // getPanelApps in repository is already optimized to use the cache
            // and load the real icons.
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
