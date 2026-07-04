package com.kalos.app

import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kalos.app.core.data.ThemePreferenceStore
import com.kalos.app.core.notification.NotificationHelper
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.core.ui.theme.ThemeMode
import com.kalos.app.core.ui.theme.colorSchemeFor
import com.kalos.app.navigation.KalosNavGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themePreferenceStore: ThemePreferenceStore

    // Destination requested by a notification tap; consumed once by the nav graph.
    private var pendingDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Match the window background to the resolved theme so there's no colour flash
        // between the (branded dark) splash and Compose's first frame.
        window.setBackgroundDrawable(ColorDrawable(resolveWindowBackground().toArgb()))
        pendingDestination = intent?.getStringExtra(NotificationHelper.EXTRA_DESTINATION)
        setContent {
            val themeMode by themePreferenceStore.mode.collectAsStateWithLifecycle()
            KalosTheme(mode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    KalosNavGraph(
                        deepLinkDestination = pendingDestination,
                        onDeepLinkHandled = { pendingDestination = null },
                    )
                }
            }
        }
    }

    /** Reads the stored theme synchronously (before Hilt injection) to pick the window background. */
    private fun resolveWindowBackground(): Color {
        val prefs = getSharedPreferences("kalos_theme", MODE_PRIVATE)
        val mode = runCatching {
            ThemeMode.valueOf(prefs.getString("mode", ThemeMode.SYSTEM.name)!!)
        }.getOrDefault(ThemeMode.SYSTEM)
        val systemDark =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return colorSchemeFor(mode, systemDark).background
    }

    // singleTop: a notification tap while the app is running arrives here instead of a new instance.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(NotificationHelper.EXTRA_DESTINATION)?.let { pendingDestination = it }
    }
}
