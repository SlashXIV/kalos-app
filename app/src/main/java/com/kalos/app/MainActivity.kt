package com.kalos.app

import android.content.Intent
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kalos.app.core.notification.NotificationHelper
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.navigation.KalosNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Destination requested by a notification tap; consumed once by the nav graph.
    private var pendingDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingDestination = intent?.getStringExtra(NotificationHelper.EXTRA_DESTINATION)
        setContent {
            KalosTheme {
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

    // singleTop: a notification tap while the app is running arrives here instead of a new instance.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(NotificationHelper.EXTRA_DESTINATION)?.let { pendingDestination = it }
    }
}
