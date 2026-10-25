package com.kalos.app.core.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kalos.app.core.domain.repository.ProgramRepository
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import com.kalos.app.R

class WorkoutReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun programRepository(): ProgramRepository
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WorkerEntryPoint::class.java,
        )
        val program = entryPoint.programRepository().getActive().first() ?: return Result.success()
        if (program.workouts.isEmpty()) return Result.success()

        val prefs = applicationContext.getSharedPreferences("kalos_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("reminder_${program.id}_enabled", false)
        if (!enabled) return Result.success()

        val dayOf = prefs.getBoolean("reminder_${program.id}_day_of", true)
        val dayBefore = prefs.getBoolean("reminder_${program.id}_day_before", false)

        val today = LocalDate.now()
        val todayDow = today.dayOfWeek.value
        val tomorrowDow = today.plusDays(1).dayOfWeek.value

        if (dayOf) {
            program.workoutOn(todayDow)?.let { pw ->
                val name = pw.template?.name ?: applicationContext.getString(R.string.notif_program_unnamed)
                NotificationHelper.postWorkoutReminder(
                    applicationContext,
                    applicationContext.getString(R.string.notif_program_today),
                    applicationContext.getString(R.string.notif_program_body, name, program.name),
                    notifId = NOTIF_ID_TODAY,
                )
            }
        }

        if (dayBefore) {
            program.workoutOn(tomorrowDow)?.let { pw ->
                val name = pw.template?.name ?: applicationContext.getString(R.string.notif_program_unnamed)
                NotificationHelper.postWorkoutReminder(
                    applicationContext,
                    applicationContext.getString(R.string.notif_program_tomorrow),
                    applicationContext.getString(R.string.notif_program_body, name, program.name),
                    notifId = NOTIF_ID_TOMORROW,
                )
            }
        }

        return Result.success()
    }

    companion object {
        private const val NOTIF_ID_TODAY = 1001
        private const val NOTIF_ID_TOMORROW = 1002
    }
}
