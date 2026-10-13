package com.kalos.app.core.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.kalos.app.core.ui.theme.KalosShape

/** Puce de filtre : contour discret, choix actif à l'encre (fond plein), état « sélectionné » annoncé. */
@Composable
fun KalosFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedIcon: ImageVector? = null,
) {
    val colors = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, style = MaterialTheme.typography.labelMedium) },
        shape = KalosShape.chip,
        leadingIcon = if (selectedIcon != null && selected) {
            { Icon(selectedIcon, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.onSurface,
            selectedLabelColor = colors.surface,
            selectedLeadingIconColor = colors.surface,
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = colors.outlineVariant),
    )
}
