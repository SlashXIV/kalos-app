package com.kalos.app.feature.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.ui.component.FitText
import com.kalos.app.core.ui.component.Graduation
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.SectionLabel
import com.kalos.app.core.ui.motion.rememberHaptics
import com.kalos.app.core.ui.theme.Brightness
import com.kalos.app.core.ui.theme.KalosShape
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.core.ui.theme.MotionPreference
import com.kalos.app.core.ui.theme.Palettes
import com.kalos.app.core.ui.theme.isDark
import androidx.compose.foundation.isSystemInDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.appearance.collectAsStateWithLifecycle()
    val dark = settings.brightness.isDark(isSystemInDarkTheme())
    val haptics = rememberHaptics()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.appearance_title),
                small = true,
                navigation = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
            SectionLabel(stringResource(R.string.appearance_palette))
            Palettes.all.forEach { palette ->
                val selected = palette.id == settings.paletteId
                val accent = (if (dark) palette.dark else palette.light).primary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                            shape = KalosShape.card,
                        )
                        .selectable(selected = selected, role = Role.RadioButton) {
                            haptics.tick(); viewModel.setPalette(palette.id)
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(stringResource(palette.nameRes), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Graduation(color = accent, modifier = Modifier.weight(1f).height(14.dp))
                    RadioButton(selected = selected, onClick = null)
                }
            }

            SectionLabel(stringResource(R.string.appearance_brightness))
            Segmented(
                options = listOf(
                    Brightness.SYSTEM to R.string.appearance_brightness_system,
                    Brightness.LIGHT to R.string.appearance_brightness_light,
                    Brightness.DARK to R.string.appearance_brightness_dark,
                ),
                selected = settings.brightness,
                onSelect = { haptics.tick(); viewModel.setBrightness(it) },
            )

            SectionLabel(stringResource(R.string.appearance_motion))
            Segmented(
                options = listOf(
                    MotionPreference.EXPRESSIVE to R.string.appearance_motion_expressive,
                    MotionPreference.CALM to R.string.appearance_motion_calm,
                ),
                selected = settings.motion,
                onSelect = { haptics.tick(); viewModel.setMotion(it) },
            )
            Text(
                stringResource(
                    if (settings.motion == MotionPreference.EXPRESSIVE) R.string.appearance_motion_expressive_desc
                    else R.string.appearance_motion_calm_desc,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .toggleable(value = settings.vibrations, role = Role.Switch) { viewModel.setVibrations(it) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.appearance_vibrations), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.appearance_vibrations_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = settings.vibrations, onCheckedChange = null)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Segmented(options: List<Pair<T, Int>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size, KalosShape.control),
                icon = {},
                modifier = Modifier.heightIn(min = 48.dp),
            ) { FitText(stringResource(label)) }
        }
    }
}
