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

        val ep = EntryPointAccessors.fromApplication(applicationContext, WorkerEntryPoint::class.java)
        val today = LocalDate.now().toString()

        if (prefs.getBoolean("nutrition", true)) {
            val meals = ep.mealRepository().getMealsForDate(today).first()
            val hasFood = meals.any { it.items.isNotEmpty() }
            if (!hasFood) {
                NotificationHelper.postSmartReminder(
                    applicationContext,
                    "N'oubliez pas de journaliser 🥗",
                    "Aucun repas enregistré aujourd'hui — suivez votre nutrition pour progresser.",
                    NOTIF_NUTRITION,
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
                val msg = if (lastTrainDate == null) "Vous n'avez encore fait aucune séance — commencez aujourd'hui !"
                          else "Dernière séance il y a $daysSince jours — remettez-vous en selle !"
                NotificationHelper.postSmartReminder(
                    applicationContext,
                    "Il est temps de s'entraîner 💪",
                    msg,
                    NOTIF_WORKOUT,
                )
            }
        }

        if (prefs.getBoolean("hydration", false)) {
            val waterMl = ep.waterRepository().observeWaterForDate(today).first()
            val goalMl = ep.waterRepository().getGoalMl()
            if (waterMl < goalMl / 2) {
                NotificationHelper.postSmartReminder(
                    applicationContext,
                    "Pensez à vous hydrater 💧",
                    "$waterMl ml bus — objectif ${goalMl} ml.",
                    NOTIF_HYDRATION,
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
