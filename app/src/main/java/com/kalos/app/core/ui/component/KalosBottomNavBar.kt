package com.kalos.app.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.kalos.app.core.ui.motion.LocalMotionLevel
import com.kalos.app.core.ui.motion.rememberHaptics
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.navigation.BottomNavItem

/** Barre des quatre onglets : l'onglet actif est souligné d'une graduation, pas d'une pastille. */
@Composable
fun KalosBottomNavBar(
    items: List<BottomNavItem>,
    currentDestination: NavDestination?,
    onItemClick: (BottomNavItem) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    Column(Modifier.fillMaxWidth().background(colors.surfaceContainer)) {
        HorizontalDivider(color = colors.outlineVariant)
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp).selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEach { item ->
                val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                val tint = if (selected) colors.onSurface else colors.onSurfaceVariant
                val mark by animateFloatAsState(
                    if (selected) 1f else 0f,
                    if (LocalMotionLevel.current.animates) androidx.compose.animation.core.tween(200) else androidx.compose.animation.core.snap(),
                    label = "onglet",
                )
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .selectable(selected = selected, role = Role.Tab) {
                            if (!selected) haptics.tick()
                            onItemClick(item)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                ) {
                    Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                    FitText(stringResource(item.label).uppercase(), style = KalosTheme.type.label, color = tint)
                    Graduation(
                        color = colors.primary,
                        modifier = Modifier.width(34.dp).height(5.dp).alpha(mark),
                        spacing = 7.dp,
                    )
                }
            }
        }
    }
}
