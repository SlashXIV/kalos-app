package com.kalos.app.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kalos.app.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** « Aujourd’hui », « Hier », sinon la date au motif [pattern] (français). */
@Composable
fun dayLabel(date: LocalDate, pattern: String): String {
    val today = LocalDate.now()
    return when (date) {
        today -> stringResource(R.string.today_title)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        else -> date.format(DateTimeFormatter.ofPattern(pattern, Locale.FRENCH))
    }
}
