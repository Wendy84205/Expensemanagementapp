package com.example.financeapp.components.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.financeapp.rememberLanguageText


data class NavItem(
    val title: String,
    val route: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector? = null,
)

@Composable
fun BottomNavBar(
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val homeText = rememberLanguageText("home")
    val transactionsText = rememberLanguageText("transactions")
    val statisticsText = rememberLanguageText("statistics")
    val settingsText = rememberLanguageText("settings")
    val aiText = "AI"

    val items = listOf(
        NavItem(
            title = homeText,
            route = "home",
            icon = Icons.Outlined.Dashboard,
            selectedIcon = Icons.Filled.Dashboard
        ),
        NavItem(
            title = transactionsText,
            route = "transactions",
            icon = Icons.Outlined.ReceiptLong,
            selectedIcon = Icons.Filled.ReceiptLong
        ),
        NavItem(
            title = aiText,
            route = "chat_ai",
            icon = Icons.Outlined.SmartToy,
            selectedIcon = Icons.Filled.SmartToy
        ),
        NavItem(
            title = statisticsText,
            route = "statistics",
            icon = Icons.Outlined.BarChart,
            selectedIcon = Icons.Filled.BarChart
        ),
        NavItem(
            title = settingsText,
            route = "settings",
            icon = Icons.Outlined.Settings,
            selectedIcon = Icons.Filled.Settings
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = modifier,
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            val iconToDisplay = if (isSelected && item.selectedIcon != null) {
                item.selectedIcon
            } else {
                item.icon
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = iconToDisplay,
                        contentDescription = item.title,
                        tint = if (isSelected) Color(0xFF3B82F6) else Color(0xFF64748B)
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 11.sp,
                        color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF64748B),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFFEFF6FF)
                )
            )
        }
    }
}