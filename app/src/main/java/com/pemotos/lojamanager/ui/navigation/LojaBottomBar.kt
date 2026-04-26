package com.pemotos.lojamanager.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun LojaBottomBar(
    currentRoute: String?,
    onNavigate: (TopLevelDestination) -> Unit,
) {
    NavigationBar {
        TopLevelDestination.entries.forEach { dest ->
            NavigationBarItem(
                selected = currentRoute == dest.route,
                onClick = { onNavigate(dest) },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) },
                alwaysShowLabel = true
            )
        }
    }
}
