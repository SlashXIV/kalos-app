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
import com.kalos.app.core.domain.model.WorkoutSet

/**
 * Shared edit-set dialog used from both the post-workout summary and the
 * history detail screen. Lets the user correct reps / weight on a completed set;
 * persistence is delegated to the caller via [onConfirm].
 *
 * Behavior:
 * - reps field is digit-only.
 * - both fields select-all on focus to prevent appended-digit mistakes
 *   (e.g. "10" → typing "8" → "108").
 * - weight accepts both `.` and `,` decimal separators (French keyboard).
 */
@Composable
fun EditWorkoutSetDialog(
    set: WorkoutSet,
    onDismiss: () -> Unit,
    onConfirm: (reps: Int, weightKg: Float) -> Unit,
) {
    val initWeight = set.weightKg.toWeightInput()
    var repsValue by remember {
        mutableStateOf(TextFieldValue(set.reps.toString(), TextRange(set.reps.toString().length)))
    }
    var weightValue by remember {
        mutableStateOf(TextFieldValue(initWeight, TextRange(initWeight.length)))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Série ${set.setNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        },
        confirmButton = {
            TextButton(onClick = {
                val reps = repsValue.text.toIntOrNull() ?: set.reps
                val weight = weightValue.text.replace(',', '.').toFloatOrNull() ?: set.weightKg
                onConfirm(reps, weight)
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
