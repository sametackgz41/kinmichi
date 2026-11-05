package app.tsuzuku.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Sans = FontFamily.SansSerif

private fun style(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    letterSpacing: Double = 0.0,
) = TextStyle(
    fontFamily = Sans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

/**
 * Complete editorial type scale. Every one of the 15 Material roles is defined so that no
 * role silently falls back to the M3 baseline, and sizes strictly decrease within each group
 * (large > medium > small) so hierarchy stays readable.
 */
val TsuzukuTypography = Typography(
    displayLarge = style(48, 54, FontWeight.SemiBold, -1.0),
    displayMedium = style(38, 44, FontWeight.SemiBold, -0.8),
    displaySmall = style(30, 36, FontWeight.SemiBold, -0.6),

    headlineLarge = style(27, 33, FontWeight.SemiBold, -0.4),
    headlineMedium = style(23, 29, FontWeight.SemiBold, -0.25),
    headlineSmall = style(20, 26, FontWeight.SemiBold, -0.15),

    titleLarge = style(18, 23, FontWeight.SemiBold),
    titleMedium = style(15, 20, FontWeight.SemiBold),
    titleSmall = style(14, 19, FontWeight.SemiBold),

    bodyLarge = style(15, 22, FontWeight.Normal),
    bodyMedium = style(14, 20, FontWeight.Normal),
    bodySmall = style(12, 17, FontWeight.Normal, 0.1),

    labelLarge = style(14, 18, FontWeight.SemiBold, 0.1),
    labelMedium = style(12, 16, FontWeight.Medium, 0.2),
    labelSmall = style(11, 15, FontWeight.Medium, 0.3),
)
