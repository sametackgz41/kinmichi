package app.tsuzuku.update

import android.content.Context
import android.net.Uri
import android.os.Build
import app.tsuzuku.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

const val GITHUB_REPO = "sametackgz41/tsuzuku"
const val GITHUB_URL = "https://github.com/$GITHUB_REPO"

/** A newer release on GitHub. */
data class AvailableUpdate(val version: String, val pageUrl: String)

/** "2.10.0" > "2.9.3"; a leading "v" and any "-suffix" are ignored. */
fun isNewerVersion(candidate: String, current: String): Boolean {
    fun parts(value: String) = value.trim().removePrefix("v").substringBefore('-')
        .split('.').map { it.toIntOrNull() ?: 0 }
    val a = parts(candidate)
    val b = parts(current)
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

/**
 * Asks GitHub, at most once a day, whether a newer release exists. One anonymous request to the
 * public releases API; nothing about the user is sent.
 */
object UpdateChecker {
    private const val PREFS = "tsuzuku_updates"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_LAST_CHECK = "last_check"
    private const val KEY_LATEST = "latest_version"
    private const val KEY_LATEST_URL = "latest_url"
    private const val KEY_DISMISSED = "dismissed_version"
    private const val DAY_MS = 24 * 60 * 60 * 1000L

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** Hides the card for [version]; a later release shows it again. */
    fun dismiss(context: Context, version: String) {
        prefs(context).edit().putString(KEY_DISMISSED, version).apply()
    }

    suspend fun check(context: Context, now: Long = System.currentTimeMillis()): AvailableUpdate? {
        if (!isEnabled(context)) return null
        val prefs = prefs(context)
        if (now - prefs.getLong(KEY_LAST_CHECK, 0L) >= DAY_MS) {
            fetchLatest()?.let { (version, url) ->
                prefs.edit().putString(KEY_LATEST, version).putString(KEY_LATEST_URL, url).putLong(KEY_LAST_CHECK, now).apply()
            }
        }
        val latest = prefs.getString(KEY_LATEST, null) ?: return null
        val url = prefs.getString(KEY_LATEST_URL, null) ?: "$GITHUB_URL/releases/latest"
        if (!isNewerVersion(latest, BuildConfig.VERSION_NAME)) return null
        if (prefs.getString(KEY_DISMISSED, null) == latest) return null
        return AvailableUpdate(latest.removePrefix("v"), url)
    }

    private suspend fun fetchLatest(): Pair<String, String>? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest").openConnection() as HttpURLConnection
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Kinmichi-Android")
            try {
                if (connection.responseCode != 200) return@runCatching null
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                json.getString("tag_name") to json.optString("html_url", "$GITHUB_URL/releases/latest")
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }
}

/** Shows "what's new" once after an update, never on a fresh install. */
object WhatsNew {
    private const val PREFS = "tsuzuku_updates"
    private const val KEY_SEEN = "whats_new_seen"

    /** True when this version's notes haven't been shown yet to someone who used an older one. */
    fun shouldShow(context: Context, existingUser: Boolean): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val seen = prefs.getString(KEY_SEEN, null)
        if (seen == BuildConfig.VERSION_NAME) return false
        if (!existingUser) {
            markSeen(context)
            return false
        }
        return true
    }

    fun markSeen(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_SEEN, BuildConfig.VERSION_NAME).apply()
    }
}

/**
 * A GitHub "new issue" link pre-filled with the bug template, the version and the diagnostic
 * report. GitHub limits URL length, so the log is trimmed to its most useful start.
 */
fun githubIssueUrl(report: String, title: String = ""): Uri {
    val trimmed = if (report.length > 5_000) report.take(5_000) + "\n…" else report
    return Uri.parse("$GITHUB_URL/issues/new").buildUpon()
        .appendQueryParameter("template", "bug_report.yml")
        .appendQueryParameter("title", title)
        .appendQueryParameter("version", BuildConfig.VERSION_NAME)
        .appendQueryParameter("device", "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}")
        .appendQueryParameter("log", trimmed)
        .build()
}
