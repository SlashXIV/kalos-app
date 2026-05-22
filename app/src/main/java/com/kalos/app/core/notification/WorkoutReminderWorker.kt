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
        val programRepository = entryPoint.programRepository()
        val program = programRepository.getActive().first() ?: return Result.success()
        if (program.workouts.isEmpty()) return Result.success()

        val today = LocalDate.now()
        val todayDow = today.dayOfWeek.value
        val tomorrowDow = today.plusDays(1).dayOfWeek.value

        val dayOf = inputData.getBoolean(KEY_DAY_OF, true)
        val dayBefore = inputData.getBoolean(KEY_DAY_BEFORE, false)

        if (dayOf) {
            val todayWorkout = program.workouts.firstOrNull { it.dayOfWeek == todayDow }
            if (todayWorkout != null) {
                val name = todayWorkout.template?.name ?: "Séance"
                NotificationHelper.postWorkoutReminder(
                    applicationContext,
                    "Séance aujourd'hui 💪",
                    "$name — ${program.name}",
                    notifId = NOTIF_ID_TODAY,
                )
            }
        }

        if (dayBefore) {
            val tomorrowWorkout = program.workouts.firstOrNull { it.dayOfWeek == tomorrowDow }
            if (tomorrowWorkout != null) {
                val name = tomorrowWorkout.template?.name ?: "Séance"
                NotificationHelper.postWorkoutReminder(
                    applicationContext,
                    "Rappel : séance demain",
                    "$name — ${program.name}",
                    notifId = NOTIF_ID_TOMORROW,
                )
            }
        }

        return Result.success()
    }

    companion object {
        const val KEY_DAY_OF = "day_of"
        const val KEY_DAY_BEFORE = "day_before"
        private const val NOTIF_ID_TODAY = 1001
        private const val NOTIF_ID_TOMORROW = 1002
    }
}
