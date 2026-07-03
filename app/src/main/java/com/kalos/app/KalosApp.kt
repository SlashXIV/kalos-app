package com.kalos.app

import android.app.Application
import com.kalos.app.core.data.seed.DatabaseSeeder
import com.kalos.app.core.notification.IntelligentReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class KalosApp : Application() {

    @Inject lateinit var seeder: DatabaseSeeder
    @Inject lateinit var smartReminderScheduler: IntelligentReminderScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        scope.launch { seeder.seedIfEmpty() }
        // Safety net: re-anchor the self-chaining reminder if it was enabled but the
        // chain was broken (worker failure, edge cases). No-op when disabled.
        if (smartReminderScheduler.isEnabled()) smartReminderScheduler.schedule()
    }
}
