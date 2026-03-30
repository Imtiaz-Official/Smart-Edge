package com.imi.smartedge.sidebar.panel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.imi.smartedge.sidebar.panel.databinding.LayoutSecureSettingsDialogBinding

object SecureSettingsDialog {

    fun show(context: Context, onPermissionGranted: () -> Unit) {
        val binding = LayoutSecureSettingsDialogBinding.inflate(LayoutInflater.from(context))
        
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(binding.root)
            .create()

        val packageName = context.packageName
        val adbCommand = "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
        binding.tvAdbCommand.text = adbCommand

        binding.btnCopyAdb.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", adbCommand))
            binding.root.showModernToast("Command copied")
        }

        binding.btnGrantRoot.setOnClickListener {
            try {
                val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"))
                p.waitFor()
                if (context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    binding.root.showModernToast("Permission granted!")
                    onPermissionGranted()
                    dialog.dismiss()
                } else {
                    binding.root.showModernToast("Root grant failed")
                }
            } catch (e: Exception) {
                binding.root.showModernToast("Root not available")
            }
        }

        binding.btnClose.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
}