package com.kalos.app.core.domain.model

/**
 * Free activity (logged without exercises) with its MET value from the Compendium of
 * Physical Activities (general or moderate intensity entry).
 */
enum class ActivityType(val met: Float) {
    CLIMBING(7.5f),
    HIKING(6.0f),
    SWIMMING(6.0f),
    RUNNING(9.8f),
    CYCLING(7.5f),
    FOOTBALL(7.0f),
    YOGA(2.5f),
    BOXING(7.8f),
    TENNIS(7.3f),
    SKIING(6.0f),
}
