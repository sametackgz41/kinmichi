package app.tsuzuku.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import app.tsuzuku.data.preferences.ThemeColorPreference

private val ColorWhite = Color(0xFFFFFFFF)

private data class AccentPalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
)

private fun lightAccent(color: ThemeColorPreference): AccentPalette = when (color) {
    ThemeColorPreference.Matcha -> AccentPalette(
        Color(0xFF3F6652), ColorWhite, Color(0xFFD5E8DA), Color(0xFF173728),
        Color(0xFF627953), ColorWhite, Color(0xFFE0E8D6), Color(0xFF26351F),
    )
    ThemeColorPreference.Sakura -> AccentPalette(
        Color(0xFF8A4F61), ColorWhite, Color(0xFFF6DCE4), Color(0xFF4B1F2D),
        Color(0xFFA26476), ColorWhite, Color(0xFFF0DEE3), Color(0xFF42252E),
    )
    ThemeColorPreference.Fuji -> AccentPalette(
        Color(0xFF39254B), ColorWhite, Color(0xFFE9DCEF), Color(0xFF271531),
        Color(0xFF695574), ColorWhite, Color(0xFFE9E0EC), Color(0xFF2B2430),
    )
    ThemeColorPreference.Amber -> AccentPalette(
        Color(0xFF755A18), ColorWhite, Color(0xFFF7E5B4), Color(0xFF3A2B00),
        Color(0xFF876B2B), ColorWhite, Color(0xFFECE3CA), Color(0xFF352E1D),
    )
    ThemeColorPreference.Ocean -> AccentPalette(
        Color(0xFF176A68), ColorWhite, Color(0xFFC8ECE9), Color(0xFF003735),
        Color(0xFF4A7774), ColorWhite, Color(0xFFD3E7E4), Color(0xFF173634),
    )
}

private fun darkAccent(color: ThemeColorPreference): AccentPalette = when (color) {
    ThemeColorPreference.Matcha -> AccentPalette(
        Color(0xFFA7D0B4), Color(0xFF123526), Color(0xFF284A38), Color(0xFFD0EDD7),
        Color(0xFFB5C99F), Color(0xFF24351C), Color(0xFF35452C), Color(0xFFD8E9C5),
    )
    ThemeColorPreference.Sakura -> AccentPalette(
        Color(0xFFEDB7C5), Color(0xFF522132), Color(0xFF6B3547), Color(0xFFFFD9E2),
        Color(0xFFD5BCC3), Color(0xFF3C292E), Color(0xFF514047), Color(0xFFF2D7DE),
    )
    ThemeColorPreference.Fuji -> AccentPalette(
        Color(0xFFD6BDE7), Color(0xFF39254B), Color(0xFF543A67), Color(0xFFF0D9FF),
        Color(0xFFD0C3D6), Color(0xFF382E3D), Color(0xFF4D414F), Color(0xFFEDE2F1),
    )
    ThemeColorPreference.Amber -> AccentPalette(
        Color(0xFFE5C36D), Color(0xFF3E2E00), Color(0xFF57440D), Color(0xFFFFE6A1),
        Color(0xFFD0C6AA), Color(0xFF352F20), Color(0xFF4C4737), Color(0xFFEDE2C5),
    )
    ThemeColorPreference.Ocean -> AccentPalette(
        Color(0xFF8BD5D0), Color(0xFF003735), Color(0xFF17504E), Color(0xFFACEDE9),
        Color(0xFFACC9C5), Color(0xFF173735), Color(0xFF334B49), Color(0xFFC7E5E1),
    )
}

private fun lightColors(color: ThemeColorPreference) = lightAccent(color).let { accent ->
    lightColorScheme(
        primary = accent.primary,
        onPrimary = accent.onPrimary,
        primaryContainer = accent.primaryContainer,
        onPrimaryContainer = accent.onPrimaryContainer,
        secondary = accent.secondary,
        onSecondary = accent.onSecondary,
        secondaryContainer = accent.secondaryContainer,
        onSecondaryContainer = accent.onSecondaryContainer,
        // Tertiary carries the editorial vermilion accent instead of M3's baseline rose.
        tertiary = Vermilion,
        onTertiary = ColorWhite,
        tertiaryContainer = Color(0xFFF8E1DB),
        onTertiaryContainer = Color(0xFF47130B),
        background = Color(0xFFF6F4EE),
        onBackground = Ink,
        surface = Color(0xFFFCFAF5),
        onSurface = Ink,
        surfaceVariant = Color(0xFFEAE7DF),
        onSurfaceVariant = MutedInk,
        // Container roles drive dialogs, sheets, menus, cards, pickers and text fields.
        // Left unset they fall back to M3's lavender baseline, so derive them from paper tones.
        surfaceContainerLowest = Color(0xFFFFFDF9),
        surfaceContainerLow = Color(0xFFF9F7F1),
        surfaceContainer = Color(0xFFF3F1EA),
        surfaceContainerHigh = Color(0xFFEEEBE4),
        surfaceContainerHighest = Color(0xFFE8E5DD),
        surfaceDim = Color(0xFFDEDBD3),
        surfaceBright = Color(0xFFFCFAF5),
        inverseSurface = Color(0xFF2C2E2A),
        inverseOnSurface = NightText,
        inversePrimary = darkAccent(color).primary,
        outline = Color(0xFF85837C),
        outlineVariant = Hairline,
        error = SoftError,
        onError = ColorWhite,
        errorContainer = Color(0xFFF9DEDA),
        onErrorContainer = Color(0xFF410E0B),
    )
}

private fun darkColors(color: ThemeColorPreference) = darkAccent(color).let { accent ->
    darkColorScheme(
        primary = accent.primary,
        onPrimary = accent.onPrimary,
        primaryContainer = accent.primaryContainer,
        onPrimaryContainer = accent.onPrimaryContainer,
        secondary = accent.secondary,
        onSecondary = accent.onSecondary,
        secondaryContainer = accent.secondaryContainer,
        onSecondaryContainer = accent.onSecondaryContainer,
        tertiary = NightVermilion,
        onTertiary = Color(0xFF4A140C),
        tertiaryContainer = Color(0xFF6B2A20),
        onTertiaryContainer = Color(0xFFFFDAD3),
        background = Color(0xFF101310),
        onBackground = NightText,
        surface = Color(0xFF171A17),
        onSurface = NightText,
        surfaceVariant = Color(0xFF232722),
        onSurfaceVariant = NightMutedText,
        surfaceContainerLowest = Color(0xFF0C0E0C),
        surfaceContainerLow = Color(0xFF151815),
        surfaceContainer = Color(0xFF1B1E1B),
        surfaceContainerHigh = Color(0xFF252924),
        surfaceContainerHighest = Color(0xFF30342F),
        surfaceDim = Color(0xFF101310),
        surfaceBright = Color(0xFF363A35),
        inverseSurface = Color(0xFFE4E2DB),
        inverseOnSurface = Color(0xFF2C2E2A),
        inversePrimary = lightAccent(color).primary,
        outline = Color(0xFF8D928B),
        outlineVariant = Color(0xFF373C36),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
    )
}

fun themePreviewColor(color: ThemeColorPreference, darkTheme: Boolean): Color =
    if (darkTheme) darkAccent(color).primary else lightAccent(color).primary

private val EditorialShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(18.dp),
)

@Composable
fun TsuzukuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColor: ThemeColorPreference = ThemeColorPreference.Matcha,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) darkColors(themeColor) else lightColors(themeColor)
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TsuzukuTypography,
        shapes = EditorialShapes,
        content = content,
    )
}
