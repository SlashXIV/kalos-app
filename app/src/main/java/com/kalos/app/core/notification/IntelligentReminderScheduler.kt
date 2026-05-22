package com.kalos.app.core.notification

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntelligentReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val prefs by lazy { context.getSharedPreferences("kalos_smart_reminders", Context.MODE_PRIVATE) }

    fun isEnabled(): Boolean = prefs.getBoolean("enabled", false)
    fun isNutritionEnabled(): Boolean = prefs.getBoolean("nutrition", true)
    fun isWorkoutEnabled(): Boolean = prefs.getBoolean("workout", true)
    fun isHydrationEnabled(): Boolean = prefs.getBoolean("hydration", false)
    fun getInactivityDays(): Int = prefs.getInt("inactivity_days", 3)
    fun getHour(): Int = prefs.getInt("hour", 20)

    fun setEnabled(v: Boolean) {
        prefs.edit().putBoolean("enabled", v).apply()
        if (v) schedule() else cancel()
    }

    fun setNutritionEnabled(v: Boolean) { prefs.edit().putBoolean("nutrition", v).apply() }
    fun setWorkoutEnabled(v: Boolean) { prefs.edit().putBoolean("workout", v).apply() }
    fun setHydrationEnabled(v: Boolean) { prefs.edit().putBoolean("hydration", v).apply() }
    fun setInactivityDays(days: Int) { prefs.edit().putInt("inactivity_days", days).apply() }

    fun setHour(hour: Int) {
        prefs.edit().putInt("hour", hour).apply()
        if (isEnabled()) schedule()
    }

    fun schedule() {
        NotificationHelper.createSmartChannel(context)
        val request = PeriodicWorkRequestBuilder<IntelligentReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(minutesUntilNextTarget(), TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(false).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private fun minutesUntilNextTarget(): Long {
        val now = LocalDateTime.now()
        val target = now.toLocalDate().atTime(getHour(), 0).let {
            if (now.isBefore(it)) it else it.plusDays(1)
        }
        return ChronoUnit.MINUTES.between(now, target).coerceAtLeast(1)
    }

    companion object {
        private const val WORK_NAME = "kalos_smart_reminder"
    }
}
