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

        binding.btnExportSettings.setOnClickListener {
            exportSettingsToDownloads()
        }

        binding.btnImportSettings.setOnClickListener {
            importFilePicker.launch("application/json")
        }
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
