package com.kalos.app

import android.app.Application
import com.kalos.app.core.data.seed.DatabaseSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class KalosApp : Application() {

    @Inject lateinit var seeder: DatabaseSeeder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        scope.launch { seeder.seedIfEmpty() }
    }
}
