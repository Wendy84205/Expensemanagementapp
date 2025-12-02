package com.example.financeapp.screen.features.category

import android.util.Log
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.financeapp.viewmodel.transaction.Category
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import com.example.financeapp.LocalLanguageViewModel
import com.example.financeapp.viewmodel.settings.LanguageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryScreen(
    navController: NavController,
    viewModel: CategoryViewModel
) {
    val languageViewModel = LocalLanguageViewModel.current
    val categories by viewModel.categories.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        languageViewModel.getTranslation("spending"),
        languageViewModel.getTranslation("income")
    )
    val tabTypes = listOf("expense", "income")

    var categoryName by remember { mutableStateOf(TextFieldValue("")) }
    var selectedIcon by remember { mutableStateOf("📁") }
    var selectedMainCategory by remember { mutableStateOf<Category?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }

    val mainCategories = remember(selectedTab, categories) {
        categories.filter { it.isMainCategory && it.type == tabTypes[selectedTab] }
    }

    // 🎨 Màu sắc đồng bộ với app
    val primaryColor = Color(0xFF0F4C75) // Navy
    val backgroundColor = Color(0xFFF5F7FA) // SoftGray
    val cardColor = Color.White
    val textColor = Color(0xFF2D3748)
    val subtitleColor = Color(0xFF718096)

    LaunchedEffect(mainCategories) {
        Log.d(
            "AddCategory",
            "Có ${mainCategories.size} danh mục chính (tab: ${tabTypes[selectedTab]})"
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        languageViewModel.getTranslation("create_category"),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = primaryColor
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardColor)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        if (categoryName.text.isNotBlank() && selectedMainCategory != null) {
                            viewModel.addCategory(
                                name = categoryName.text,
                                type = tabTypes[selectedTab],
                                isMainCategory = false,
                                parentCategoryId = selectedMainCategory?.id,
                                icon = selectedIcon
                            )
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = categoryName.text.isNotBlank() && selectedMainCategory != null,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        disabledContainerColor = Color(0xFFE2E8F0)
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Text(
                        languageViewModel.getTranslation("create_category"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        },
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs với thiết kế đẹp hơn
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = primaryColor,
                    modifier = Modifier.fillMaxWidth(),
                    divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier
                                .tabIndicatorOffset(tabPositions[selectedTab])
                                .height(3.dp)
                                .padding(horizontal = 16.dp),
                            color = primaryColor
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                selectedMainCategory = null
                                categoryName = TextFieldValue("")
                            },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == index) primaryColor else subtitleColor,
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }
                }
            }

            // Nội dung chính
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icon hiển thị với thiết kế đẹp
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(
                                    Color(0xFFF8F9FA),
                                    CircleShape
                                )
                                .clip(CircleShape)
                                .clickable { showIconPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                selectedIcon,
                                fontSize = 42.sp
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            languageViewModel.getTranslation("click_to_change_icon"),
                            color = primaryColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(32.dp))

                    // Form nhập liệu
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Nhập tên danh mục
                        Column {
                            Text(
                                languageViewModel.getTranslation("category_name"),
                                color = textColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = categoryName,
                                onValueChange = { if (it.text.length <= 30) categoryName = it },
                                placeholder = { Text(languageViewModel.getTranslation("category_name_example")) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    focusedLabelColor = primaryColor,
                                    unfocusedLabelColor = subtitleColor,
                                    cursorColor = primaryColor
                                ),
                                singleLine = true,
                                trailingIcon = {
                                    Text(
                                        "${categoryName.text.length}/30",
                                        color = subtitleColor,
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }

                        // Chọn danh mục cha
                        Column {
                            Text(
                                languageViewModel.getTranslation("parent_category"),
                                color = textColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCategoryDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            selectedMainCategory?.name ?: languageViewModel.getTranslation("select_parent_category"),
                                            color = if (selectedMainCategory != null) textColor else subtitleColor,
                                            fontSize = 16.sp,
                                            fontWeight = if (selectedMainCategory != null) FontWeight.Medium else FontWeight.Normal
                                        )
                                        if (selectedMainCategory != null) {
                                            Text(
                                                "${languageViewModel.getTranslation("icon")}: ${selectedMainCategory?.icon}",
                                                color = subtitleColor,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = languageViewModel.getTranslation("select_category"),
                                        tint = subtitleColor
                                    )
                                }
                            }
                        }

                        // Thông báo trạng thái
                        if (categoryName.text.isNotBlank() && selectedMainCategory != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
                                border = BorderStroke(1.dp, Color(0xFFC8E6C9))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color(0xFF4CAF50), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("✓", color = Color.White, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        languageViewModel.getTranslation("ready_to_create_category"),
                                        color = Color(0xFF2E7D32),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // BottomSheet chọn danh mục cha với UI đẹp hơn
    if (showCategoryDialog) {
        CategorySelectionBottomSheet(
            mainCategories = mainCategories,
            selectedMainCategory = selectedMainCategory,
            onCategorySelected = { category ->
                selectedMainCategory = category
                showCategoryDialog = false
            },
            onDismiss = { showCategoryDialog = false },
            primaryColor = primaryColor,
            backgroundColor = backgroundColor,
            languageViewModel = languageViewModel
        )
    }

    // ✅ Icon Picker BottomSheet
    if (showIconPicker) {
        IconPickerBottomSheet(
            selectedIcon = selectedIcon,
            onIconSelected = { icon ->
                selectedIcon = icon
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false },
            primaryColor = primaryColor,
            languageViewModel = languageViewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IconPickerBottomSheet(
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    primaryColor: Color,
    languageViewModel: LanguageViewModel
) {
    val sheetState = rememberModalBottomSheetState()
    val commonIcons = listOf(
        "🍽️", "🛍️", "🚗", "🏠", "💄", "🎮", "🏥", "❤️",
        "🧾", "👨‍👩‍👧‍👦", "📊", "🎓", "💵", "🎁", "📈", "💼",
        "☕", "🍕", "🍔", "🎬", "🎵", "📱", "💻", "✈️"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF5F7FA),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                languageViewModel.getTranslation("select_icon"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D3748),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.height(400.dp)
            ) {
                items(commonIcons.chunked(4)) { rowIcons: List<String> ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowIcons.forEach { icon ->
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(
                                        if (icon == selectedIcon) primaryColor.copy(alpha = 0.2f) else Color.White,
                                        CircleShape
                                    )
                                    .clickable { onIconSelected(icon) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(icon, fontSize = 28.sp)
                            }
                        }
                        // Fill empty spaces
                        repeat(4 - rowIcons.size) {
                            Spacer(modifier = Modifier.width(60.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelectionBottomSheet(
    mainCategories: List<Category>,
    selectedMainCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    onDismiss: () -> Unit,
    primaryColor: Color,
    backgroundColor: Color,
    languageViewModel: LanguageViewModel
) {
    val sheetState = rememberModalBottomSheetState()
    var searchText by remember { mutableStateOf("") }

    val filteredCategories = remember(mainCategories, searchText) {
        if (searchText.isBlank()) {
            mainCategories
        } else {
            mainCategories.filter { category ->
                category.name.contains(searchText, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = backgroundColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        languageViewModel.getTranslation("select_parent_category"),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D3748)
                    )
                    Text(
                        languageViewModel.getTranslation("select_group_for_new_category"),
                        fontSize = 14.sp,
                        color = Color(0xFF718096)
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFF1F5F9), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Thanh tìm kiếm
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text(languageViewModel.getTranslation("search_categories")) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF64748B)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedTextColor = Color(0xFF2D3748),
                    unfocusedTextColor = Color(0xFF2D3748)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Số lượng kết quả
            Text(
                "${languageViewModel.getTranslation("found")} ${filteredCategories.size} ${languageViewModel.getTranslation("categories")}",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Danh sách danh mục
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                if (filteredCategories.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "📭",
                                    fontSize = 48.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    languageViewModel.getTranslation("no_categories_found"),
                                    color = Color(0xFF64748B),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    languageViewModel.getTranslation("try_different_keywords"),
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else {
                    items(filteredCategories) { category ->
                        CategorySelectionItem(
                            category = category,
                            isSelected = selectedMainCategory?.id == category.id,
                            onClick = { onCategorySelected(category) },
                            primaryColor = primaryColor,
                            languageViewModel = languageViewModel
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CategorySelectionItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    languageViewModel: LanguageViewModel
) {
    val backgroundColor = if (isSelected) {
        primaryColor.copy(alpha = 0.1f)
    } else {
        Color.White
    }

    val borderColor = if (isSelected) {
        primaryColor
    } else {
        Color(0xFFF1F5F9)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(Color(0xFFF8F9FA), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(category.icon, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Thông tin danh mục
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = category.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) primaryColor else Color(0xFF2D3748)
                )
                Text(
                    text = if (category.isMainCategory) languageViewModel.getTranslation("main_category") else languageViewModel.getTranslation("sub_category"),
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            // Radio button custom
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        if (isSelected) primaryColor else Color.Transparent,
                        CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = if (isSelected) primaryColor else Color(0xFFCBD5E1),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text(
                        "✓",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}