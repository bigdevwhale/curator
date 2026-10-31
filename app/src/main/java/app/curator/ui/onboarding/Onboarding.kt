package app.curator.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.curator.R
import app.curator.container
import app.curator.data.Media
import app.curator.ui.art.Facade
import app.curator.ui.components.Btn
import app.curator.ui.components.Dot
import app.curator.ui.components.Kind
import app.curator.ui.components.Paper
import app.curator.ui.components.Stamp
import app.curator.ui.components.wallpaper
import app.curator.ui.theme.C
import app.curator.ui.theme.T

/** Asks for the gallery; afterwards the museum looks again and takes stock. */
@Composable
fun askAccess(after: () -> Unit = {}): () -> Unit {
    val c = LocalContext.current.container
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        c.write { sync() }
        after()
    }
    return { launcher.launch(Media.permissions()) }
}

@Composable
fun Onboarding() {
    val c = LocalContext.current.container
    var page by remember { mutableIntStateOf(0) }
    val ask = askAccess { c.write { found(); settings.update { it.copy(onboarded = true) } } }
    Column(Modifier.fillMaxSize().wallpaper().statusBarsPadding().navigationBarsPadding().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.museum_of).uppercase(), style = T.Caps, modifier = Modifier.padding(top = 12.dp))
        Text(stringResource(R.string.app_name), style = T.Hero)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (page) {
                0 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Facade(1, Modifier.fillMaxWidth())
                    Text(stringResource(R.string.ob_1t), style = T.Title.copy(color = C.paper), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 18.dp))
                    Text(stringResource(R.string.ob_1b), style = T.Body.copy(color = C.onWall), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
                }
                1 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Paper(Modifier.fillMaxWidth()) {
                        for ((g, t, col) in listOf(
                            Triple("↑", R.string.ob_up, C.red), Triple("→", R.string.ob_right, C.green), Triple("↓", R.string.ob_down, C.brassD),
                            Triple("←", R.string.ob_left, C.muted), Triple("✦", R.string.ob_hold, C.gold),
                        )) Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(g, style = T.Title.copy(color = col), modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                            Text(stringResource(t), style = T.Body)
                        }
                    }
                    Text(stringResource(R.string.ob_2t), style = T.Title.copy(color = C.paper), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 18.dp))
                    Text(stringResource(R.string.ob_2b), style = T.Body.copy(color = C.onWall), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
                }
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Stamp(stringResource(R.string.act_signed), C.red)
                    Text(stringResource(R.string.ob_3t), style = T.Title.copy(color = C.paper), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 26.dp))
                    Text(stringResource(R.string.ob_3b), style = T.Body.copy(color = C.onWall), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            for (i in 0..2) Dot(if (i == page) C.gold else C.onWall.copy(alpha = .4f))
        }
        if (page < 2) Btn(stringResource(R.string.next), { page++ }, Modifier.fillMaxWidth())
        else Btn(stringResource(R.string.ob_go), ask, Modifier.fillMaxWidth())
    }
}

/** The gallery is closed to the museum: ask again, or send the person to the app's settings. */
@Composable
fun AccessScreen() {
    val ctx = LocalContext.current
    val ask = askAccess()
    Column(Modifier.fillMaxSize().wallpaper().statusBarsPadding().navigationBarsPadding().padding(24.dp), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Facade(1, Modifier.fillMaxWidth())
        Text(stringResource(R.string.closed_t), style = T.Title.copy(color = C.paper), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 18.dp))
        Text(stringResource(R.string.closed_b), style = T.Body.copy(color = C.onWall), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp, bottom = 20.dp))
        Btn(stringResource(R.string.closed_ask), ask, Modifier.fillMaxWidth())
        Btn(stringResource(R.string.closed_settings), {
            runCatching { ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }, Modifier.fillMaxWidth().padding(top = 8.dp), Kind.WALL)
    }
}
