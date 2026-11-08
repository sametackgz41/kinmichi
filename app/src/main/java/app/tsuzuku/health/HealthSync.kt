package app.tsuzuku.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import app.tsuzuku.data.HabitRepository
import app.tsuzuku.model.HealthMetric
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Converts a reading to the habit's own unit: kilometres vs metres, hours vs minutes. */
internal fun healthValueInUnit(metric: HealthMetric, raw: Double, unit: String): Double {
    val u = unit.trim().lowercase()
    return when (metric) {
        // raw = metres
        HealthMetric.Distance -> if (u == "m" || u.startsWith("metre") || u.startsWith("meter")) raw else raw / 1000.0
        // raw = minutes
        HealthMetric.ExerciseDuration, HealthMetric.SleepDuration ->
            if (u == "h" || u.startsWith("saat") || u.startsWith("hour") || u == "sa") raw / 60.0 else raw
        else -> raw
    }
}

/**
 * Reads today's totals from Health Connect for the metrics habits are linked to and writes
 * them as health progress. Read-only; does nothing when Health Connect is missing or a
 * permission was not granted.
 */
object HealthSync {
    private const val TAG = "HealthSync"

    fun isAvailable(context: Context): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    enum class Status { Ready, NeedsInstall, Unsupported }

    /** Ready to use, missing (Android 13 and below need the app), or not possible on this phone. */
    fun status(context: Context): Status = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> Status.Ready
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Status.NeedsInstall
        else -> if (android.os.Build.VERSION.SDK_INT >= 28) Status.NeedsInstall else Status.Unsupported
    }

    /** Opens what fits the status: the store page to install it, or Health Connect's own permission page. */
    fun open(context: Context) {
        val provider = "com.google.android.apps.healthdata"
        val intent = when (status(context)) {
            Status.Ready -> if (android.os.Build.VERSION.SDK_INT >= 34) {
                android.content.Intent("android.health.connect.action.MANAGE_HEALTH_PERMISSIONS")
                    .putExtra(android.content.Intent.EXTRA_PACKAGE_NAME, context.packageName)
            } else {
                android.content.Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
            }
            else -> android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("market://details?id=$provider&url=healthconnect%3A%2F%2Fonboarding"),
            ).setPackage("com.android.vending")
        }.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure {
            runCatching {
                context.startActivity(
                    android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=$provider"))
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    fun permissionFor(metric: HealthMetric): String? = when (metric) {
        HealthMetric.None -> null
        HealthMetric.Steps -> HealthPermission.getReadPermission(StepsRecord::class)
        HealthMetric.Distance -> HealthPermission.getReadPermission(DistanceRecord::class)
        HealthMetric.Calories -> HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
        HealthMetric.ExerciseDuration -> HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        HealthMetric.SleepDuration -> HealthPermission.getReadPermission(SleepSessionRecord::class)
    }

    /** Day-by-day values for the last [days] days, oldest first, in the habit's [unit]; null when not readable. */
    suspend fun history(context: Context, metric: HealthMetric, unit: String, days: Int = 30): List<Pair<LocalDate, Double>>? {
        if (metric == HealthMetric.None || !isAvailable(context)) return null
        return runCatching {
            val client = HealthConnectClient.getOrCreate(context)
            if (permissionFor(metric) !in client.permissionController.getGrantedPermissions()) return null
            val zone = ZoneId.systemDefault()
            val now = Instant.now()
            val today = LocalDate.now()
            (days - 1 downTo 0).map { back ->
                val day = today.minusDays(back.toLong())
                val start = day.atStartOfDay(zone).toInstant()
                val end = minOf(now, day.plusDays(1).atStartOfDay(zone).toInstant())
                day to healthValueInUnit(metric, read(client, metric, start, end, day, zone) ?: 0.0, unit)
            }
        }.onFailure { Log.w(TAG, "Health history failed", it) }.getOrNull()
    }

    /** Every read permission the app can ask for. */
    val allPermissions: Set<String> get() = HealthMetric.entries.mapNotNull(::permissionFor).toSet()

    /** What has been allowed so far; empty when Health Connect isn't there. */
    suspend fun granted(context: Context): Set<String> =
        if (!isAvailable(context)) emptySet()
        else runCatching { HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions() }.getOrDefault(emptySet())

    /** Whether the app may read this metric right now (installed and allowed). */
    suspend fun canRead(context: Context, metric: HealthMetric): Boolean =
        metric != HealthMetric.None && isAvailable(context) && runCatching {
            permissionFor(metric) in HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions()
        }.getOrDefault(false)

    suspend fun syncToday(context: Context, repository: HabitRepository) {
        if (!isAvailable(context)) return
        val metrics = repository.healthMetricsInUse()
        if (metrics.isEmpty()) return
        runCatching {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val dayStart = today.atStartOfDay(zone).toInstant()
            val now = Instant.now()
            metrics.filter { permissionFor(it) in granted }.forEach { metric ->
                val raw = read(client, metric, dayStart, now, today, zone) ?: return@forEach
                repository.syncHealth(metric) { unit -> healthValueInUnit(metric, raw, unit) }
            }
        }.onFailure { Log.w(TAG, "Health sync failed", it) }
    }

    private suspend fun read(
        client: HealthConnectClient,
        metric: HealthMetric,
        dayStart: Instant,
        now: Instant,
        today: LocalDate,
        zone: ZoneId,
    ): Double? {
        val todayRange = TimeRangeFilter.between(dayStart, now)
        return when (metric) {
            HealthMetric.None -> null
            HealthMetric.Steps ->
                client.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), todayRange))[StepsRecord.COUNT_TOTAL]?.toDouble()
            HealthMetric.Distance ->
                client.aggregate(AggregateRequest(setOf(DistanceRecord.DISTANCE_TOTAL), todayRange))[DistanceRecord.DISTANCE_TOTAL]?.inMeters
            HealthMetric.Calories ->
                client.aggregate(AggregateRequest(setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL), todayRange))[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
            HealthMetric.ExerciseDuration ->
                client.aggregate(AggregateRequest(setOf(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL), todayRange))[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]
                    ?.toMinutes()?.toDouble()
            HealthMetric.SleepDuration -> {
                // Last night: from 18:00 yesterday until noon today.
                val night = TimeRangeFilter.between(
                    today.minusDays(1).atTime(18, 0).atZone(zone).toInstant(),
                    minOf(now, today.atTime(12, 0).atZone(zone).toInstant()),
                )
                client.aggregate(AggregateRequest(setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL), night))[SleepSessionRecord.SLEEP_DURATION_TOTAL]
                    ?.toMinutes()?.toDouble()
            }
        }
    }
}
