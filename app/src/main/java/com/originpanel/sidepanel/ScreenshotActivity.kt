package com.originpanel.sidepanel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast

class ScreenshotActivity : Activity() {

    private lateinit var projectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure the activity is as invisible as possible before the system dialog pops up
        window.setBackgroundDrawableResource(android.R.color.transparent)
        
        Log.d("ScreenshotActivity", "onCreate: requesting permission")
        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("ScreenshotActivity", "onActivityResult: code=$resultCode")
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                Log.d("ScreenshotActivity", "Permission granted, starting service elevation")
                val elevateIntent = Intent(this, FloatingPanelService::class.java).apply {
                    action = FloatingPanelService.ACTION_SCREENSHOT
                    putExtra("RESULT_CODE", resultCode)
                    putExtra("DATA", data)
                }
                startForegroundService(elevateIntent)
                finish()
            } else {
                Log.e("ScreenshotActivity", "Permission denied or data null")
                Toast.makeText(this, "Screenshot permission denied", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE = 100
    }
}
