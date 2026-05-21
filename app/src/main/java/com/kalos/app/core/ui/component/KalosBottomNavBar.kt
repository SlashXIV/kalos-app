package com.kalos.app.core.ui.component

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.kalos.app.navigation.BottomNavItem

@Composable
fun KalosBottomNavBar(
    items: List<BottomNavItem>,
    currentDestination: NavDestination?,
    onItemClick: (BottomNavItem) -> Unit,
) {
    NavigationBar {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = { onItemClick(item) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.icon,
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}
