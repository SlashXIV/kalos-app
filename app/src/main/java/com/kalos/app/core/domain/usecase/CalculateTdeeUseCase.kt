package com.kalos.app.core.domain.usecase

import com.kalos.app.core.domain.model.UserProfile
import javax.inject.Inject

class CalculateTdeeUseCase @Inject constructor(
    private val calculateBmr: CalculateBmrUseCase,
) {
    operator fun invoke(profile: UserProfile): Float {
        val bmr = calculateBmr(profile)
        return bmr * profile.activityLevel.multiplier
    }
}
