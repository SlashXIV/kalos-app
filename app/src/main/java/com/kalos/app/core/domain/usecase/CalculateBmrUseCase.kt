package com.kalos.app.core.domain.usecase

import com.kalos.app.core.domain.model.Sex
import com.kalos.app.core.domain.model.UserProfile
import javax.inject.Inject

class CalculateBmrUseCase @Inject constructor() {
    // Mifflin-St Jeor formula
    operator fun invoke(profile: UserProfile): Float {
        val base = 10f * profile.weightKg + 6.25f * profile.heightCm - 5f * profile.age
        return if (profile.sex == Sex.MALE) base + 5f else base - 161f
    }
}
