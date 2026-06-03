package com.kalos.app.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.kalos.app.core.domain.model.ExerciseTrackingMode
import com.kalos.app.core.domain.model.WorkoutSet
import com.kalos.app.core.ui.util.formatSecsAsDuration
import com.kalos.app.core.ui.util.parseDurationToSecs

/**
 * Shared edit-set dialog used from both the post-workout summary and the
 * history detail screen. Lets the user correct reps / weight / duration on a
 * completed set; persistence is delegated to the caller via [onConfirm].
 *
 * Inputs rendered are driven by [trackingMode]:
 * - REPS_WEIGHT      → reps + weight (current default behaviour for musculation).
 * - DURATION         → duration (mm:ss) only.
 * - DURATION_WEIGHT  → weight + duration.
 *
 * Behavior:
 * - reps field is digit-only.
 * - all fields select-all on focus to prevent appended-digit mistakes
 *   (e.g. "10" → typing "8" → "108").
 * - weight accepts both `.` and `,` decimal separators (French keyboard).
 * - duration accepts "mm:ss", "ss" or empty.
 */
@Composable
fun EditWorkoutSetDialog(
    set: WorkoutSet,
    trackingMode: ExerciseTrackingMode,
    onDismiss: () -> Unit,
    onConfirm: (reps: Int, weightKg: Float, durationSecs: Int) -> Unit,
) {
    val initWeight = set.weightKg.toWeightInput()
    val initDuration = formatSecsAsDuration(set.durationSecs)
    var repsValue by remember {
        mutableStateOf(TextFieldValue(set.reps.toString(), TextRange(set.reps.toString().length)))
    }
    var weightValue by remember {
        mutableStateOf(TextFieldValue(initWeight, TextRange(initWeight.length)))
    }
    var durationValue by remember {
        mutableStateOf(TextFieldValue(initDuration, TextRange(initDuration.length)))
    }

    val showReps = trackingMode == ExerciseTrackingMode.REPS_WEIGHT
    val showWeight = trackingMode == ExerciseTrackingMode.REPS_WEIGHT ||
        trackingMode == ExerciseTrackingMode.DURATION_WEIGHT
    val showDuration = trackingMode == ExerciseTrackingMode.DURATION ||
        trackingMode == ExerciseTrackingMode.DURATION_WEIGHT

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Série ${set.setNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (showReps) {
                    OutlinedTextField(
                        value = repsValue,
                        onValueChange = { tv ->
                            val filtered = tv.text.filter { it.isDigit() }
                            repsValue = TextFieldValue(filtered, TextRange(filtered.length))
                        },
                        modifier = Modifier.fillMaxWidth().onFocusChanged { fs ->
                            if (fs.isFocused)
                                repsValue = repsValue.copy(selection = TextRange(0, repsValue.text.length))
                        },
                        label = { Text("Répétitions") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                if (showWeight) {
                    OutlinedTextField(
                        value = weightValue,
                        onValueChange = { weightValue = it },
                        modifier = Modifier.fillMaxWidth().onFocusChanged { fs ->
                            if (fs.isFocused)
                                weightValue = weightValue.copy(selection = TextRange(0, weightValue.text.length))
                        },
                        label = { Text("Poids (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                }
                if (showDuration) {
                    OutlinedTextField(
                        value = durationValue,
                        onValueChange = { tv ->
                            val filtered = tv.text.filter { it.isDigit() || it == ':' }
                            durationValue = TextFieldValue(filtered, TextRange(filtered.length))
                        },
                        modifier = Modifier.fillMaxWidth().onFocusChanged { fs ->
                            if (fs.isFocused)
                                durationValue = durationValue.copy(selection = TextRange(0, durationValue.text.length))
                        },
                        label = { Text("Durée (mm:ss)") },
                        placeholder = { Text("0:30") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val reps = if (showReps) repsValue.text.toIntOrNull() ?: set.reps else 0
                val weight = if (showWeight) weightValue.text.replace(',', '.').toFloatOrNull() ?: set.weightKg
                             else 0f
                val durationSecs = if (showDuration) parseDurationToSecs(durationValue.text) ?: set.durationSecs
                                   else 0
                onConfirm(reps, weight, durationSecs)
            }) { Text("Confirmer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}

/** "180.0" → "180", "82.5" → "82.5", 0f → "". */
private fun Float.toWeightInput(): String = when {
    this <= 0f -> ""
    this == toLong().toFloat() -> toLong().toString()
    else -> "%.1f".format(this)
}
