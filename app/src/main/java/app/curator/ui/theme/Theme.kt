package app.curator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.curator.R

/** A modern museum. Light: white gallery walls in daylight. Dark: the same halls at night, photos under spotlights. */
@Immutable
data class Palette(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val ink2: Color,
    val muted: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val red: Color,
    val redSoft: Color,
    val green: Color,
    val greenSoft: Color,
    val stone: Color,
    val stoneLine: Color,
    val lamp: Color,
    val photoBg: Color,
    val scrim: Color,
)

val Light = Palette(
    dark = false,
    bg = Color(0xFFF5F2EC), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFEDE8DF), line = Color(0xFFE2DCD1),
    ink = Color(0xFF1B1A17), ink2 = Color(0xFF45413A), muted = Color(0xFF8A847A),
    accent = Color(0xFFA9762A), accentSoft = Color(0xFFF3E7D0), onAccent = Color(0xFFFFFFFF),
    red = Color(0xFFC0412E), redSoft = Color(0xFFF7E1DC), green = Color(0xFF2E7A57), greenSoft = Color(0xFFDDEFE5),
    stone = Color(0xFFFFFFFF), stoneLine = Color(0xFF1B1A17), lamp = Color(0xFFF2C66D),
    photoBg = Color(0xFFE6E1D8), scrim = Color(0x99141210),
)

val Dark = Palette(
    dark = true,
    bg = Color(0xFF12110F), surface = Color(0xFF1D1B18), surface2 = Color(0xFF26231F), line = Color(0xFF332F29),
    ink = Color(0xFFF3EFE7), ink2 = Color(0xFFCFC9BE), muted = Color(0xFF8F897E),
    accent = Color(0xFFE0B25A), accentSoft = Color(0xFF3A2F1C), onAccent = Color(0xFF1B1609),
    red = Color(0xFFE76B55), redSoft = Color(0xFF3B211C), green = Color(0xFF5BBE8E), greenSoft = Color(0xFF1C3328),
    stone = Color(0xFF1D1B18), stoneLine = Color(0xFFE9E3D8), lamp = Color(0xFFF2C66D),
    photoBg = Color(0xFF0B0A09), scrim = Color(0xCC000000),
)

/** The palette in use. Read from anywhere, drawing code included; switching theme recomposes and redraws. */
object C {
    var p by mutableStateOf(Light)
    val dark get() = p.dark
    val bg get() = p.bg
    val surface get() = p.surface
    val surface2 get() = p.surface2
    val line get() = p.line
    val ink get() = p.ink
    val ink2 get() = p.ink2
    val muted get() = p.muted
    val accent get() = p.accent
    val accentSoft get() = p.accentSoft
    val onAccent get() = p.onAccent
    val red get() = p.red
    val redSoft get() = p.redSoft
    val green get() = p.green
    val greenSoft get() = p.greenSoft
    val stone get() = p.stone
    val stoneLine get() = p.stoneLine
    val lamp get() = p.lamp
    val photoBg get() = p.photoBg
    val scrim get() = p.scrim
}

/** Cormorant Garamond by Christian Thalmann (SIL OFL): the museum's voice in titles. */
val Display = FontFamily(
    Font(R.font.cormorant_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_bold, FontWeight.Bold),
    Font(R.font.cormorant_italic, FontWeight.Normal, FontStyle.Italic),
)

/** Inter by Rasmus Andersson (SIL OFL): everything that has to be read fast. */
val Sans = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

/** PT Sans Narrow by ParaType (SIL OFL): only the ink stamps. */
val Narrow = FontFamily(Font(R.font.pt_sans_narrow_bold, FontWeight.Bold))

object T {
    val Hero get() = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 44.sp, color = C.ink)
    val Title get() = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 34.sp, color = C.ink)
    val Heading get() = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 26.sp, color = C.ink)
    val Italic get() = TextStyle(fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 20.sp, lineHeight = 25.sp, color = C.ink)
    val Body get() = TextStyle(fontFamily = Sans, fontSize = 15.sp, lineHeight = 22.sp, color = C.ink)
    val Strong get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp, color = C.ink)
    val Small get() = TextStyle(fontFamily = Sans, fontSize = 13.sp, lineHeight = 18.sp, color = C.muted)
    val Label get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 17.sp, color = C.muted)
    val Caps get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.6.sp, color = C.muted)
    val Button get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 18.sp)
    val Number get() = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 32.sp, color = C.ink)
    val Stamp get() = TextStyle(fontFamily = Narrow, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = 4.sp)
}

/** 0: as the system, 1: light, 2: dark. */
@Composable
fun CuratorTheme(mode: Int = 0, content: @Composable () -> Unit) {
    val dark = when (mode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val p = if (dark) Dark else Light
    if (C.p != p) C.p = p
    val view = androidx.compose.ui.platform.LocalView.current
    SideEffect {
        if (C.p != p) C.p = p
        (view.context as? android.app.Activity)?.window?.let { w ->
            androidx.core.view.WindowCompat.getInsetsController(w, view).apply { isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark }
        }
    }
    MaterialTheme(
        colorScheme = if (dark) darkColorScheme(primary = p.accent, onPrimary = p.onAccent, background = p.bg, surface = p.surface, onSurface = p.ink, onBackground = p.ink, outline = p.line, error = p.red)
        else lightColorScheme(primary = p.accent, onPrimary = p.onAccent, background = p.bg, surface = p.surface, onSurface = p.ink, onBackground = p.ink, outline = p.line, error = p.red),
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
        content = content,
    )
}
