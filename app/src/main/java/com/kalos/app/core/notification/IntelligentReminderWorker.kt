package com.kalos.app.core.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kalos.app.core.domain.repository.MealRepository
import com.kalos.app.core.domain.repository.WaterRepository
import com.kalos.app.core.domain.repository.WorkoutRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.kalos.app.R

class IntelligentReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun mealRepository(): MealRepository
        fun workoutRepository(): WorkoutRepository
        fun waterRepository(): WaterRepository
    }

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("kalos_smart_reminders", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false)) return Result.success()

        // Chain the next day's run up-front, anchored to the configured hour, so a
        // failure below never breaks the schedule.
        IntelligentReminderScheduler.enqueueNext(applicationContext, prefs.getInt("hour", 20))

        val ep = EntryPointAccessors.fromApplication(applicationContext, WorkerEntryPoint::class.java)
        val today = LocalDate.now().toString()

        if (prefs.getBoolean("nutrition", true)) {
            val meals = ep.mealRepository().getMealsForDate(today).first()
            val hasFood = meals.any { it.items.isNotEmpty() }
            if (!hasFood) {
                NotificationHelper.postSmartReminder(
                    applicationContext,
                    applicationContext.getString(R.string.notif_nutrition_title),
                    applicationContext.getString(R.string.notif_nutrition_body),
                    NOTIF_NUTRITION,
                    NotificationHelper.DEST_NUTRITION,
                )
            }
        }

        if (prefs.getBoolean("workout", true)) {
            val inactivityDays = prefs.getInt("inactivity_days", 3)
            val trainedDates = ep.workoutRepository().getTrainedDates().first()
            val lastTrainDate = trainedDates.maxOrNull()?.let { LocalDate.parse(it) }
            val daysSince = if (lastTrainDate == null) Int.MAX_VALUE
                           else ChronoUnit.DAYS.between(lastTrainDate, LocalDate.now()).toInt()
            if (daysSince >= inactivityDays) {
                val msg = if (lastTrainDate == null) applicationContext.getString(R.string.notif_workout_never)
                          else applicationContext.resources.getQuantityString(R.plurals.notif_workout_since, daysSince, daysSince)
                NotificationHelper.postSmartReminder(
                    applicationContext,
                    applicationContext.getString(R.string.notif_workout_title),
                    msg,
                    NOTIF_WORKOUT,
                    NotificationHelper.DEST_WORKOUT,
                )
            }
        }

        if (prefs.getBoolean("hydration", false)) {
            val waterMl = ep.waterRepository().observeWaterForDate(today).first()
            val goalMl = ep.waterRepository().getGoalMl()
            if (waterMl < goalMl / 2) {
                NotificationHelper.postSmartReminder(
                    applicationContext,
                    applicationContext.getString(R.string.notif_water_title),
                    applicationContext.getString(R.string.notif_water_body, waterMl, goalMl),
                    NOTIF_HYDRATION,
                    NotificationHelper.DEST_WATER,
                )
            }
        }

        return Result.success()
    }

    companion object {
        private const val NOTIF_NUTRITION = 2001
        private const val NOTIF_WORKOUT = 2002
        private const val NOTIF_HYDRATION = 2003
    }
}
