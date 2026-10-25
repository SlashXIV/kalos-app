package com.kalos.app.core.ui.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.ZoneId

/**
 * Today's date, emitted on collection and again at each midnight while collected.
 * Screens collect with WhileSubscribed, so coming back to the app re-reads the date as well
 * (a delay does not advance while the device sleeps).
 */
fun todayFlow(): Flow<LocalDate> = flow {
    while (true) {
        val today = LocalDate.now()
        emit(today)
        val nextMidnight = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        delay((nextMidnight - System.currentTimeMillis()).coerceAtLeast(1_000))
    }
}.distinctUntilChanged()
