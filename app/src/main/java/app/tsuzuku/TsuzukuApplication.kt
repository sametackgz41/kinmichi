package app.tsuzuku

import android.app.Application
import app.tsuzuku.data.HabitRepository
import app.tsuzuku.data.LibraryRepository
import app.tsuzuku.data.GoalRepository
import app.tsuzuku.data.RoomGoalRepository
import app.tsuzuku.data.BackupRepository
import app.tsuzuku.data.CloudSyncRepository
import app.tsuzuku.data.DiagnosticStore
import app.tsuzuku.data.HealthConnectRepository
import app.tsuzuku.data.RoomHabitRepository
import app.tsuzuku.data.RoomLibraryRepository
import app.tsuzuku.data.ReviewRepository
import app.tsuzuku.data.RoomReviewRepository
import app.tsuzuku.data.PlanRepository
import app.tsuzuku.data.CategoryRepository
import app.tsuzuku.data.ConnectionRepository
import app.tsuzuku.data.FocusRepository
import app.tsuzuku.data.InboxRepository
import app.tsuzuku.data.ProjectRepository
import app.tsuzuku.data.RoomProjectRepository
import app.tsuzuku.data.intelligence.IntelligenceRepository
import app.tsuzuku.data.intelligence.RoomIntelligenceRepository
import app.tsuzuku.data.local.TsuzukuDatabase
import app.tsuzuku.data.preferences.SettingsRepository
import app.tsuzuku.data.preferences.TimerSessionStore
import app.tsuzuku.notifications.HabitNotifications
import app.tsuzuku.notifications.HabitReminderScheduler
import app.tsuzuku.widget.TsuzukuWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TsuzukuApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val habitRepository: HabitRepository by lazy {
        RoomHabitRepository(TsuzukuDatabase.getInstance(this).habitDao())
    }

    val libraryRepository: LibraryRepository by lazy {
        RoomLibraryRepository(TsuzukuDatabase.getInstance(this).libraryDao())
    }

    val reviewRepository: ReviewRepository by lazy {
        val db = TsuzukuDatabase.getInstance(this)
        RoomReviewRepository(
            reviewDao = db.reviewDao(),
            habitDao = db.habitDao(),
            habitRepository = habitRepository,
            planDao = db.planDao(),
            goalDao = db.goalDao(),
            focusDao = db.focusDao(),
            libraryDao = db.libraryDao(),
        )
    }

    val goalRepository: GoalRepository by lazy {
        RoomGoalRepository(TsuzukuDatabase.getInstance(this).goalDao())
    }

    val planRepository: PlanRepository by lazy {
        PlanRepository(TsuzukuDatabase.getInstance(this).planDao())
    }

    val categoryRepository: CategoryRepository by lazy {
        CategoryRepository(TsuzukuDatabase.getInstance(this).categoryDao())
    }

    val focusRepository: FocusRepository by lazy {
        FocusRepository(TsuzukuDatabase.getInstance(this).focusDao())
    }

    val inboxRepository: InboxRepository by lazy {
        InboxRepository(TsuzukuDatabase.getInstance(this).inboxDao())
    }

    val connectionRepository: ConnectionRepository by lazy {
        ConnectionRepository(TsuzukuDatabase.getInstance(this).connectionDao())
    }

    val projectRepository: ProjectRepository by lazy {
        val db = TsuzukuDatabase.getInstance(this)
        RoomProjectRepository(
            projectDao = db.projectDao(),
            planDao = db.planDao(),
            goalDao = db.goalDao(),
        )
    }

    val intelligenceRepository: IntelligenceRepository by lazy {
        val db = TsuzukuDatabase.getInstance(this)
        RoomIntelligenceRepository(
            habitDao = db.habitDao(),
            planDao = db.planDao(),
            goalDao = db.goalDao(),
            focusDao = db.focusDao(),
            reviewDao = db.reviewDao(),
        )
    }

    val backupRepository: BackupRepository by lazy {
        BackupRepository(TsuzukuDatabase.getInstance(this).backupDao(), this)
    }

    val diagnosticStore: DiagnosticStore by lazy { DiagnosticStore(this) }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(this)
    }

    val cloudSyncRepository: CloudSyncRepository by lazy {
        CloudSyncRepository(this, backupRepository, settingsRepository)
    }

    val healthConnectRepository: HealthConnectRepository by lazy {
        HealthConnectRepository(this, habitRepository)
    }

    val reminderScheduler: HabitReminderScheduler by lazy {
        HabitReminderScheduler(this, TsuzukuDatabase.getInstance(this).habitDao())
    }

    val timerSessionStore: TimerSessionStore by lazy {
        TimerSessionStore(this)
    }

    override fun onCreate() {
        super.onCreate()
        HabitNotifications.createChannel(this)
        diagnosticStore.installCrashCapture()
        applicationScope.launch {
            reminderScheduler.rescheduleAll()
            TsuzukuWidgetProvider.requestUpdate(this@TsuzukuApplication)
            cloudSyncRepository.synchronize()
            healthConnectRepository.refreshAndSync()
        }
    }

    fun requestCloudSync() {
        applicationScope.launch { cloudSyncRepository.synchronize() }
    }

    fun requestInitialCloudSetup() {
        applicationScope.launch { cloudSyncRepository.initializeFreshInstallation() }
    }

    fun requestHealthSync() {
        applicationScope.launch { healthConnectRepository.refreshAndSync() }
    }
}
