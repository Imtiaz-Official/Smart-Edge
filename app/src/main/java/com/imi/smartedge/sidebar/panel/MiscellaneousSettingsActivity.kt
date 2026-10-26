package com.imi.smartedge.sidebar.panel

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.imi.smartedge.sidebar.panel.databinding.ActivitySettingsMiscBinding
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MiscellaneousSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsMiscBinding
    private lateinit var panelPrefs: PanelPreferences

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    private val importFilePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        try {
            val stream = contentResolver.openInputStream(uri) ?: return@registerForActivityResult
            val json = BufferedReader(InputStreamReader(stream)).use { it.readText() }
            stream.close()
            val success = panelPrefs.importFromJson(json)
            if (success) {
                applyGlobalRefresh()
                binding.root.showModernToast("Settings imported successfully!")
            } else {
                binding.root.showModernToast("Invalid backup file – import failed")
            }
        } catch (e: Exception) {
            binding.root.showModernToast("Could not read file: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsMiscBinding.inflate(layoutInflater)
        setContentView(binding.root)

        panelPrefs = PanelPreferences(this)

        binding.toolbar.setNavigationOnClickListener { finish() }

        updateLanguageLabel()
        binding.featureLanguage.setOnClickListener { showLanguagePicker() }

        binding.btnExportSettings.setOnClickListener {
            exportSettingsToDownloads()
        }

        binding.btnImportSettings.setOnClickListener {
            importFilePicker.launch("application/json")
        }

        binding.btnCrashLogs.setOnClickListener {
            showCrashLogDialog()
        }
    }

    private fun updateLanguageLabel() {
        binding.tvLanguageValue.text = when (panelPrefs.appLanguage) {
            "es" -> "Español"
            "uk" -> "Українська"
            else -> "English"
        }
    }

    private fun showLanguagePicker() {
        val languages = arrayOf("English", "Español", "Українська")
        val codes = arrayOf("en", "es", "uk")
        val currentIndex = codes.indexOf(panelPrefs.appLanguage).let { if (it == -1) 0 else it }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.misc_app_language))
            .setSingleChoiceItems(languages, currentIndex) { dialog, which ->
                val selected = codes[which]
                if (selected != panelPrefs.appLanguage) {
                    panelPrefs.appLanguage = selected
                    LocaleHelper.setLocale(this, selected)
                    
                    // Restart app
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showCrashLogDialog() {
        val log = CrashLogger.getCrashLog(this)
        if (log.isNullOrBlank()) {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Diagnostic Logs")
                .setMessage("No crash reports found. The application has been running smoothly without uncaught errors.")
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }

        val scroll = android.widget.ScrollView(this)
        val tv = android.widget.TextView(this).apply {
            text = log
            textSize = 11f
            typeface = android.graphics.Typeface.MONOSPACE
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            setTextIsSelectable(true)
        }
        scroll.addView(tv)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Crash & Diagnostic Logs")
            .setView(scroll)
            .setPositiveButton("Copy") { _, _ ->
                val cm = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("Crash Log", log))
                binding.root.showModernToast("Crash log copied to clipboard")
            }
            .setNeutralButton("Clear") { _, _ ->
                CrashLogger.clearCrashLog(this)
                binding.root.showModernToast("Logs cleared")
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun exportSettingsToDownloads() {
        try {
            val json = panelPrefs.exportToJson()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "smartedge_backup_$timestamp.json"
            val folderName = "SidePanel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Scoped storage — write via MediaStore to Downloads/SidePanel/
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$folderName")
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    binding.root.showModernToast("Saved to Downloads/$folderName/$fileName")
                } else {
                    binding.root.showModernToast("Export failed – could not create file")
                }
            } else {
                // Legacy — write directly to Downloads/SidePanel/
                val dir = java.io.File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    folderName
                )
                dir.mkdirs()
                java.io.File(dir, fileName).writeText(json)
                binding.root.showModernToast("Saved to Downloads/$folderName/$fileName")
            }
        } catch (e: Exception) {
            binding.root.showModernToast("Export failed: ${e.message}")
        }
    }

    private fun applyGlobalRefresh() {
        val intent = Intent(this, FloatingPanelService::class.java).apply {
            action = FloatingPanelService.ACTION_REFRESH
        }
        startService(intent)
    }
}
