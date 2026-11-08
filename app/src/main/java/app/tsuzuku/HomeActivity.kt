package app.tsuzuku

import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.fillMaxSize
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

class HomeActivity : FragmentActivity() {
    private var isUnlocked by mutableStateOf(true)
    private var biometricPromptActive = false
    private var backgroundedAt: Long = 0L

    /** Screen requested by a launcher shortcut, consumed once the main UI is showing. */
    private var launchRoute by mutableStateOf<String?>(null)

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
        if (savedInstanceState == null) launchRoute = routeFor(intent)
        val freshStart = savedInstanceState == null
        setContent {
            var showOpening by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(freshStart) }
            val settings by settingsRepository.settings.collectAsStateWithLifecycle()
            val introReplay by app.tsuzuku.ui.IntroReplay.stage.collectAsStateWithLifecycle()
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
            app.tsuzuku.ui.theme.LookPrefs.load(this@HomeActivity)
            app.tsuzuku.ui.screens.ProfilePrefs.load(this@HomeActivity)
            val baseDensity = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                    baseDensity.density, baseDensity.fontScale * app.tsuzuku.ui.theme.LookPrefs.textScale,
                ),
            ) {
            TsuzukuTheme(darkTheme = darkTheme, palette = settings.palette, pureBlack = settings.pureBlack, highContrast = app.tsuzuku.ui.theme.LookPrefs.highContrast) {
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize()) {
                if (settings.appLockEnabled && !isUnlocked) {
                    app.tsuzuku.ui.screens.AppLockScreen(
                        biometricEnabled = settings.biometricUnlockEnabled,
                        biometricAvailable = biometricAvailable(),
                        name = settings.profileName,
                        autoPrompt = !showOpening,
                        onVerifyPin = settingsRepository::verifyPin,
                        onUnlocked = { isUnlocked = true },
                        onBiometric = { showBiometricPrompt() },
                    )
                } else if (introReplay == app.tsuzuku.ui.IntroReplay.Stage.Tour) {
                    OnboardingScreen { app.tsuzuku.ui.IntroReplay.next() }
                } else if (introReplay == app.tsuzuku.ui.IntroReplay.Stage.Welcome) {
                    val cloud = application.cloudSyncRepository
                    val account by cloud.account.collectAsStateWithLifecycle()
                    val syncState by cloud.syncState.collectAsStateWithLifecycle()
                    app.tsuzuku.ui.screens.WelcomeScreen(
                        account = account,
                        syncState = syncState,
                        initialName = settings.profileName,
                        onSignIn = { activity, provider ->
                            if (provider == app.tsuzuku.ui.screens.SignInProvider.GitHub) cloud.signInWithGitHub(activity) else cloud.signIn(activity)
                        },
                        onFinish = { name, _ ->
                            if (name.isNotBlank()) settingsRepository.setProfileName(name)
                            cloud.clearStatus()
                            app.tsuzuku.ui.IntroReplay.next()
                        },
                    )
                } else if (!settings.onboardingCompleted) {
                    OnboardingScreen {
                        settingsRepository.completeOnboarding()
                    }
                } else if (!settings.initialGoogleSetupCompleted) {
                    val cloud = application.cloudSyncRepository
                    val account by cloud.account.collectAsStateWithLifecycle()
                    val syncState by cloud.syncState.collectAsStateWithLifecycle()
                    app.tsuzuku.ui.screens.WelcomeScreen(
                        account = account,
                        syncState = syncState,
                        initialName = settings.profileName,
                        onSignIn = { activity, provider ->
                            if (provider == app.tsuzuku.ui.screens.SignInProvider.GitHub) cloud.signInWithGitHub(activity) else cloud.signIn(activity)
                        },
                        onFinish = { name, signedIn ->
                            if (name.isNotBlank()) settingsRepository.setProfileName(name)
                            cloud.clearStatus()
                            settingsRepository.completeInitialGoogleSetup()
                            if (signedIn) application.requestInitialCloudSetup()
                        },
                    )
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
                        diagnosticStore = application.diagnosticStore,
                        reminderScheduler = application.reminderScheduler,
                        timerSessionStore = application.timerSessionStore,
                        settings = settings,
                        onSetProfileName = settingsRepository::setProfileName,
                        onSetTheme = settingsRepository::setTheme,
                        onSetPalette = { settingsRepository.setPalette(it); app.tsuzuku.widget.TsuzukuWidgets.requestUpdate(this@HomeActivity) },
                        onSetPureBlack = { settingsRepository.setPureBlack(it); app.tsuzuku.widget.TsuzukuWidgets.requestUpdate(this@HomeActivity) },
                        onSetWidgetOpacity = { settingsRepository.setWidgetOpacity(it); app.tsuzuku.widget.TsuzukuWidgets.requestUpdate(this@HomeActivity) },
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
                        connectionRepository = application.connectionRepository,
                        onSetTodaySectionOrder = settingsRepository::setTodaySectionOrder,
                        onSetTodayHiddenSections = settingsRepository::setTodayHiddenSections,
                        launchRoute = launchRoute,
                        onLaunchRouteHandled = { launchRoute = null },
                    )
                }
                // The opening, once per start of the app, over whatever comes first.
                if (showOpening) app.tsuzuku.ui.screens.OpeningScreen(onDone = { showOpening = false })
                }
            }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Reset first so the same screen asked for twice in a row still opens again.
        routeFor(intent)?.let { route ->
            launchRoute = null
            window.decorView.post { launchRoute = route }
        }
    }

    private fun routeFor(intent: Intent?): String? = when {
        // Reopened from recents: the old request was already handled back then.
        intent == null || intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0 -> null
        else -> routeForAction(intent)
    }

    private fun routeForAction(intent: Intent): String? = when (intent.action) {
        ACTION_ADD_HABIT -> {
            app.tsuzuku.ui.NewHabitTemplate.offer(intent)
            "habit/new"
        }
        ACTION_PROGRESS -> "progress"
        ACTION_MONTH_REVIEW -> "month-review"
        else -> null
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
        // Today's screen time for habits linked to it.
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            app.tsuzuku.health.ScreenTime.syncToday(this@HomeActivity, tsuzukuApplication.habitRepository)
            app.tsuzuku.widget.TsuzukuWidgets.requestUpdate(this@HomeActivity)
        }
        // Fresh Health Connect totals every time the app comes to the front.
        if (settings.healthSyncEnabled) {
            lifecycleScope.launch {
                app.tsuzuku.health.HealthSync.syncToday(this@HomeActivity, tsuzukuApplication.habitRepository)
                app.tsuzuku.widget.TsuzukuWidgets.requestUpdate(this@HomeActivity)
            }
        }
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

    companion object {
        const val ACTION_ADD_HABIT = "app.tsuzuku.action.ADD_HABIT"
        const val ACTION_PROGRESS = "app.tsuzuku.action.PROGRESS"
        const val ACTION_MONTH_REVIEW = "app.tsuzuku.action.MONTH_REVIEW"
    }
}
