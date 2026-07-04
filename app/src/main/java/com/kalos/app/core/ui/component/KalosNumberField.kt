package com.kalos.app.core.ui.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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

/**
 * Outlined text field for entering a short value that selects its whole content on focus,
 * so tapping an existing value to change it doesn't drop the cursor mid-number and append
 * digits (the miss-click that plagued the workout set inputs).
 *
 * String-based API (callers keep their own `String` state and any filtering in [onValueChange]);
 * the select-all behaviour is handled internally via [TextFieldValue].
 */
@Composable
fun KalosNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null,
    suffix: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    singleLine: Boolean = true,
    isError: Boolean = false,
    enabled: Boolean = true,
) {
    var tfv by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    // Keep the internal buffer in sync when the external value diverges (reset, caller-side
    // filtering, programmatic change). Cursor goes to the end in that case.
    if (value != tfv.text) {
        tfv = tfv.copy(text = value, selection = TextRange(value.length))
    }

    OutlinedTextField(
        value = tfv,
        onValueChange = { newValue ->
            tfv = newValue
            if (newValue.text != value) onValueChange(newValue.text)
        },
        modifier = modifier.onFocusChanged { focusState ->
            if (focusState.isFocused && tfv.text.isNotEmpty()) {
                tfv = tfv.copy(selection = TextRange(0, tfv.text.length))
            }
        },
        label = label,
        placeholder = placeholder,
        prefix = prefix,
        suffix = suffix,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        isError = isError,
        enabled = enabled,
    )
}
