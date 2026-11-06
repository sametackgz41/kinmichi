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
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun TsuzukuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> FallbackDark
        else -> FallbackLight
    }
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

/** The standard type scale with emphasised headings; body text stays as is for readability. */
internal val ExpressiveTypography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.copy(fontWeight = FontWeight.SemiBold),
        displayMedium = t.displayMedium.copy(fontWeight = FontWeight.SemiBold),
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.SemiBold),
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}
