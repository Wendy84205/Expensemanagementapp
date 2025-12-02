package com.example.financeapp.components.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.financeapp.AppColorConstants
import com.example.financeapp.LocalLanguageViewModel
import com.example.financeapp.viewmodel.transaction.Category
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import kotlin.collections.isNotEmpty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectionScreen(
    navController: NavController,
    categoryViewModel: CategoryViewModel,
    transactionType: String?,
    returnTo: String?,
    onCategorySelected: (Category) -> Unit
) {
    val languageViewModel = LocalLanguageViewModel.current
    val categories by categoryViewModel.categories.collectAsState()
    
    val selectableCategories = remember(categories, transactionType) {
        if (transactionType != null) {
            categoryViewModel.getSelectableCategories(transactionType)
        } else {
            categories.filter { !it.isMainCategory }
        }
    }
    
    val mainCategories = remember(categories, transactionType) {
        if (transactionType != null) {
            categoryViewModel.getMainCategories(transactionType).filter { it.name != "Khác" }
        } else {
            categories.filter { it.isMainCategory && it.name != "Khác" }
        }
    }
    
    val categoryGroups = remember(mainCategories, selectableCategories) {
        mainCategories.map { mainCategory ->
            val subCategories = selectableCategories.filter { it.parentCategoryId == mainCategory.id }
            CategoryGroupData(mainCategory.name, subCategories, getGroupColor(mainCategory.name))
        }.filter { it.subCategories.isNotEmpty() }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        languageViewModel.getTranslation("select_category"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = languageViewModel.getTranslation("back"),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColorConstants.Navy
                )
            )
        },
        containerColor = AppColorConstants.SoftGray
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            categoryGroups.forEach { group ->
                item {
                    CategoryGroupSection(
                        groupName = group.name,
                        categories = group.subCategories,
                        onCategorySelected = onCategorySelected,
                        groupColor = group.color
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryGroupSection(
    groupName: String,
    categories: List<Category>,
    onCategorySelected: (com.example.financeapp.viewmodel.transaction.Category) -> Unit,
    groupColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header với icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            groupColor,
                            RoundedCornerShape(10.dp)
                        )
                        .clip(RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getGroupIcon(groupName),
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = groupName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColorConstants.TextDark
                    )
                    Text(
                        text = "${categories.size} danh mục con",
                        fontSize = 12.sp,
                        color = AppColorConstants.TextLight
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Grid layout giống CategoryScreen - 4 cột
            val rows = categories.chunked(4)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { rowCategories ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowCategories.forEach { category ->
                            CategorySelectionItem(
                                category = category,
                                onClick = { onCategorySelected(category) }
                            )
                        }
                        // Thêm các item trống để căn đều
                        if (rowCategories.size < 4) {
                            repeat(4 - rowCategories.size) {
                                Spacer(modifier = Modifier.width(60.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySelectionItem(
    category: com.example.financeapp.viewmodel.transaction.Category,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(60.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(Color(0xFFF7FAFC), CircleShape)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.icon,
                fontSize = 18.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.name,
            fontSize = 11.sp,
            color = Color(0xFF4A5568),
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            lineHeight = 12.sp
        )
    }
}

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color(0xFF667EEA)
    }
}

private fun getGroupColor(groupName: String): Color {
    return when {
        groupName.contains("sinh hoạt", ignoreCase = true) -> Color(0xFFFFCC80)
        groupName.contains("phát sinh", ignoreCase = true) -> Color(0xFFFFF59D)
        groupName.contains("cố định", ignoreCase = true) -> Color(0xFFBBDEFB)
        groupName.contains("đầu tư", ignoreCase = true) -> Color(0xFFC8E6C9)
        groupName.contains("lương", ignoreCase = true) -> Color(0xFFB2DFDB)
        groupName.contains("thu nhập", ignoreCase = true) -> Color(0xFFD1C4E9)
        else -> Color(0xFFE1BEE7)
    }
}

private fun getGroupIcon(groupName: String): String {
    return when {
        groupName.contains("sinh hoạt", ignoreCase = true) -> "🏠"
        groupName.contains("phát sinh", ignoreCase = true) -> "🛍️"
        groupName.contains("cố định", ignoreCase = true) -> "📋"
        groupName.contains("đầu tư", ignoreCase = true) -> "📈"
        groupName.contains("lương", ignoreCase = true) -> "💰"
        groupName.contains("thu nhập", ignoreCase = true) -> "💵"
        else -> "📁"
    }
}

data class CategoryGroupData(
    val name: String,
    val subCategories: List<com.example.financeapp.viewmodel.transaction.Category>,
    val color: Color
)

