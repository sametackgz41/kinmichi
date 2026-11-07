package app.tsuzuku.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemePreference {
    System,
    Light,
    Dark;

    companion object {
        fun fromStored(value: String?): ThemePreference = entries.firstOrNull { it.name == value } ?: System
    }
}

enum class ThemeColorPreference {
    Matcha,
    Sakura,
    Fuji,
    Amber,
    Ocean;

    companion object {
        fun fromStored(value: String?): ThemeColorPreference =
            entries.firstOrNull { it.name == value } ?: Matcha
    }
}

/** Colour palette: Tsuzuku's own Washi look, or the wallpaper colours (Material You). */
enum class PalettePreference {
    Washi,
    Sumi,
    Matcha,
    Sakura,
    Umi,
    Dynamic;

    companion object {
        fun fromStored(value: String?): PalettePreference = entries.firstOrNull { it.name == value } ?: Washi
    }
}

enum class LanguagePreference(val languageTag: String?) {
    System(null),
    Turkish("tr"),
    English("en");

    companion object {
        fun fromStored(value: String?): LanguagePreference = entries.firstOrNull { it.name == value } ?: System
    }
}

enum class WeekStartPreference {
    Monday,
    Sunday;

    companion object {
        fun fromStored(value: String?): WeekStartPreference = entries.firstOrNull { it.name == value } ?: Monday
    }
}

data class AppSettings(
    val profileName: String = "",
    val theme: ThemePreference = ThemePreference.System,
    val lightThemeColor: ThemeColorPreference = ThemeColorPreference.Matcha,
    val darkThemeColor: ThemeColorPreference = ThemeColorPreference.Matcha,
    val palette: PalettePreference = PalettePreference.Washi,
    val pureBlack: Boolean = false,
    val language: LanguagePreference = LanguagePreference.System,
    val weekStart: WeekStartPreference = WeekStartPreference.Monday,
    val showDailySummary: Boolean = true,
    val protectScreen: Boolean = false,
    val appLockEnabled: Boolean = false,
    val biometricUnlockEnabled: Boolean = false,
    val lockDelayMinutes: Int = 0,
    val onboardingCompleted: Boolean = false,
    val initialGoogleSetupCompleted: Boolean = false,
    val cloudSyncEnabled: Boolean = true,
    val lastCloudHash: String = "",
    val lastCloudSyncAt: Long = 0L,
    val vacationStartEpochDay: Long? = null,
    val vacationEndEpochDay: Long? = null,
    val todaySectionOrder: String = "summary,habits,plan,goals,library,focus,health,routines,intelligence",
    val todayHiddenSections: String = DEFAULT_TODAY_HIDDEN_SECTIONS,
    val healthSyncEnabled: Boolean = true,
    val offlineIntelligenceEnabled: Boolean = true,
    val cloudAiEnabled: Boolean = false,
    val geminiApiKey: String = "",
    val aiShareHabitTitles: Boolean = true,
    val aiShareTaskTitles: Boolean = true,
    val aiShareNotes: Boolean = false,
    val cloudAiModel: String = "gemini-1.5-flash",
)

class SettingsRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    init {
        val existingInstallation = preferences.all.isNotEmpty()
        val editor = preferences.edit()
        if (!preferences.contains(KEY_ONBOARDING_COMPLETED)) {
            editor.putBoolean(KEY_ONBOARDING_COMPLETED, existingInstallation)
        }
        if (!preferences.contains(KEY_INITIAL_GOOGLE_SETUP_COMPLETED)) {
            editor.putBoolean(KEY_INITIAL_GOOGLE_SETUP_COMPLETED, existingInstallation)
        }
        editor.apply()
    }
    private val mutableSettings = MutableStateFlow(preferences.toSettings())

    val settings: StateFlow<AppSettings> = mutableSettings.asStateFlow()

    fun setProfileName(value: String) = update(KEY_PROFILE_NAME, value.trim().take(40))
    fun setTheme(value: ThemePreference) = update(KEY_THEME, value.name)
    fun setLightThemeColor(value: ThemeColorPreference) = update(KEY_LIGHT_THEME_COLOR, value.name)
    fun setDarkThemeColor(value: ThemeColorPreference) = update(KEY_DARK_THEME_COLOR, value.name)
    fun setPalette(value: PalettePreference) = update(KEY_PALETTE, value.name)
    fun setPureBlack(value: Boolean) = update(KEY_PURE_BLACK, value)
    fun setLanguage(value: LanguagePreference) = update(KEY_LANGUAGE, value.name)
    fun setWeekStart(value: WeekStartPreference) = update(KEY_WEEK_START, value.name)
    fun setShowDailySummary(value: Boolean) = update(KEY_DAILY_SUMMARY, value)
    fun setProtectScreen(value: Boolean) = update(KEY_PROTECT_SCREEN, value)
    fun completeOnboarding() = update(KEY_ONBOARDING_COMPLETED, true)
    fun completeInitialGoogleSetup() = update(KEY_INITIAL_GOOGLE_SETUP_COMPLETED, true)
    fun setCloudSyncEnabled(value: Boolean) = update(KEY_CLOUD_SYNC_ENABLED, value)
    fun setTodaySectionOrder(value: String) = update(KEY_TODAY_SECTION_ORDER, value)
    fun setTodayHiddenSections(value: String) = update(KEY_TODAY_HIDDEN_SECTIONS, value)
    fun setHealthSyncEnabled(value: Boolean) = update(KEY_HEALTH_SYNC_ENABLED, value)
    fun setOfflineIntelligenceEnabled(value: Boolean) = update(KEY_OFFLINE_INTELLIGENCE_ENABLED, value)
    fun setCloudAiEnabled(value: Boolean) = update(KEY_CLOUD_AI_ENABLED, value)
    fun setGeminiApiKey(value: String) = update(KEY_GEMINI_API_KEY, value.trim())
    fun setAiShareHabitTitles(value: Boolean) = update(KEY_AI_SHARE_HABIT_TITLES, value)
    fun setAiShareTaskTitles(value: Boolean) = update(KEY_AI_SHARE_TASK_TITLES, value)
    fun setAiShareNotes(value: Boolean) = update(KEY_AI_SHARE_NOTES, value)
    fun setCloudAiModel(value: String) = update(KEY_CLOUD_AI_MODEL, value)

    fun setVacation(startEpochDay: Long, endEpochDay: Long) {
        preferences.edit().putLong(KEY_VACATION_START, startEpochDay).putLong(KEY_VACATION_END, endEpochDay).apply()
        refresh()
    }

    fun clearVacation() {
        preferences.edit().remove(KEY_VACATION_START).remove(KEY_VACATION_END).apply()
        refresh()
    }

    fun recordCloudSync(hash: String, syncedAt: Long = System.currentTimeMillis()) {
        preferences.edit()
            .putString(KEY_LAST_CLOUD_HASH, hash)
            .putLong(KEY_LAST_CLOUD_SYNC_AT, syncedAt)
            .apply()
        refresh()
    }

    fun clearCloudSyncBaseline() {
        preferences.edit()
            .remove(KEY_LAST_CLOUD_HASH)
            .remove(KEY_LAST_CLOUD_SYNC_AT)
            .apply()
        refresh()
    }

    fun setPin(pin: String): Boolean {
        if (!PinSecurity.isValid(pin)) return false
        val salt = PinSecurity.newSalt()
        preferences.edit()
            .putString(KEY_PIN_SALT, salt)
            .putString(KEY_PIN_HASH, PinSecurity.hash(pin, salt))
            .putBoolean(KEY_BIOMETRIC_UNLOCK, false)
            .apply()
        refresh()
        return true
    }

    fun changePin(currentPin: String, newPin: String): Boolean {
        if (!verifyPin(currentPin) || !PinSecurity.isValid(newPin)) return false
        return setPin(newPin)
    }

    fun clearPin(currentPin: String): Boolean {
        if (!verifyPin(currentPin)) return false
        preferences.edit()
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_BIOMETRIC_UNLOCK, false)
            .apply()
        refresh()
        return true
    }

    fun verifyPin(pin: String): Boolean = PinSecurity.matches(
        pin = pin,
        salt = preferences.getString(KEY_PIN_SALT, "").orEmpty(),
        expectedHash = preferences.getString(KEY_PIN_HASH, "").orEmpty(),
    )

    fun setBiometricUnlock(value: Boolean) {
        update(KEY_BIOMETRIC_UNLOCK, value && preferences.getString(KEY_PIN_HASH, "").orEmpty().isNotBlank())
    }
    fun setLockDelayMinutes(value: Int) = update(KEY_LOCK_DELAY_MINUTES, value.coerceIn(0, 15).toString())

    private fun update(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
        mutableSettings.value = preferences.toSettings()
    }

    private fun update(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
        refresh()
    }

    private fun refresh() {
        mutableSettings.value = preferences.toSettings()
    }

    private fun SharedPreferences.toSettings() = AppSettings(
        profileName = getString(KEY_PROFILE_NAME, "").orEmpty(),
        theme = ThemePreference.fromStored(getString(KEY_THEME, null)),
        lightThemeColor = ThemeColorPreference.fromStored(getString(KEY_LIGHT_THEME_COLOR, null)),
        darkThemeColor = ThemeColorPreference.fromStored(getString(KEY_DARK_THEME_COLOR, null)),
        palette = PalettePreference.fromStored(getString(KEY_PALETTE, null)),
        pureBlack = getBoolean(KEY_PURE_BLACK, false),
        language = LanguagePreference.fromStored(getString(KEY_LANGUAGE, null)),
        weekStart = WeekStartPreference.fromStored(getString(KEY_WEEK_START, null)),
        showDailySummary = getBoolean(KEY_DAILY_SUMMARY, true),
        protectScreen = getBoolean(KEY_PROTECT_SCREEN, false),
        appLockEnabled = getString(KEY_PIN_HASH, "").orEmpty().isNotBlank(),
        biometricUnlockEnabled = getBoolean(KEY_BIOMETRIC_UNLOCK, false),
        lockDelayMinutes = getString(KEY_LOCK_DELAY_MINUTES, "0")?.toIntOrNull()?.coerceIn(0, 15) ?: 0,
        onboardingCompleted = getBoolean(KEY_ONBOARDING_COMPLETED, false),
        initialGoogleSetupCompleted = getBoolean(KEY_INITIAL_GOOGLE_SETUP_COMPLETED, false),
        cloudSyncEnabled = getBoolean(KEY_CLOUD_SYNC_ENABLED, true),
        lastCloudHash = getString(KEY_LAST_CLOUD_HASH, "").orEmpty(),
        lastCloudSyncAt = getLong(KEY_LAST_CLOUD_SYNC_AT, 0L),
        vacationStartEpochDay = getLong(KEY_VACATION_START, Long.MIN_VALUE).takeUnless { it == Long.MIN_VALUE },
        vacationEndEpochDay = getLong(KEY_VACATION_END, Long.MIN_VALUE).takeUnless { it == Long.MIN_VALUE },
        todaySectionOrder = getString(KEY_TODAY_SECTION_ORDER, "summary,habits,plan,goals,library,focus,health,routines,intelligence") ?: "summary,habits,plan,goals,library,focus,health,routines,intelligence",
        todayHiddenSections = getString(KEY_TODAY_HIDDEN_SECTIONS, DEFAULT_TODAY_HIDDEN_SECTIONS).orEmpty(),
        healthSyncEnabled = getBoolean(KEY_HEALTH_SYNC_ENABLED, true),
        offlineIntelligenceEnabled = getBoolean(KEY_OFFLINE_INTELLIGENCE_ENABLED, true),
        cloudAiEnabled = getBoolean(KEY_CLOUD_AI_ENABLED, false),
        geminiApiKey = getString(KEY_GEMINI_API_KEY, "").orEmpty(),
        aiShareHabitTitles = getBoolean(KEY_AI_SHARE_HABIT_TITLES, true),
        aiShareTaskTitles = getBoolean(KEY_AI_SHARE_TASK_TITLES, true),
        aiShareNotes = getBoolean(KEY_AI_SHARE_NOTES, false),
        cloudAiModel = getString(KEY_CLOUD_AI_MODEL, "gemini-1.5-flash") ?: "gemini-1.5-flash",
    )

    companion object {
        const val PREFERENCES_NAME = "tsuzuku_settings"
        const val KEY_LANGUAGE = "language"

        private const val KEY_PROFILE_NAME = "profile_name"
        private const val KEY_THEME = "theme"
        private const val KEY_PALETTE = "palette"
        private const val KEY_PURE_BLACK = "pure_black"
        private const val KEY_LIGHT_THEME_COLOR = "light_theme_color"
        private const val KEY_DARK_THEME_COLOR = "dark_theme_color"
        private const val KEY_WEEK_START = "week_start"
        private const val KEY_DAILY_SUMMARY = "daily_summary"
        private const val KEY_PROTECT_SCREEN = "protect_screen"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_BIOMETRIC_UNLOCK = "biometric_unlock"
        private const val KEY_LOCK_DELAY_MINUTES = "lock_delay_minutes"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_INITIAL_GOOGLE_SETUP_COMPLETED = "initial_google_setup_completed"
        private const val KEY_CLOUD_SYNC_ENABLED = "cloud_sync_enabled"
        private const val KEY_LAST_CLOUD_HASH = "last_cloud_hash"
        private const val KEY_LAST_CLOUD_SYNC_AT = "last_cloud_sync_at"
        private const val KEY_VACATION_START = "vacation_start"
        private const val KEY_VACATION_END = "vacation_end"
        private const val KEY_TODAY_SECTION_ORDER = "today_section_order"
        private const val KEY_TODAY_HIDDEN_SECTIONS = "today_hidden_sections"
        private const val KEY_HEALTH_SYNC_ENABLED = "health_sync_enabled"
        private const val KEY_OFFLINE_INTELLIGENCE_ENABLED = "offline_intelligence_enabled"
        private const val KEY_CLOUD_AI_ENABLED = "cloud_ai_enabled"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_AI_SHARE_HABIT_TITLES = "ai_share_habit_titles"
        private const val KEY_AI_SHARE_TASK_TITLES = "ai_share_task_titles"
        private const val KEY_AI_SHARE_NOTES = "ai_share_notes"
        private const val KEY_CLOUD_AI_MODEL = "cloud_ai_model"
    }
}

object AppLocaleContext {
    fun wrap(context: Context): Context {
        val stored = context.getSharedPreferences(SettingsRepository.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(SettingsRepository.KEY_LANGUAGE, null)
        val preference = LanguagePreference.fromStored(stored)
        val locale = preference.languageTag?.let(Locale::forLanguageTag)
            ?: Resources.getSystem().configuration.locales[0]

        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(configuration)
    }
}

/** Today starts focused on habits; other sections can be turned on from the customise sheet. */
const val DEFAULT_TODAY_HIDDEN_SECTIONS = "plan,goals,library,focus,health,routines,intelligence"
