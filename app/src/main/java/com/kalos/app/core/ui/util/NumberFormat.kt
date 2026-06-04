package com.kalos.app.core.ui.util

import java.text.NumberFormat
import java.util.Locale

/**
 * Locale-aware integer formatting with grouping: 23673 → "23 673" in French.
 * Deliberately delegates the separator choice to [NumberFormat] — no assumption
 * about the exact grouping character (narrow NBSP vs space vs dot).
 *
 * Note: [NumberFormat] is not thread-safe; this helper is intended for
 * composition-time formatting on the main thread only.
 */
fun formatGroupedInt(value: Number): String =
    NumberFormat.getIntegerInstance(Locale.FRENCH).format(value)
