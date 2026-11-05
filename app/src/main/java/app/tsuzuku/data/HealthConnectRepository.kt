package app.tsuzuku.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface HealthConnectState {
    data object Unavailable : HealthConnectState
    data object PermissionRequired : HealthConnectState
    data class Connected(val todaySteps: Long) : HealthConnectState
    data class Error(val message: String) : HealthConnectState
}

class HealthConnectRepository(
    private val context: Context,
    private val habitRepository: HabitRepository,
) {
    val permissions: Set<String> = setOf(HealthPermission.getReadPermission(StepsRecord::class))
    private val available = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    private val client by lazy { HealthConnectClient.getOrCreate(context) }
    private val mutableState = MutableStateFlow<HealthConnectState>(
        if (available) HealthConnectState.PermissionRequired else HealthConnectState.Unavailable,
    )
    val state: StateFlow<HealthConnectState> = mutableState.asStateFlow()

    suspend fun refreshAndSync() {
        if (!available) {
            mutableState.value = HealthConnectState.Unavailable
            return
        }
        runCatching {
            val granted = client.permissionController.getGrantedPermissions()
            if (!granted.containsAll(permissions)) {
                mutableState.value = HealthConnectState.PermissionRequired
                return
            }
            val zone = ZoneId.systemDefault()
            val start = LocalDate.now().atStartOfDay(zone).toInstant()
            val end = Instant.now()
            val result = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                ),
            )
            val steps = result[StepsRecord.COUNT_TOTAL] ?: 0L
            habitRepository.syncHealthSteps(steps)
            mutableState.value = HealthConnectState.Connected(steps)
        }.onFailure { mutableState.value = HealthConnectState.Error(it.localizedMessage ?: "Health Connect error") }
    }

    suspend fun disconnect() {
        if (!available) return
        runCatching { client.permissionController.revokeAllPermissions() }
        mutableState.value = HealthConnectState.PermissionRequired
    }
}
