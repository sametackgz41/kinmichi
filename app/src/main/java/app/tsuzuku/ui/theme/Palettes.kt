package app.tsuzuku.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import app.tsuzuku.data.preferences.PalettePreference

/**
 * The few hand-picked colours a palette is made of; every Material role is derived from them so
 * each palette stays coherent in both modes. Kintsugi gold (tertiary) is shared by all palettes.
 */
private data class PaletteSpec(
    val background: Long,
    val card: Long,
    val line: Long,
    val outline: Long,
    val text: Long,
    val muted: Long,
    val track: Long,
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val secondary: Long,
    val onSecondary: Long,
    val secondaryContainer: Long,
    val onSecondaryContainer: Long,
)

private fun PaletteSpec.toScheme(dark: Boolean): ColorScheme {
    val bg = Color(background)
    val ink = Color(text)
    fun toward(amount: Float) = lerp(bg, ink, amount)
    return if (dark) {
        darkColorScheme(
            primary = Color(primary), onPrimary = Color(onPrimary),
            primaryContainer = Color(primaryContainer), onPrimaryContainer = Color(onPrimaryContainer),
            secondary = Color(secondary), onSecondary = Color(onSecondary),
            secondaryContainer = Color(secondaryContainer), onSecondaryContainer = Color(onSecondaryContainer),
            tertiary = Color(0xFFE0C178), onTertiary = Color(0xFF3D2E05),
            tertiaryContainer = Color(0xFF5C4712), onTertiaryContainer = Color(0xFFF2E3BC),
            error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
            errorContainer = Color(0xFF8C1D18), onErrorContainer = Color(0xFFF9DEDC),
            background = bg, onBackground = ink, surface = bg, onSurface = ink,
            surfaceVariant = Color(track), onSurfaceVariant = Color(muted),
            outline = Color(outline), outlineVariant = Color(line),
            inverseSurface = ink, inverseOnSurface = toward(0.12f), inversePrimary = Color(onPrimary),
            surfaceContainerLowest = lerp(bg, Color.Black, 0.3f), surfaceContainerLow = Color(card),
            surfaceContainer = toward(0.07f), surfaceContainerHigh = toward(0.11f),
            surfaceContainerHighest = toward(0.15f), surfaceDim = bg, surfaceBright = toward(0.18f),
        )
    } else {
        lightColorScheme(
            primary = Color(primary), onPrimary = Color(onPrimary),
            primaryContainer = Color(primaryContainer), onPrimaryContainer = Color(onPrimaryContainer),
            secondary = Color(secondary), onSecondary = Color(onSecondary),
            secondaryContainer = Color(secondaryContainer), onSecondaryContainer = Color(onSecondaryContainer),
            tertiary = Color(0xFF8A6A1F), onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFF2E3BC), onTertiaryContainer = Color(0xFF4A380A),
            error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF8C1D18),
            background = bg, onBackground = ink, surface = bg, onSurface = ink,
            surfaceVariant = Color(track), onSurfaceVariant = Color(muted),
            outline = Color(outline), outlineVariant = Color(line),
            inverseSurface = toward(0.85f), inverseOnSurface = bg, inversePrimary = Color(primaryContainer),
            surfaceContainerLowest = lerp(bg, Color.White, 0.7f), surfaceContainerLow = Color(card),
            surfaceContainer = toward(0.025f), surfaceContainerHigh = toward(0.05f),
            surfaceContainerHighest = toward(0.08f), surfaceDim = toward(0.1f), surfaceBright = Color(card),
        )
    }
}

