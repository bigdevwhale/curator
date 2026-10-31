package app.curator

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ApplicationProvider
import app.curator.data.Access
import app.curator.data.DemoSeed
import app.curator.data.Settings
import app.curator.data.Source
import app.curator.ui.CuratorRoot
import app.curator.ui.Overlay
import app.curator.ui.Tab
import app.curator.ui.onboarding.Onboarding
import app.curator.ui.theme.CuratorTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/** Every main screen over the demo museum, drawn to PNGs (run with `-Prender`), in both languages. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }
    private val app get() = ApplicationProvider.getApplicationContext<CuratorApp>()

    private fun seed(change: (Settings) -> Settings = { it }): Settings = runBlocking {
        val c = app.container
        c.demo = true
        c.access.value = Access.FULL
        DemoSeed.seedIfEmpty(app, c)
        c.settings.update { change(it.copy(onboarded = true)) }
        c.settings.now()
    }

    private fun shot(name: String, waitFor: String?, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { CuratorTheme { content() } }
        rule.waitUntil(20_000) { rule.mainClock.advanceTimeBy(100); waitFor == null || rule.onAllNodesWithText(waitFor, substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.mainClock.advanceTimeBy(1_500)
        rule.waitForIdle()
        Thread.sleep(600)
        rule.mainClock.advanceTimeBy(800)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun s(id: Int) = app.getString(id)

    private fun screen(name: String, tab: Tab, overlay: Overlay? = null, wait: String = s(R.string.tab_halls), change: (Settings) -> Settings = { it }) {
        val st = seed(change)
        shot(name, wait) { CuratorRoot(st, System.currentTimeMillis(), tab, overlay) }
    }

    private fun lastMonth() = LocalDate.now().minusMonths(1).let { it.year * 100 + it.monthValue }

    @Test fun welcome() = shot("0-welcome", null) { Onboarding() }

    /** The launcher icon as a PNG for the README and the site. */
    @Test fun logo() {
        val n = 512
        val image = android.graphics.Bitmap.createBitmap(n, n, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(image)
        val clip = android.graphics.Path().apply { addRoundRect(0f, 0f, n.toFloat(), n.toFloat(), n * .22f, n * .22f, android.graphics.Path.Direction.CW) }
        canvas.clipPath(clip)
        val k = n / 80f * 108f
        val off = (n - k) / 2
        for (id in listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground)) {
            val d = androidx.core.content.ContextCompat.getDrawable(app, id)!!
            d.setBounds(off.toInt(), off.toInt(), (off + k).toInt(), (off + k).toInt())
            d.draw(canvas)
        }
        File(out.parentFile, "logo.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun museum() = screen("1-museum", Tab.MUSEUM)
    @Test fun expert() = screen("2-expert", Tab.MUSEUM, Overlay.Crate(Source.Month(lastMonth())), s(R.string.expert_t)) {
        it.copy(flagShots = true, flagBlur = true, flagDark = true, flagDups = true)
    }
    @Test fun card() = screen("3-card", Tab.MUSEUM, Overlay.Crate(Source.Month(lastMonth())), s(R.string.a_keep)) {
        it.copy(flagShots = false, flagBlur = false, flagDark = false, flagDups = false)
    }
    @Test fun halls() = screen("4-halls", Tab.HALLS)
    @Test fun act() = screen("5-act", Tab.ACT)
    @Test fun hall() = screen("6-hall", Tab.HALLS, Overlay.Hall(1))
    @Test fun chronicle() = screen("7-chronicle", Tab.MUSEUM, Overlay.Chronicle)
    @Test fun settings() = screen("8-settings", Tab.MUSEUM, Overlay.Settings)
    @Test fun wing() {
        val st = seed()
        app.container.ceremony.value = Ceremony.Wing(3)
        shot("9-wing", s(R.string.ribbon)) { CuratorRoot(st, System.currentTimeMillis(), Tab.MUSEUM, null) }
    }
}

@Config(sdk = [34], qualifiers = "en-w411dp-h891dp-xxhdpi")
class RenderScreensEn : ScreensBase() { override val lang = "en" }

@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderScreensRu : ScreensBase() { override val lang = "ru" }

@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-night-xxhdpi")
class RenderScreensRuDark : ScreensBase() { override val lang = "ru-dark" }
