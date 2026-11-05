package app.tsuzuku

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsuzuku.data.preferences.AppLocaleContext
import app.tsuzuku.data.preferences.ThemePreference
import app.tsuzuku.ui.TsuzukuApp
import app.tsuzuku.ui.screens.AppLockScreen
import app.tsuzuku.ui.screens.CloudAccountScreen
import app.tsuzuku.ui.screens.OnboardingScreen
import app.tsuzuku.ui.theme.TsuzukuTheme

class MainActivity : FragmentActivity() {
    private var isUnlocked by mutableStateOf(true)
    private var biometricPromptActive = false
    private var backgroundedAt: Long = 0L

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocaleContext.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemNavigation()
        val application = application as TsuzukuApplication
        val repository = application.habitRepository
        val libraryRepository = application.libraryRepository
        val goalRepository = application.goalRepository
        val settingsRepository = application.settingsRepository
        isUnlocked = !settingsRepository.settings.value.appLockEnabled
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle()
            val darkTheme = when (settings.theme) {
                ThemePreference.System -> isSystemInDarkTheme()
                ThemePreference.Light -> false
                ThemePreference.Dark -> true
            }
            SideEffect {
                if (settings.protectScreen) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
            TsuzukuTheme(darkTheme = darkTheme) {
                if (settings.appLockEnabled && !isUnlocked) {
                    AppLockScreen(
                        biometricEnabled = settings.biometricUnlockEnabled,
                        biometricAvailable = biometricAvailable(),
                        onVerifyPin = settingsRepository::verifyPin,
                        onUnlocked = { isUnlocked = true },
                        onBiometric = { showBiometricPrompt() },
                    )
                } else if (!settings.initialGoogleSetupCompleted) {
                    CloudAccountScreen(
                        repository = application.cloudSyncRepository,
                        settings = settings,
                        onBack = {},
                        onSetCloudSyncEnabled = settingsRepository::setCloudSyncEnabled,
                        onDataRestored = {
                            application.reminderScheduler.rescheduleAll()
                        },
                        initialSetup = true,
                        onInitialSetupComplete = { googleName ->
                            settingsRepository.completeInitialGoogleSetup()
                            if (googleName != null) {
                                if (googleName.isNotBlank()) settingsRepository.setProfileName(googleName)
                                settingsRepository.completeOnboarding()
                                application.requestInitialCloudSetup()
                            }
                        },
                    )
                } else if (!settings.onboardingCompleted) {
                    OnboardingScreen {
                        settingsRepository.completeOnboarding()
                    }
                } else {
                    TsuzukuApp(
                        repository = repository,
                        libraryRepository = libraryRepository,
                        reviewRepository = application.reviewRepository,
                        goalRepository = goalRepository,
                        planRepository = application.planRepository,
                        categoryRepository = application.categoryRepository,
                        backupRepository = application.backupRepository,
                        cloudSyncRepository = application.cloudSyncRepository,
                        healthConnectRepository = application.healthConnectRepository,
                        diagnosticStore = application.diagnosticStore,
                        reminderScheduler = application.reminderScheduler,
                        timerSessionStore = application.timerSessionStore,
                        settings = settings,
                        onSetProfileName = settingsRepository::setProfileName,
                        onSetTheme = settingsRepository::setTheme,
                        onSetLanguage = { language ->
                            settingsRepository.setLanguage(language)
                            recreate()
                        },
                        onSetWeekStart = settingsRepository::setWeekStart,
                        onSetShowDailySummary = settingsRepository::setShowDailySummary,
                        onSetProtectScreen = settingsRepository::setProtectScreen,
                        onSetCloudSyncEnabled = settingsRepository::setCloudSyncEnabled,
                        onSetVacation = settingsRepository::setVacation,
                        onClearVacation = settingsRepository::clearVacation,
                        biometricAvailable = biometricAvailable(),
                        onSetPin = settingsRepository::setPin,
                        onChangePin = settingsRepository::changePin,
                        onDisablePin = { pin -> settingsRepository.clearPin(pin).also { if (it) isUnlocked = true } },
                        onSetBiometric = settingsRepository::setBiometricUnlock,
                        onSetLockDelayMinutes = settingsRepository::setLockDelayMinutes,
                        focusRepository = application.focusRepository,
                        inboxRepository = application.inboxRepository,
                        connectionRepository = application.connectionRepository,
                        projectRepository = application.projectRepository,
                        intelligenceRepository = application.intelligenceRepository,
                        onSetTodaySectionOrder = settingsRepository::setTodaySectionOrder,
                        onSetTodayHiddenSections = settingsRepository::setTodayHiddenSections,
                        onSetHealthSyncEnabled = settingsRepository::setHealthSyncEnabled,
                        onSetOfflineIntelligenceEnabled = settingsRepository::setOfflineIntelligenceEnabled,
                        onSetCloudAiEnabled = settingsRepository::setCloudAiEnabled,
                        onSetGeminiApiKey = settingsRepository::setGeminiApiKey,
                        onSetAiShareHabitTitles = settingsRepository::setAiShareHabitTitles,
                        onSetAiShareTaskTitles = settingsRepository::setAiShareTaskTitles,
                        onSetAiShareNotes = settingsRepository::setAiShareNotes,
                        onSetCloudAiModel = settingsRepository::setCloudAiModel,
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val tsuzukuApplication = application as TsuzukuApplication
        val settings = tsuzukuApplication.settingsRepository.settings.value
        if (!isChangingConfigurations && settings.cloudSyncEnabled) {
            tsuzukuApplication.requestCloudSync()
        }
        if (!isChangingConfigurations && !biometricPromptActive && settings.appLockEnabled) {
            backgroundedAt = System.currentTimeMillis()
            if (settings.lockDelayMinutes == 0) isUnlocked = false
        }
    }

    override fun onResume() {
        super.onResume()
        val tsuzukuApplication = application as TsuzukuApplication
        val settings = tsuzukuApplication.settingsRepository.settings.value
        if (settings.appLockEnabled && backgroundedAt > 0L && settings.lockDelayMinutes > 0 &&
            System.currentTimeMillis() - backgroundedAt >= settings.lockDelayMinutes * 60_000L
        ) isUnlocked = false
        backgroundedAt = 0L
        tsuzukuApplication.requestHealthSync()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemNavigation()
    }

    private fun hideSystemNavigation() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun biometricAvailable(): Boolean {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        return BiometricManager.from(this).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricPrompt() {
        if (!biometricAvailable() || biometricPromptActive) return
        biometricPromptActive = true
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    biometricPromptActive = false
                    isUnlocked = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    biometricPromptActive = false
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt_title))
            .setSubtitle(getString(R.string.biometric_prompt_subtitle))
            .setAllowedAuthenticators(authenticators)
            .setNegativeButtonText(getString(R.string.use_pin_instead))
            .build()
        prompt.authenticate(info)
    }
}
