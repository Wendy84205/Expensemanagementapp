@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.financeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun ImageInputScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    var selectedImages by remember { mutableStateOf<List<String>>(emptyList()) }

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Ghi chép GD",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .padding(padding)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    item {
                        Text(
                            "Thêm giao dịch hàng loạt từ ảnh",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF2D3748),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Chọn tối đa 3 ảnh chụp màn hình Lịch sử hoặc\nKết quả giao dịch ngân hàng, Grab, Shopee...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF718096),
                            lineHeight = 20.sp
                        )
                    }

                    // Image Selection Area
                    item {
                        ImageSelectionArea(
                            selectedImages = selectedImages,
                            onImagesSelected = { images ->
                                selectedImages = images.take(3)
                            },
                            onImageRemove = { image ->
                                selectedImages = selectedImages - image
                            }
                        )
                    }

                    // Transaction History Examples
                    item {
                        TransactionHistorySection()
                    }

                    // Transaction Result Examples
                    item {
                        TransactionResultSection()
                    }

                    // QR Code Examples
                    item {
                        QrCodeSection()
                    }

                    // Action Button
                    item {
                        Button(
                            onClick = {
                                // Xử lý nhận dạng ảnh
                                processImages(selectedImages)
                            },
                            enabled = selectedImages.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF667EEA),
                                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                "XỬ LÝ ẢNH",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageSelectionArea(
    selectedImages: List<String>,
    onImagesSelected: (List<String>) -> Unit,
    onImageRemove: (String) -> Unit
) {
    Column {
        // Selected Images Grid
        if (selectedImages.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                selectedImages.forEachIndexed { index, image ->
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    ) {
                        // Placeholder for selected image
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF7FAFC), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Ảnh ${index + 1}",
                                color = Color(0xFF718096),
                                fontSize = 12.sp
                            )
                        }

                        // Remove button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(20.dp)
                                .background(Color.Red, CircleShape)
                                .clickable { onImageRemove(image) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "×",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Add more button if less than 3
                if (selectedImages.size < 3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp)
                            .border(2.dp, Color(0xFF667EEA), RoundedCornerShape(12.dp))
                            .clickable {
                                // Simulate image selection
                                onImagesSelected(selectedImages + "new_image_${selectedImages.size + 1}")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = "Thêm ảnh",
                                tint = Color(0xFF667EEA),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "Thêm",
                                color = Color(0xFF667EEA),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Main Selection Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clickable {
                    // Simulate image selection
                    if (selectedImages.isEmpty()) {
                        onImagesSelected(listOf("image_1"))
                    }
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC)),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Chọn ảnh ngay",
                        color = Color(0xFF2D3748),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tối đa 3 ảnh",
                        color = Color(0xFF718096),
                        fontSize = 14.sp
                    )
                }

                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = "Chọn ảnh",
                    tint = Color(0xFF667EEA),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun TransactionHistorySection() {
    Column {
        Text(
            "Lịch sử giao dịch",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF2D3748),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Transaction history items
        TransactionHistoryItem("Tích chuyển ra", "-40.000đ", Color(0xFFF56565))
        TransactionHistoryItem("Tích chuyển vào", "+240.000đ", Color(0xFF48BB78))
        TransactionHistoryItem("Tích chuyển ra", "-100.000đ", Color(0xFFF56565))
        TransactionHistoryItem("Tích chuyển ra", "-100.000đ", Color(0xFFF56565))
    }
}

@Composable
private fun TransactionHistoryItem(description: String, amount: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            description,
            color = Color(0xFF2D3748),
            fontSize = 14.sp
        )
        Text(
            amount,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun TransactionResultSection() {
    Column {
        Text(
            "Kết quả giao dịch",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF2D3748),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "-100.000đ",
                    color = Color(0xFFF56565),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Text(
                    "Thành công",
                    color = Color(0xFF48BB78),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Divider(color = Color(0xFFE2E8F0))

                TransactionDetailRow("Mã giao dịch", "XXX-XXX")
                TransactionDetailRow("Người nhận", "ABC")
            }
        }
    }
}

@Composable
private fun TransactionDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = Color(0xFF718096),
            fontSize = 14.sp
        )
        Text(
            value,
            color = Color(0xFF2D3748),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun QrCodeSection() {
    Column {
        Text(
            "QR Code",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF2D3748),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // QR Code Example
            QrCodeExampleCard(
                title = "QR nhận tiền",
                color = Color(0xFF48BB78),
                modifier = Modifier.weight(1f)
            )
            QrCodeExampleCard(
                title = "Ảnh mờ",
                color = Color(0xFFF56565),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QrCodeExampleCard(
    title: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.QrCode,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Hàm xử lý ảnh (placeholder)
private fun processImages(images: List<String>) {
    // TODO: Implement image processing logic
    // Sử dụng ML Kit để nhận dạng văn bản từ ảnh
    println("Processing ${images.size} images...")
}