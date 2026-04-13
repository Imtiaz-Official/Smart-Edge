package com.imi.smartedge.sidebar.panel

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.imi.smartedge.sidebar.panel.databinding.LayoutSecureSettingsDialogBinding

object SecureSettingsDialog {

    fun show(context: Context, onPermissionGranted: () -> Unit) {
        val binding = LayoutSecureSettingsDialogBinding.inflate(LayoutInflater.from(context))
        
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(binding.root)
            .create()

        fun updateStatus() {
            val isGranted = context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
            
            // Clear existing animations
            binding.statusDot.clearAnimation()
            
            if (isGranted) {
                binding.tvStatus.text = "ACTIVE"
                binding.statusCard.setCardBackgroundColor(context.getColor(R.color.md_theme_primaryContainer))
                binding.tvStatus.setTextColor(context.getColor(R.color.md_theme_onPrimaryContainer))
                binding.statusDot.setCardBackgroundColor(context.getColor(R.color.md_theme_primary))
            } else {
                binding.tvStatus.text = "INACTIVE"
                binding.statusCard.setCardBackgroundColor(context.getColor(R.color.md_theme_errorContainer))
                binding.tvStatus.setTextColor(context.getColor(R.color.md_theme_onErrorContainer))
                binding.statusDot.setCardBackgroundColor(context.getColor(R.color.md_theme_error))
                
                // Add pulse animation for Inactive state
                val pulse = android.view.animation.AlphaAnimation(1f, 0.4f).apply {
                    duration = 800
                    repeatMode = android.view.animation.Animation.REVERSE
                    repeatCount = android.view.animation.Animation.INFINITE
                }
                binding.statusDot.startAnimation(pulse)
            }
        }

        updateStatus()

        val packageName = context.packageName
        val adbCommand = "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
        binding.tvAdbCommand.text = adbCommand

        binding.btnCopyAdb.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", adbCommand))
            binding.root.showModernToast("Command copied")
        }

        binding.btnGrantShizuku.setOnClickListener {
            try {
                if (rikka.shizuku.Shizuku.pingBinder()) {
                    if (rikka.shizuku.Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                        try {
                            val sh = "pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
                            val process = rikka.shizuku.Shizuku.newProcess(arrayOf("sh", "-c", sh), null, null)
                            process.waitFor()
                            if (context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
                                binding.root.showModernToast("Permission granted via Shizuku!")
                                updateStatus()
                                onPermissionGranted()
                            } else {
                                binding.root.showModernToast("Shizuku grant failed")
                            }
                        } catch (e: Exception) {
                            binding.root.showModernToast("Shizuku execution failed")
                        }
                    } else {
                        rikka.shizuku.Shizuku.requestPermission(1001)
                        binding.root.showModernToast("Allow Shizuku and try again")
                    }
                } else {
                    binding.root.showModernToast("Shizuku is not running")
                }
            } catch (e: Exception) {
                binding.root.showModernToast("Shizuku unavailable")
            }
        }

        binding.btnRevokeShizuku.setOnClickListener {
            try {
                if (rikka.shizuku.Shizuku.pingBinder()) {
                    if (rikka.shizuku.Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                        try {
                            val sh = "pm revoke $packageName android.permission.WRITE_SECURE_SETTINGS"
                            val process = rikka.shizuku.Shizuku.newProcess(arrayOf("sh", "-c", sh), null, null)
                            process.waitFor()
                            if (context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                                binding.root.showModernToast("Permission revoked!")
                                updateStatus()
                                onPermissionGranted()
                            }
                        } catch (e: Exception) {
                            binding.root.showModernToast("Shizuku revoke failed")
                        }
                    } else {
                        rikka.shizuku.Shizuku.requestPermission(1001)
                    }
                }
            } catch (e: Exception) {}
        }

        binding.btnGrantRoot.setOnClickListener {
            try {
                val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"))
                p.waitFor()
                if (context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
                    binding.root.showModernToast("Permission granted via Root!")
                    updateStatus()
                    onPermissionGranted()
                } else {
                    binding.root.showModernToast("Root grant failed")
                }
            } catch (e: Exception) {
                binding.root.showModernToast("Root not available")
            }
        }

        binding.btnRevokeRoot.setOnClickListener {
            try {
                val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "pm revoke $packageName android.permission.WRITE_SECURE_SETTINGS"))
                p.waitFor()
                if (context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                    binding.root.showModernToast("Permission revoked!")
                    updateStatus()
                    onPermissionGranted()
                }
            } catch (e: Exception) {
                binding.root.showModernToast("Root revoke failed")
            }
        }

        binding.btnClose.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
}
