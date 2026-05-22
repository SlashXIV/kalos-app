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
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val prefs by lazy { context.getSharedPreferences("kalos_prefs", Context.MODE_PRIVATE) }

    fun isProgramEnabled(id: Long) = prefs.getBoolean("reminder_${id}_enabled", false)
    fun isProgramDayOf(id: Long) = prefs.getBoolean("reminder_${id}_day_of", true)
    fun isProgramDayBefore(id: Long) = prefs.getBoolean("reminder_${id}_day_before", false)

    fun setProgramEnabled(id: Long, v: Boolean) {
        prefs.edit().putBoolean("reminder_${id}_enabled", v).apply()
        schedule()
    }

    fun setProgramDayOf(id: Long, v: Boolean) {
        prefs.edit().putBoolean("reminder_${id}_day_of", v).apply()
        schedule()
    }

    fun setProgramDayBefore(id: Long, v: Boolean) {
        prefs.edit().putBoolean("reminder_${id}_day_before", v).apply()
        schedule()
    }

    fun schedule() {
        NotificationHelper.createChannel(context)
        val request = PeriodicWorkRequestBuilder<WorkoutReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(minutesUntilNextMorning(), TimeUnit.MINUTES)
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

    private fun minutesUntilNextMorning(): Long {
        val now = LocalDateTime.now()
        val target = now.toLocalDate().atTime(8, 0).let {
            if (now.isBefore(it)) it else it.plusDays(1)
        }
        return ChronoUnit.MINUTES.between(now, target).coerceAtLeast(1)
    }

    companion object {
        private const val WORK_NAME = "kalos_workout_reminder"
    }
}
