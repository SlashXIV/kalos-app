package com.kalos.app.core.domain.model

/** Meilleure charge d'un exercice pour un nombre de répétitions (séries validées). */
data class RepRecord(val reps: Int, val weightKg: Float)

/**
 * Records qui disent quelque chose : une charge n'est gardée que si aucune série à plus de
 * répétitions ne l'a égalée (20 kg × 8 n'apprend rien si l'on a fait 24 kg × 10). Répétitions croissantes.
 */
fun List<RepRecord>.meaningful(): List<RepRecord> {
    var bestWithMoreReps = 0f
    return sortedByDescending { it.reps }
        .filter { r -> (r.weightKg > bestWithMoreReps).also { kept -> if (kept) bestWithMoreReps = r.weightKg } }
        .reversed()
}
