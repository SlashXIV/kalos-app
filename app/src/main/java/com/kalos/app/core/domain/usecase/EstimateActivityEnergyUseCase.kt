package com.kalos.app.core.domain.usecase

import com.kalos.app.core.domain.model.ActivityType
import javax.inject.Inject
import kotlin.math.roundToInt

class EstimateActivityEnergyUseCase @Inject constructor() {
    // Gross energy: MET × body weight (kg) × duration (h). Display only, never added to the budget.
    operator fun invoke(type: ActivityType, weightKg: Float, durationSecs: Long): Int =
        (type.met * weightKg * durationSecs / 3600f).roundToInt()
}
