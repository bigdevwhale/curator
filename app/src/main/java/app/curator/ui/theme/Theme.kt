package app.curator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.curator.R

/** An old museum: green striped wallpaper, inventory paper, brass plates, red ink for write-off and gold for exhibits. */
@Immutable
object C {
    val wall = Color(0xFF1D3A31)
    val wall2 = Color(0xFF173029)
    val wallD = Color(0xFF10211B)
    val paper = Color(0xFFEFE6D2)
    val paper2 = Color(0xFFE2D5B8)
    val paperLine = Color(0xFFC9B998)
    val ink = Color(0xFF2A231D)
    val muted = Color(0xFF7A6C5A)
    val onWall = Color(0xFFC9BB9A)
    val brass = Color(0xFFC49A5C)
    val brassL = Color(0xFFD3AD6C)
    val brassD = Color(0xFF8F6B38)
    val red = Color(0xFFA3342B)
    val green = Color(0xFF3F6B4F)
    val gold = Color(0xFFD4A73F)
    val lamp = Color(0xFFF3C76A)
    val crate = Color(0xFFC29A63)
    val crateD = Color(0xFF7A5A30)
    val scrim = Color(0xE60B1410)
    val photoBg = Color(0xFF15100C)
}

/** Cormorant Garamond by Christian Thalmann (SIL OFL): names, titles, the museum's voice. */
val Display = FontFamily(
    Font(R.font.cormorant_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_bold, FontWeight.Bold),
    Font(R.font.cormorant_italic, FontWeight.Normal, FontStyle.Italic),
)

/** PT Serif by ParaType (SIL OFL): the text of the labels and the acts. */
val Serif = FontFamily(
    Font(R.font.pt_serif_regular, FontWeight.Normal),
    Font(R.font.pt_serif_bold, FontWeight.Bold),
    Font(R.font.pt_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

/** PT Sans Narrow by ParaType (SIL OFL): stamps, small caps and the plates. */
val Narrow = FontFamily(Font(R.font.pt_sans_narrow_bold, FontWeight.Bold))

object T {
    val Hero = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 46.sp, color = C.paper)
    val Title = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp, color = C.ink)
    val Heading = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 25.sp, color = C.ink)
    val Body = TextStyle(fontFamily = Serif, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val Strong = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp, color = C.ink)
    val Small = TextStyle(fontFamily = Serif, fontSize = 13.sp, lineHeight = 18.sp, color = C.muted)
    val Italic = TextStyle(fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 19.sp, lineHeight = 23.sp, color = C.ink)
    val Caps = TextStyle(fontFamily = Narrow, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 15.sp, letterSpacing = 2.5.sp, color = C.brass)
    val Label = TextStyle(fontFamily = Narrow, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 17.sp, letterSpacing = .3.sp, color = C.muted)
    val Button = TextStyle(fontFamily = Narrow, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 19.sp, letterSpacing = 1.2.sp)
    val Stamp = TextStyle(fontFamily = Narrow, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = 4.sp)
    val Number = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 26.sp, color = C.ink)
}

@Composable
fun CuratorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.brass, onPrimary = C.ink, background = C.wall, surface = C.paper, onSurface = C.ink,
            onBackground = C.paper, outline = C.paperLine, error = C.red, secondary = C.green,
        ),
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
        content = content,
    )
}
