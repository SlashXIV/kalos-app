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
    private val prefs by lazy {
        context.getSharedPreferences("kalos_prefs", Context.MODE_PRIVATE)
    }

    fun schedule() {
        val dayOf = prefs.getBoolean(PREF_DAY_OF, true)
        val dayBefore = prefs.getBoolean(PREF_DAY_BEFORE, false)

        if (!dayOf && !dayBefore) {
            cancel()
            return
        }

        NotificationHelper.createChannel(context)

        val data = workDataOf(
            WorkoutReminderWorker.KEY_DAY_OF to dayOf,
            WorkoutReminderWorker.KEY_DAY_BEFORE to dayBefore,
        )
        val initialDelay = minutesUntilNextMorning()
        val request = PeriodicWorkRequestBuilder<WorkoutReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MINUTES)
            .setInputData(data)
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

    fun setDayOf(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_DAY_OF, enabled).apply()
        schedule()
    }

    fun setDayBefore(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_DAY_BEFORE, enabled).apply()
        schedule()
    }

    fun isDayOfEnabled(): Boolean = prefs.getBoolean(PREF_DAY_OF, true)
    fun isDayBeforeEnabled(): Boolean = prefs.getBoolean(PREF_DAY_BEFORE, false)

    private fun minutesUntilNextMorning(): Long {
        val now = LocalDateTime.now()
        val target8AM = now.toLocalDate().atTime(8, 0)
        val target = if (now.isBefore(target8AM)) target8AM else target8AM.plusDays(1)
        return ChronoUnit.MINUTES.between(now, target).coerceAtLeast(1)
    }

    companion object {
        private const val WORK_NAME = "kalos_workout_reminder"
        const val PREF_DAY_OF = "reminder_day_of"
        const val PREF_DAY_BEFORE = "reminder_day_before"
    }
}
