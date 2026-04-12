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

        binding.btnGrantShizuku.setOnClickListener {
            try {
                if (rikka.shizuku.Shizuku.pingBinder()) {
                    if (rikka.shizuku.Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        try {
                            val process = rikka.shizuku.Shizuku.newProcess(arrayOf("sh", "-c", "pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"), null, null)
                            process.waitFor()
                            if (context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                binding.root.showModernToast("Permission granted via Shizuku!")
                                onPermissionGranted()
                                dialog.dismiss()
                            } else {
                                binding.root.showModernToast("Shizuku grant failed")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ShizukuExec", "Execution error", e)
                            binding.root.showModernToast("Shizuku execution failed: ${e.message}")
                        }
                    } else {
                        rikka.shizuku.Shizuku.requestPermission(1001)
                        binding.root.showModernToast("Requesting Shizuku... Try again after allowing.")
                    }
                } else {
                    binding.root.showModernToast("Shizuku is not running")
                }
            } catch (e: Exception) {
                binding.root.showModernToast("Shizuku not installed or available")
            }
        }

        binding.btnRevokeShizuku.setOnClickListener {
            try {
                if (rikka.shizuku.Shizuku.pingBinder()) {
                    if (rikka.shizuku.Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        try {
                            val process = rikka.shizuku.Shizuku.newProcess(arrayOf("sh", "-c", "pm revoke $packageName android.permission.WRITE_SECURE_SETTINGS"), null, null)
                            process.waitFor()
                            if (context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                binding.root.showModernToast("Permission revoked via Shizuku!")
                                onPermissionGranted()
                                dialog.dismiss()
                            } else {
                                binding.root.showModernToast("Shizuku revoke failed")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ShizukuExec", "Execution error", e)
                            binding.root.showModernToast("Shizuku execution failed: ${e.message}")
                        }
                    } else {
                        rikka.shizuku.Shizuku.requestPermission(1001)
                        binding.root.showModernToast("Requesting Shizuku... Try again after allowing.")
                    }
                } else {
                    binding.root.showModernToast("Shizuku is not running")
                }
            } catch (e: Exception) {
                binding.root.showModernToast("Shizuku not installed or available")
            }
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

        binding.btnRevokeRoot.setOnClickListener {
            try {
                val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "pm revoke $packageName android.permission.WRITE_SECURE_SETTINGS"))
                p.waitFor()
                if (context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    binding.root.showModernToast("Permission revoked via Root!")
                    onPermissionGranted()
                    dialog.dismiss()
                } else {
                    binding.root.showModernToast("Root revoke failed")
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