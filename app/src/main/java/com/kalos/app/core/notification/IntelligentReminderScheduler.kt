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

    /**
     * Schedules the next reminder as a one-time job at the configured hour. The worker
     * re-enqueues the following day at the end of its run, forming a self-perpetuating
     * chain anchored to the wall-clock hour — unlike a PeriodicWorkRequest, which drifts
     * after the first run (Doze/batching) and ignored the chosen time in practice.
     */
    fun schedule() {
        NotificationHelper.createSmartChannel(context)
        enqueueNext(context, getHour())
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        private const val WORK_NAME = "kalos_smart_reminder"

        /** Enqueues a single run at the next occurrence of [hour]. Called by the scheduler and by the worker to chain the next day. */
        fun enqueueNext(context: Context, hour: Int) {
            val request = OneTimeWorkRequestBuilder<IntelligentReminderWorker>()
                .setInitialDelay(minutesUntilNextTarget(hour), TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(false).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        private fun minutesUntilNextTarget(hour: Int): Long {
            val now = LocalDateTime.now()
            val target = now.toLocalDate().atTime(hour, 0).let {
                if (now.isBefore(it)) it else it.plusDays(1)
            }
            return ChronoUnit.MINUTES.between(now, target).coerceAtLeast(1)
        }
    }
}
