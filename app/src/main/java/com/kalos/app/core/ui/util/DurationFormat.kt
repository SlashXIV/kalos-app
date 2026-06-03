package com.kalos.app.core.ui.util

/**
 * Lightweight `mm:ss` ↔ seconds helpers for duration-tracked sets (cardio, holds).
 *
 * Accepted input forms for [parseDurationToSecs]:
 * - "" → 0
 * - "30"     → 30 seconds
 * - "1:30"   → 90 seconds
 * - "12:05"  → 725 seconds
 * - "1:2"    → 62 seconds (single-digit seconds tolerated on input)
 *
 * Invalid forms return null so callers can decide whether to keep the previous value
 * or treat it as 0.
 */
fun parseDurationToSecs(input: String): Int? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return 0
    if (!trimmed.contains(":")) return trimmed.toIntOrNull()?.takeIf { it >= 0 }
    val parts = trimmed.split(":")
    if (parts.size != 2) return null
    val m = parts[0].toIntOrNull()?.takeIf { it >= 0 } ?: return null
    val s = parts[1].toIntOrNull()?.takeIf { it in 0..59 } ?: return null
    return m * 60 + s
}

/** 0 → "", 30 → "0:30", 90 → "1:30", 725 → "12:05". */
fun formatSecsAsDuration(secs: Int): String {
    if (secs <= 0) return ""
    val m = secs / 60
    val s = secs % 60
    return "%d:%02d".format(m, s)
}
