package app.tsuzuku.data

import android.content.Context
import android.os.Build
import app.tsuzuku.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

class DiagnosticStore(private val context: Context) {
    private val crashFile = File(context.filesDir, "last_crash.txt")

    fun installCrashCapture() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val trace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
                crashFile.writeText("${Instant.now()}\nThread: ${thread.name}\n$trace")
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun hasCrashReport(): Boolean = crashFile.exists() && crashFile.length() > 0
    fun lastCrash(): String? = crashFile.takeIf { it.exists() }?.readText()?.takeIf(String::isNotBlank)
    fun clearCrash() = runCatching { crashFile.delete() }.isSuccess

    fun report(): String = buildString {
        appendLine("Tsuzuku diagnostic report")
        appendLine("Generated: ${Instant.now()}")
        appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Android: ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Locale: ${context.resources.configuration.locales[0]}")
        appendLine()
        appendLine("Last captured crash:")
        appendLine(lastCrash() ?: "None")
    }
}
