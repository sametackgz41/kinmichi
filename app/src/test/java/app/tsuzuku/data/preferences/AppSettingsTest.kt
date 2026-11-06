package app.tsuzuku.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    @Test
    fun invalidStoredValuesFallBackToSafeDefaults() {
        assertEquals(ThemePreference.System, ThemePreference.fromStored("unknown"))
        assertEquals(ThemeColorPreference.Matcha, ThemeColorPreference.fromStored("unknown"))
        assertEquals(LanguagePreference.System, LanguagePreference.fromStored(null))
        assertEquals(WeekStartPreference.Monday, WeekStartPreference.fromStored("Saturday"))
    }

    @Test
    fun storedEnumNamesRoundTrip() {
        assertEquals(ThemePreference.Dark, ThemePreference.fromStored(ThemePreference.Dark.name))
        assertEquals(ThemeColorPreference.Fuji, ThemeColorPreference.fromStored(ThemeColorPreference.Fuji.name))
        assertEquals(LanguagePreference.Turkish, LanguagePreference.fromStored(LanguagePreference.Turkish.name))
        assertEquals(WeekStartPreference.Sunday, WeekStartPreference.fromStored(WeekStartPreference.Sunday.name))
    }

    @Test
    fun languageTagsRepresentExplicitLocalesOnly() {
        // Saved values for languages that were dropped fall back to the system language.
        assertEquals(LanguagePreference.System, LanguagePreference.fromStored("Japanese"))
        assertNull(LanguagePreference.System.languageTag)
        assertEquals("tr", LanguagePreference.Turkish.languageTag)
        assertEquals("en", LanguagePreference.English.languageTag)
    }

    @Test
    fun defaultsMatchTheInitialProductExperience() {
        val settings = AppSettings()

        assertEquals(ThemePreference.System, settings.theme)
        assertEquals(ThemeColorPreference.Matcha, settings.lightThemeColor)
        assertEquals(ThemeColorPreference.Matcha, settings.darkThemeColor)
        assertEquals(WeekStartPreference.Monday, settings.weekStart)
        assertTrue(settings.showDailySummary)
        assertFalse(settings.protectScreen)
        assertFalse(settings.appLockEnabled)
        assertFalse(settings.biometricUnlockEnabled)
        assertEquals(0, settings.lockDelayMinutes)
        assertFalse(settings.onboardingCompleted)
        assertFalse(settings.initialGoogleSetupCompleted)
        assertTrue(settings.cloudSyncEnabled)
        assertEquals("", settings.lastCloudHash)
        assertEquals(0L, settings.lastCloudSyncAt)
        assertNull(settings.vacationStartEpochDay)
        assertNull(settings.vacationEndEpochDay)
    }
}