private val SumiLight = PaletteSpec(
    0xFFEFECE6, 0xFFF8F6F2, 0xFFD6D1C8, 0xFF8F887D, 0xFF141210, 0xFF59544D, 0xFFE2DDD4,
    0xFFB83A24, 0xFFFFFFFF, 0xFFF3DCD5, 0xFF5A1A0E,
    0xFF3D3934, 0xFFFFFFFF, 0xFFE3DED6, 0xFF1E1C1A,
)
private val SumiDark = PaletteSpec(
    0xFF12110F, 0xFF1C1A17, 0xFF2E2A25, 0xFF6E675D, 0xFFEDE6D8, 0xFFA79F92, 0xFF2A2622,
    0xFFE8603F, 0xFF12110F, 0xFF3A2620, 0xFFF6D3C8,
    0xFFCFC6B8, 0xFF26231F, 0xFF2E2A25, 0xFFEDE6D8,
)
private val MatchaLight = PaletteSpec(
    0xFFF1F3EA, 0xFFF9FAF4, 0xFFD5DBC6, 0xFF858E78, 0xFF1B2016, 0xFF535C4A, 0xFFE2E7D6,
    0xFF4E6B2E, 0xFFFFFFFF, 0xFFDDE7CC, 0xFF1F3209,
    0xFF6B5E3E, 0xFFFFFFFF, 0xFFEEE3C9, 0xFF2E260F,
)
private val MatchaDark = PaletteSpec(
    0xFF121710, 0xFF1B2118, 0xFF2F3829, 0xFF6B7560, 0xFFE3E8DA, 0xFFA9B39C, 0xFF263021,
    0xFFA7C77F, 0xFF1B2A0C, 0xFF33452A, 0xFFDDE7CC,
    0xFFD2C6A4, 0xFF362E14, 0xFF3E3620, 0xFFEEE3C9,
)
private val SakuraLight = PaletteSpec(
    0xFFFBF1F1, 0xFFFFF8F8, 0xFFEBD3D6, 0xFF977C81, 0xFF2A1A1D, 0xFF6B5357, 0xFFF2E1E3,
    0xFFA2445E, 0xFFFFFFFF, 0xFFF6DCE3, 0xFF4A1426,
    0xFF6E5B7A, 0xFFFFFFFF, 0xFFEBDDF4, 0xFF2A1A35,
)
private val SakuraDark = PaletteSpec(
    0xFF1C1316, 0xFF271B1F, 0xFF3D2C31, 0xFF806A6F, 0xFFF2E4E7, 0xFFC2A9AF, 0xFF33242A,
    0xFFF2A7BC, 0xFF4A1426, 0xFF4A2733, 0xFFF6DCE3,
    0xFFD6C2E3, 0xFF3B2A47, 0xFF3F3049, 0xFFEBDDF4,
)
private val UmiLight = PaletteSpec(
    0xFFEEF3F5, 0xFFF7FAFB, 0xFFCFDCE1, 0xFF798C96, 0xFF14202A, 0xFF4C5D68, 0xFFDEE7EB,
    0xFF1F6680, 0xFFFFFFFF, 0xFFD3E6EE, 0xFF0A3140,
    0xFF4D6A60, 0xFFFFFFFF, 0xFFD4E8DF, 0xFF0F2A22,
)
private val UmiDark = PaletteSpec(
    0xFF0E161C, 0xFF16212A, 0xFF263642, 0xFF61747F, 0xFFDCE8EE, 0xFF9DB0BC, 0xFF1F2D37,
    0xFF7CC6E0, 0xFF07313F, 0xFF1E4252, 0xFFD3E6EE,
    0xFFB3CEC3, 0xFF1F362D, 0xFF2B4239, 0xFFD4E8DF,
)

/**
 * Tsuzuku's own scheme for [palette] in the given mode. [PalettePreference.Dynamic] is resolved
 * by the caller (it needs a context); here it falls back to Washi.
 */
fun paletteScheme(palette: PalettePreference, dark: Boolean, pureBlack: Boolean = false): ColorScheme {
    val scheme = when (palette) {
        PalettePreference.Washi, PalettePreference.Dynamic -> if (dark) WashiDark else WashiLight
        PalettePreference.Sumi -> (if (dark) SumiDark else SumiLight).toScheme(dark)
        PalettePreference.Matcha -> (if (dark) MatchaDark else MatchaLight).toScheme(dark)
        PalettePreference.Sakura -> (if (dark) SakuraDark else SakuraLight).toScheme(dark)
        PalettePreference.Umi -> (if (dark) UmiDark else UmiLight).toScheme(dark)
    }
    return if (dark && pureBlack) scheme.withPureBlack() else scheme
}

/** AMOLED night: the page goes true black, cards keep a faint lift so they still read as cards. */
fun ColorScheme.withPureBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = lerp(Color.Black, surfaceContainerLow, 0.7f),
    surfaceContainer = lerp(Color.Black, surfaceContainer, 0.75f),
)
