package app.curator.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.curator.BuildConfig
import app.curator.R
import app.curator.container
import app.curator.data.Settings
import app.curator.ui.Home
import app.curator.ui.Nav
import app.curator.ui.components.Btn
import app.curator.ui.components.Kind
import app.curator.ui.components.Paper
import app.curator.ui.components.TopBar
import app.curator.ui.openUrl
import app.curator.ui.theme.C
import app.curator.ui.theme.T

/** The StillCraft bot: deep links file a bug or an idea for this app, or follow its releases. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "curator"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
}

@Composable
private fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    Text(text, style = T.Label.copy(color = if (on) C.paper else C.ink),
        modifier = Modifier.background(if (on) C.green else C.paper, RoundedCornerShape(16.dp)).border(1.dp, if (on) C.green else C.paperLine, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp))
}

@Composable
private fun Section(title: String, hint: String? = null) {
    Text(title.uppercase(), style = T.Caps.copy(color = C.brassD), modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
    if (hint != null) Text(hint, style = T.Small, modifier = Modifier.padding(bottom = 8.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnOff(on: Boolean, set: (Boolean) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Chip(stringResource(R.string.on), on) { set(true) }
        Chip(stringResource(R.string.off), !on) { set(false) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsPage(h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val s = h.s
    fun put(f: (Settings) -> Settings) = c.write { settings.update(f) }
    val notify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> c.remind(ok) }

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.settings), null, nav::close)
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp)) {
            Paper(Modifier.fillMaxWidth()) {
                Section(stringResource(R.string.set_crate), stringResource(R.string.set_crate_hint))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (n in listOf(12, 24, 48)) Chip(n.toString(), s.crate == n) { put { it.copy(crate = n) } }
                }

                Section(stringResource(R.string.set_expert), stringResource(R.string.set_expert_hint))
                for ((label, on, set) in listOf<Triple<Int, Boolean, (Boolean) -> Unit>>(
                    Triple(R.string.d_shot, s.flagShots) { v -> put { it.copy(flagShots = v) } },
                    Triple(R.string.d_blur, s.flagBlur) { v -> put { it.copy(flagBlur = v) } },
                    Triple(R.string.d_dark, s.flagDark) { v -> put { it.copy(flagDark = v) } },
                    Triple(R.string.d_dup, s.flagDups) { v -> put { it.copy(flagDups = v) } },
                )) Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(label), style = T.Body, modifier = Modifier.weight(1f))
                    OnOff(on, set)
                }

                Section(stringResource(R.string.set_remind), stringResource(R.string.set_remind_hint))
                OnOff(s.remind) { on ->
                    if (on && Build.VERSION.SDK_INT >= 33) notify.launch(Manifest.permission.POST_NOTIFICATIONS) else c.remind(on)
                }

                Section(stringResource(R.string.set_lang))
                val cur = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((tag, l) in listOf("" to R.string.lang_system, "ru" to R.string.lang_ru, "en" to R.string.lang_en))
                        Chip(stringResource(l), cur == tag) { AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)) }
                }

                Section(stringResource(R.string.privacy), stringResource(R.string.privacy_text))
            }

            Paper(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                Section(stringResource(R.string.community), stringResource(R.string.community_text))
                for ((label, sub, url) in listOf(
                    Triple(R.string.community_bug, R.string.community_bug_sub, Community.BUG),
                    Triple(R.string.community_idea, R.string.community_idea_sub, Community.IDEA),
                    Triple(R.string.community_follow, R.string.community_follow_sub, Community.FOLLOW),
                    Triple(R.string.community_chat, R.string.community_chat_sub, Community.CHAT),
                )) Row(Modifier.fillMaxWidth().clickable { openUrl(ctx, url) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(stringResource(label), style = T.Strong); Text(stringResource(sub), style = T.Small) }
                    Text("›", style = T.Title.copy(color = C.brassD))
                }

                Section(stringResource(R.string.about))
                Text(stringResource(R.string.about_text, BuildConfig.VERSION_NAME), style = T.Small)
                Btn(stringResource(R.string.source), { openUrl(ctx, "https://github.com/bigdevwhale/curator") }, Modifier.padding(top = 10.dp), Kind.GHOST)
            }
        }
    }
}
