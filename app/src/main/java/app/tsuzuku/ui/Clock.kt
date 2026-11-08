package app.tsuzuku.ui

import java.util.Locale

/**
 * A time of day as people write it in the app's language: 23.00 in Turkish, 23:00 elsewhere.
 * [minutes] is minutes since midnight.
 */
fun formatClock(minutes: Int, locale: Locale = Locale.getDefault()): String {
    val separator = if (locale.language == "tr") "." else ":"
    val m = ((minutes % 1440) + 1440) % 1440
    return "%02d%s%02d".format(m / 60, separator, m % 60)
}

/** Reads "23.00" or "23:00" back into minutes since midnight. */
fun parseClock(text: String): Int? {
    val parts = text.trim().split(':', '.')
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    return if (hour in 0..23 && minute in 0..59) hour * 60 + minute else null
}

/** The evening review ("How did today go?") opens from this hour. */
const val DAY_REVIEW_HOUR = 23
