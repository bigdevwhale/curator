package app.curator

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.curator.data.Settings
import app.curator.ui.CuratorRoot
import app.curator.ui.theme.CuratorTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val ready by container.ready.collectAsStateWithLifecycle()
            // Wait for the settings (and the demo museum) so the welcome never flashes for someone already here.
            if (ready) settings?.let { s -> CuratorTheme(s.theme) { CuratorRoot(s) } }
        }
    }
}
