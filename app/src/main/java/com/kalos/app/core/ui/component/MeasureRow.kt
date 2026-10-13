package com.kalos.app.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.kalos.app.R
import com.kalos.app.core.ui.theme.KalosTheme

/**
 * Une quantité et son objectif : libellé avec sa pastille, « 118 / 160 g », règle graduée.
 * TalkBack lit une seule phrase : « Protéines, 118 sur 160 grammes » (+ « au-dessus de l'objectif »).
 */
@Composable
fun MeasureRow(
    label: String,
    value: Float,
    goal: Float,
    valueText: String,
    goalText: String,
    unit: String,
    spokenUnit: String,
    color: Color,
    modifier: Modifier = Modifier,
    overLimit: Float = 1.05f,
) {
    val over = goal > 0f && value > goal * overLimit
    val sentence = stringResource(R.string.measure_sentence, label, valueText, goalText, spokenUnit) +
        if (over) ", " + stringResource(R.string.state_over_target) else ""
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = sentence }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Swatch(color)
            Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 6.dp).weight(1f))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = if (over) KalosTheme.nutrients.overTarget else Color.Unspecified)) { append(valueText) }
                    withStyle(SpanStyle(color = muted)) { append(" / $goalText $unit") }
                },
                style = KalosTheme.type.metricSmall,
            )
        }
        GraduatedScale(value = value, goal = goal, color = if (over) KalosTheme.nutrients.overTarget else color)
    }
}
