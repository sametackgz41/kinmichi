package app.tsuzuku.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import app.tsuzuku.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Fallback for devices without Material You (Android 11 and lower): a calm, neutral blue
 * scheme generated from a single seed so every M3 role is defined.
 */
internal val FallbackLight = lightColorScheme(
    primary = Color(0xFF405F90), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E3FF), onPrimaryContainer = Color(0xFF274777),
    secondary = Color(0xFF565F71), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDAE2F9), onSecondaryContainer = Color(0xFF3E4759),
    tertiary = Color(0xFF705575), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFAD8FD), onTertiaryContainer = Color(0xFF573E5C),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF9F9FF), onBackground = Color(0xFF191C20),
    surface = Color(0xFFF9F9FF), onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE0E2EC), onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF74777F), outlineVariant = Color(0xFFC4C6D0),
    inverseSurface = Color(0xFF2E3036), inverseOnSurface = Color(0xFFF0F0F7),
    inversePrimary = Color(0xFFAAC7FF),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF3F3FA),
    surfaceContainer = Color(0xFFEDEDF4), surfaceContainerHigh = Color(0xFFE7E8EE),
    surfaceContainerHighest = Color(0xFFE2E2E9),
    surfaceDim = Color(0xFFD9D9E0), surfaceBright = Color(0xFFF9F9FF),
)

internal val FallbackDark = darkColorScheme(
    primary = Color(0xFFAAC7FF), onPrimary = Color(0xFF0A305F),
    primaryContainer = Color(0xFF284777), onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFBEC6DC), onSecondary = Color(0xFF283141),
    secondaryContainer = Color(0xFF3E4759), onSecondaryContainer = Color(0xFFDAE2F9),
    tertiary = Color(0xFFDDBCE0), onTertiary = Color(0xFF3F2844),
    tertiaryContainer = Color(0xFF573E5C), onTertiaryContainer = Color(0xFFFAD8FD),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111318), onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318), onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF44474E), onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099), outlineVariant = Color(0xFF44474E),
    inverseSurface = Color(0xFFE2E2E9), inverseOnSurface = Color(0xFF2E3036),
    inversePrimary = Color(0xFF405F90),
    surfaceContainerLowest = Color(0xFF0C0E13), surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024), surfaceContainerHigh = Color(0xFF282A2F),
    surfaceContainerHighest = Color(0xFF33353A),
    surfaceDim = Color(0xFF111318), surfaceBright = Color(0xFF37393E),
)

/**
 * Washi: Tsuzuku's own look. Warm paper, ink text, indigo (aizome) as the main colour,
 * matcha green as the second and kintsugi gold for comebacks.
 */
internal val WashiLight = lightColorScheme(
    primary = Color(0xFF2E4A7D), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E1EF), onPrimaryContainer = Color(0xFF1B2F52),
    secondary = Color(0xFF56693F), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E7D2), onSecondaryContainer = Color(0xFF2B3820),
    tertiary = Color(0xFF8A6A1F), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF2E3BC), onTertiaryContainer = Color(0xFF4A380A),
    error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF8C1D18),
    background = Color(0xFFF5F0E6), onBackground = Color(0xFF1F1D1A),
    surface = Color(0xFFF5F0E6), onSurface = Color(0xFF1F1D1A),
    surfaceVariant = Color(0xFFE9E1D2), onSurfaceVariant = Color(0xFF5E584E),
    outline = Color(0xFF8C8476), outlineVariant = Color(0xFFDCD3C3),
    inverseSurface = Color(0xFF33302B), inverseOnSurface = Color(0xFFF5F0E6),
    inversePrimary = Color(0xFFA9BDE6),
    surfaceContainerLowest = Color(0xFFFFFDF9), surfaceContainerLow = Color(0xFFFBF8F2),
    surfaceContainer = Color(0xFFF3EDE2), surfaceContainerHigh = Color(0xFFEDE6D9),
    surfaceContainerHighest = Color(0xFFE7DFD0),
    surfaceDim = Color(0xFFE2D9C9), surfaceBright = Color(0xFFFBF8F2),
)

