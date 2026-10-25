package com.kalos.app.core.domain.usecase

import com.kalos.app.core.domain.model.RepRecord
import javax.inject.Inject

class EstimateOneRepMaxUseCase @Inject constructor() {
    // Epley : charge × (1 + répétitions / 30). Au-delà de 12 répétitions l'estimation dérive : ignorées.
    operator fun invoke(records: List<RepRecord>): Float? = records
        .filter { it.reps in 1..MAX_REPS && it.weightKg > 0f }
        .maxOfOrNull { if (it.reps == 1) it.weightKg else it.weightKg * (1 + it.reps / 30f) }

    companion object {
        const val MAX_REPS = 12
    }
}
