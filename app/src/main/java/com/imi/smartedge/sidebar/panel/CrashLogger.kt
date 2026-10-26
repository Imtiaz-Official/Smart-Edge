package com.imi.smartedge.sidebar.panel

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {
    private const val LOG_FILE_NAME = "crash_log.txt"

    fun install(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                recordCrash(context, thread, throwable)
            } catch (e: Exception) {
                Log.e("CrashLogger", "Failed to record crash", e)
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun recordCrash(context: Context, thread: Thread, throwable: Throwable) {
        val file = File(context.filesDir, LOG_FILE_NAME)
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()

        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val logEntry = """
            ==================== CRASH REPORT ====================
            Time: $timeStr
            Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})
            Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
            Thread: ${thread.name} (id: ${thread.id})
            Exception: ${throwable.javaClass.name}: ${throwable.message}
            Stack Trace:
            $stackTrace
            =======================================================

        """.trimIndent()

        file.appendText(logEntry)
    }

    fun getCrashLog(context: Context): String? {
        val file = File(context.filesDir, LOG_FILE_NAME)
        return if (file.exists() && file.length() > 0) file.readText() else null
    }

    fun clearCrashLog(context: Context) {
        val file = File(context.filesDir, LOG_FILE_NAME)
        if (file.exists()) file.delete()
    }
}