/** Washi at night: indigo ink instead of paper, the same accents lifted for contrast. */
internal val WashiDark = darkColorScheme(
    primary = Color(0xFFA9BDE6), onPrimary = Color(0xFF10274D),
    primaryContainer = Color(0xFF2B4170), onPrimaryContainer = Color(0xFFD9E1EF),
    secondary = Color(0xFFB6C79C), onSecondary = Color(0xFF22301A),
    secondaryContainer = Color(0xFF3A4A2C), onSecondaryContainer = Color(0xFFDDE5CF),
    tertiary = Color(0xFFE0C178), onTertiary = Color(0xFF3D2E05),
    tertiaryContainer = Color(0xFF5C4712), onTertiaryContainer = Color(0xFFF2E3BC),
    error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18), onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF15171C), onBackground = Color(0xFFECE6DA),
    surface = Color(0xFF15171C), onSurface = Color(0xFFECE6DA),
    surfaceVariant = Color(0xFF2C2F36), onSurfaceVariant = Color(0xFFBDB6AA),
    outline = Color(0xFF878176), outlineVariant = Color(0xFF3A3D44),
    inverseSurface = Color(0xFFECE6DA), inverseOnSurface = Color(0xFF2A2C31),
    inversePrimary = Color(0xFF2E4A7D),
    surfaceContainerLowest = Color(0xFF101216), surfaceContainerLow = Color(0xFF1A1C21),
    surfaceContainer = Color(0xFF1F2127), surfaceContainerHigh = Color(0xFF282A31),
    surfaceContainerHighest = Color(0xFF32353B),
    surfaceDim = Color(0xFF15171C), surfaceBright = Color(0xFF3A3D44),
)

@Composable
fun TsuzukuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: app.tsuzuku.data.preferences.PalettePreference = app.tsuzuku.data.preferences.PalettePreference.Washi,
    pureBlack: Boolean = false,
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        palette == app.tsuzuku.data.preferences.PalettePreference.Dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) {
                dynamicDarkColorScheme(context).let { if (pureBlack) it.withPureBlack() else it }
            } else {
                dynamicLightColorScheme(context)
            }
        else -> paletteScheme(palette, darkTheme, pureBlack)
    }.let { if (highContrast) it.withHighContrast() else it }
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

    // Material 3 Expressive: springy motion, rounder shapes and bolder headings.
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = ExpressiveTypography,
        shapes = ExpressiveShapes,
        content = content,
    )
}

internal val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/** Newsreader, an open-licensed editorial serif, for headings and big numbers. */
@OptIn(ExperimentalTextApi::class)
val SerifDisplay: FontFamily = FontFamily(
    listOf(500, 600, 700).flatMap { weight ->
        listOf(
            Font(
                R.font.newsreader, FontWeight(weight), FontStyle.Normal,
                variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.Setting("opsz", 36f)),
            ),
            Font(
                R.font.newsreader_italic, FontWeight(weight), FontStyle.Italic,
                variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.Setting("opsz", 16f)),
            ),
        )
    } + Font(
        R.font.newsreader_italic, FontWeight.Normal, FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(400), FontVariation.Setting("opsz", 16f)),
    ),
)

/** Serif headings over the standard sans body text, which stays as is for readability. */
internal val ExpressiveTypography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.copy(fontFamily = SerifDisplay, fontWeight = FontWeight.SemiBold),
        displayMedium = t.displayMedium.copy(fontFamily = SerifDisplay, fontWeight = FontWeight.SemiBold),
        displaySmall = t.displaySmall.copy(fontFamily = SerifDisplay, fontWeight = FontWeight.SemiBold),
        headlineLarge = t.headlineLarge.copy(fontFamily = SerifDisplay, fontWeight = FontWeight.SemiBold),
        headlineMedium = t.headlineMedium.copy(fontFamily = SerifDisplay, fontWeight = FontWeight.SemiBold),
        headlineSmall = t.headlineSmall.copy(fontFamily = SerifDisplay, fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontFamily = SerifDisplay, fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}
