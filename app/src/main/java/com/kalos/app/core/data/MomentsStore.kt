package com.kalos.app.core.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Moments qui ne se jouent qu'une fois par jour (objectif atteint). */
@Singleton
class MomentsStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("kalos_moments", Context.MODE_PRIVATE)

    /** Vrai au premier appel pour [date], faux ensuite : le moment est consommé. */
    fun claimGoalMoment(date: String): Boolean {
        if (prefs.getString(KEY_GOAL, null) == date) return false
        prefs.edit().putString(KEY_GOAL, date).apply()
        return true
    }

    private companion object {
        const val KEY_GOAL = "goal_reached_date"
    }
}
