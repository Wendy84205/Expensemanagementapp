package com.example.financeapp.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.financeapp.rememberLanguageText

data class NavItem(
    val title: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun BottomNavBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    // Lấy các text cho navigation
    val homeText = rememberLanguageText("home")
    val transactionsText = rememberLanguageText("transactions")
    val statisticsText = rememberLanguageText("statistics")
    val settingsText = rememberLanguageText("settings")
    val aiText = "AI"

    val items = listOf(
        NavItem(homeText, "home", Icons.Default.Home),
        NavItem(transactionsText, "transactions", Icons.AutoMirrored.Filled.List),
        NavItem(aiText, "chat_ai", Icons.Default.SmartToy),
        NavItem(statisticsText, "statistics", Icons.Default.PieChart),
        NavItem(settingsText, "settings", Icons.Default.Settings)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = modifier,
        containerColor = Color.White.copy(alpha = 0.95f)
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
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
                        item.icon,
                        contentDescription = item.title,
                        tint = if (currentRoute == item.route) Color(0xFF1565C0) else Color.Gray
                    )
                },
                label = {
                    Text(
                        item.title,
                        fontSize = 12.sp,
                        color = if (currentRoute == item.route) Color(0xFF1565C0) else Color.Gray
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}