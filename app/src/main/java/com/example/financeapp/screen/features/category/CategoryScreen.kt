package com.example.financeapp.screen.features.category

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.financeapp.viewmodel.transaction.Category
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import com.example.financeapp.LocalLanguageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    navController: NavController,
    categoryViewModel: CategoryViewModel
) {
    val languageViewModel = LocalLanguageViewModel.current
    var selectedTab by remember { mutableStateOf(0) }
    val categories by categoryViewModel.categories.collectAsState()
    val transactionType = if (selectedTab == 0) "expense" else "income"
    val categoryGroups = remember(categories, transactionType) {
        categoryViewModel.getCategoriesGroupedByParent(transactionType)
    }

    val currentCategoryCount = remember(categories, transactionType) {
        categories.count { it.type == transactionType && !it.isMainCategory }
    }

    val tabs = listOf(
        languageViewModel.getTranslation("spending"),
        languageViewModel.getTranslation("income")
    )

    // Colors
    val primaryColor = Color(0xFF2196F3) // Blue
    val backgroundColor = Color(0xFFF5F5F5) // Light Gray
    val cardColor = Color.White
    val textColor = Color(0xFF333333) // Dark Gray
    val subtitleColor = Color(0xFF666666) // Gray

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = languageViewModel.getTranslation("category_management"),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = languageViewModel.getTranslation("back"),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row đơn giản
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = primaryColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedTab == index) primaryColor else Color(0xFF718096),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Card thêm danh mục mới
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("add_category?type=$transactionType")
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(primaryColor, CircleShape)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = languageViewModel.getTranslation("add_new_category"),
                                    color = Color(0xFF2D3748),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${languageViewModel.getTranslation("has")} $currentCategoryCount/20 ${languageViewModel.getTranslation("categories")}",
                                    color = Color(0xFF718096),
                                    fontSize = 12.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFFA0AEC0)
                            )
                        }
                    }
                }

                // Hiển thị các nhóm danh mục
                items(categoryGroups.toList()) { (mainCategory, subCategories) ->
                    CategoryGroupComposable(
                        mainCategory = mainCategory,
                        subCategories = subCategories,
                        viewModel = categoryViewModel,
                        navController = navController,
                        primaryColor = primaryColor
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryGroupComposable(
    mainCategory: Category,
    subCategories: List<Category>,
    viewModel: CategoryViewModel,
    navController: NavController,
    primaryColor: Color = Color(0xFF0F4C75)
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header nhóm
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            getGroupColor(mainCategory.name),
                            RoundedCornerShape(10.dp)
                        )
                        .clip(RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getGroupIcon(mainCategory.name),
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = mainCategory.name,
                        color = Color(0xFF2D3748),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${subCategories.size} danh mục con",
                        color = Color(0xFF718096),
                        fontSize = 12.sp
                    )
                }

                // Nút thêm danh mục con
                val languageViewModel = LocalLanguageViewModel.current
                if (mainCategory.name != "Khác" && viewModel.canAddSubCategory(mainCategory.id)) {
                    IconButton(
                        onClick = {
                            navController.navigate("add_sub_category?parentId=${mainCategory.id}")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = languageViewModel.getTranslation("add_sub_category"),
                            tint = primaryColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hiển thị danh mục con dạng lưới đơn giản
            if (subCategories.isNotEmpty()) {
                SubCategoryGrid(
                    subCategories = subCategories,
                    viewModel = viewModel,
                    navController = navController,
                    primaryColor = primaryColor
                )
            } else {
                EmptySubCategoryState()
            }
        }
    }
}

@Composable
fun SubCategoryGrid(
    subCategories: List<Category>,
    viewModel: CategoryViewModel,
    navController: NavController,
    primaryColor: Color
) {
    val rows = subCategories.chunked(4) // 4 cột thay vì 3

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowCategories.forEach { category ->
                    SubCategoryItem(
                        category = category,
                        viewModel = viewModel,
                        navController = navController,
                        primaryColor = primaryColor
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

@Composable
fun SubCategoryItem(
    category: Category,
    viewModel: CategoryViewModel,
    navController: NavController,
    primaryColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
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
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 12.sp
        )
    }
}

@Composable
fun EmptySubCategoryState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = Color(0xFFCBD5E0),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Chưa có danh mục con",
                color = Color(0xFFA0AEC0),
                fontSize = 14.sp
            )
        }
    }
}

// Hàm lấy icon cho từng nhóm danh mục
private fun getGroupIcon(groupName: String): String {
    return when {
        groupName.contains("sinh hoạt", ignoreCase = true) -> "🏠"
        groupName.contains("phát sinh", ignoreCase = true) -> "🎯"
        groupName.contains("cố định", ignoreCase = true) -> "📊"
        groupName.contains("đầu tư", ignoreCase = true) -> "💹"
        groupName.contains("lương", ignoreCase = true) -> "💰"
        groupName.contains("thu nhập", ignoreCase = true) -> "💸"
        groupName.contains("khác", ignoreCase = true) -> "📦"
        else -> "📁"
    }
}

// Hàm lấy màu cho từng nhóm danh mục
private fun getGroupColor(groupName: String): Color {
    return when {
        groupName.contains("sinh hoạt", ignoreCase = true) -> Color(0xFFFFF3E0)
        groupName.contains("phát sinh", ignoreCase = true) -> Color(0xFFFFF9C4)
        groupName.contains("cố định", ignoreCase = true) -> Color(0xFFE3F2FD)
        groupName.contains("đầu tư", ignoreCase = true) -> Color(0xFFE8F5E8)
        groupName.contains("lương", ignoreCase = true) -> Color(0xFFE0F2F1)
        groupName.contains("thu nhập", ignoreCase = true) -> Color(0xFFF3E5F5)
        groupName.contains("khác", ignoreCase = true) -> Color(0xFFFCE4EC)
        else -> Color(0xFFF3E5F5)
    }
}