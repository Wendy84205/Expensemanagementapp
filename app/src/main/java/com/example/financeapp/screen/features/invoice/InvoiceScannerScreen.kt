// screen/features/invoice/InvoiceScannerScreen.kt
package com.example.financeapp.screen.features.invoice

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.financeapp.data.models.Transaction
import com.example.financeapp.viewmodel.invoice.InvoiceScannerViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun InvoiceScannerScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: InvoiceScannerViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera permissions
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Camera setup
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.selectFromGallery(context, it)
        }
    }

    // Bottom sheet state
    val sheetState = rememberModalBottomSheetState()
    var showResultSheet by remember { mutableStateOf(false) }

    // Animation states
    var isFlashOn by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }

    // Check camera permission and show camera if granted
    LaunchedEffect(cameraPermissionState.status) {
        showCamera = cameraPermissionState.status.isGranted
    }

    // Handle scan completion
    LaunchedEffect(state.scannedTransaction) {
        state.scannedTransaction?.let {
            showResultSheet = true
        }
    }

    // Handle navigation to AddTransactionScreen
    LaunchedEffect(state.shouldNavigateToAddTransaction) {
        if (state.shouldNavigateToAddTransaction) {
            state.scannedTransaction?.let { transaction ->
                // Pass data to AddTransactionScreen
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("scanned_transaction", transaction)

                navController.navigate("add_transaction") {
                    popUpTo("invoice_scanner") { inclusive = true }
                }
            }
            viewModel.resetNavigationFlag()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Quét hóa đơn",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Đóng",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showCamera) {
                // Camera Preview
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER

                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                                    val preview = Preview.Builder()
                                        .build()
                                        .also {
                                            it.setSurfaceProvider(surfaceProvider)
                                        }

                                    imageCapture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                        .build()

                                    // Select back camera
                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                    // Unbind use cases before rebinding
                                    cameraProvider.unbindAll()

                                    // Bind use cases to camera
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture
                                    )
                                } catch (exc: Exception) {
                                    Log.e("Camera", "Use case binding failed", exc)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Scanner overlay with animation
                ScannerOverlay()

                // Capture button
                FloatingActionButton(
                    onClick = {
                        viewModel.captureImage(imageCapture, context, cameraExecutor)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp),
                    containerColor = Color(0xFF4CAF50),
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Icon(
                        Icons.Filled.Camera,
                        contentDescription = "Chụp ảnh",
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }
            } else {
                // Camera permission denied view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Outlined.CameraAlt,
                        contentDescription = "Camera",
                        modifier = Modifier.size(80.dp),
                        tint = Color.White.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "Cần quyền truy cập camera",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Ứng dụng cần quyền truy cập camera để quét hóa đơn",
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Cấp quyền camera",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Scanning animation
            if (state.isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Scanning animation
                        CircularProgressIndicator(
                            modifier = Modifier.size(80.dp),
                            strokeWidth = 4.dp,
                            color = Color(0xFF4CAF50),
                            trackColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Đang phân tích hóa đơn...",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                "Vui lòng đợi trong giây lát",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }

                        // Progress steps
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("Xử lý ảnh", "Nhận diện chữ", "Trích xuất thông tin").forEachIndexed { index, step ->
                                StepIndicator(
                                    step = step,
                                    isActive = state.progressStep > index,
                                    isCurrent = state.progressStep == index
                                )
                            }
                        }
                    }
                }
            }

            // Flashlight button
            if (!state.isProcessing && showCamera) {
                FloatingActionButton(
                    onClick = { isFlashOn = !isFlashOn },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .size(48.dp),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(
                        if (isFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = "Đèn flash",
                        tint = if (isFlashOn) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Bottom controls (always visible)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Gallery button
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00BCD4)
                        ),
                        enabled = !state.isProcessing
                    ) {
                        Icon(
                            Icons.Filled.PhotoLibrary,
                            contentDescription = "Thư viện",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Thư viện")
                    }

                    // Test button
                    OutlinedButton(
                        onClick = {
                            viewModel.processTestInvoice()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        enabled = !state.isProcessing
                    ) {
                        Text("Dùng mẫu")
                    }
                }

                // Additional info when camera not available
                if (!showCamera) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Bạn có thể chọn ảnh từ thư viện để quét hóa đơn",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Scan result bottom sheet
            if (showResultSheet && state.scannedTransaction != null) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showResultSheet = false
                        viewModel.resetState()
                    },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    state.scannedTransaction?.let { transaction ->
                        ScanResultBottomSheet(
                            transaction = transaction,
                            confidence = state.confidenceScore,
                            onUseData = {
                                viewModel.confirmUseTransaction()
                                showResultSheet = false
                            },
                            onEdit = {
                                // For now, just use the data
                                viewModel.confirmUseTransaction()
                                showResultSheet = false
                            },
                            onRescan = {
                                showResultSheet = false
                                viewModel.resetState()
                            },
                            onClose = {
                                showResultSheet = false
                                viewModel.resetState()
                            }
                        )
                    }
                }
            }

            // Error dialog
            state.error?.let { error ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearError() },
                    title = {
                        Text(
                            "Quét thất bại",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    text = {
                        Column {
                            Text(error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Vui lòng thử lại với ảnh rõ nét hơn",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Thử lại")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            viewModel.clearError()
                            navController.popBackStack()
                        }) {
                            Text("Hủy")
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

@Composable
fun ScannerOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Dimmed overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )

        // Scanner frame
        Canvas(modifier = Modifier
            .align(Alignment.Center)
            .size(280.dp, 380.dp)
        ) {
            // Draw frame border
            drawRect(
                color = Color.White.copy(alpha = 0.8f),
                topLeft = Offset.Zero,
                size = Size(size.width, size.height),
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw corner indicators
            val cornerLength = 30.dp.toPx()
            val cornerWidth = 4.dp.toPx()
            val cornerColor = Color.White

            // Top-left corner
            drawLine(
                color = cornerColor,
                start = Offset(0f, 0f),
                end = Offset(cornerLength, 0f),
                strokeWidth = cornerWidth
            )
            drawLine(
                color = cornerColor,
                start = Offset(0f, 0f),
                end = Offset(0f, cornerLength),
                strokeWidth = cornerWidth
            )

            // Top-right corner
            drawLine(
                color = cornerColor,
                start = Offset(size.width, 0f),
                end = Offset(size.width - cornerLength, 0f),
                strokeWidth = cornerWidth
            )
            drawLine(
                color = cornerColor,
                start = Offset(size.width, 0f),
                end = Offset(size.width, cornerLength),
                strokeWidth = cornerWidth
            )

            // Bottom-left corner
            drawLine(
                color = cornerColor,
                start = Offset(0f, size.height),
                end = Offset(cornerLength, size.height),
                strokeWidth = cornerWidth
            )
            drawLine(
                color = cornerColor,
                start = Offset(0f, size.height),
                end = Offset(0f, size.height - cornerLength),
                strokeWidth = cornerWidth
            )

            // Bottom-right corner
            drawLine(
                color = cornerColor,
                start = Offset(size.width, size.height),
                end = Offset(size.width - cornerLength, size.height),
                strokeWidth = cornerWidth
            )
            drawLine(
                color = cornerColor,
                start = Offset(size.width, size.height),
                end = Offset(size.width, size.height - cornerLength),
                strokeWidth = cornerWidth
            )
        }

        // Instructions
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Căn chỉnh hóa đơn trong khung",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Đảm bảo đủ sáng và rõ nét",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun StepIndicator(
    step: String,
    isActive: Boolean,
    isCurrent: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Step circle
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = when {
                        isCurrent -> Color(0xFF4CAF50)
                        isActive -> Color(0xFF4CAF50).copy(alpha = 0.5f)
                        else -> Color.White.copy(alpha = 0.2f)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isActive) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = Color.White
                )
            }
        }

        // Step label
        Text(
            step,
            color = when {
                isCurrent -> Color.White
                isActive -> Color.White.copy(alpha = 0.8f)
                else -> Color.White.copy(alpha = 0.4f)
            },
            fontSize = 10.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultBottomSheet(
    transaction: Transaction,
    confidence: Float,
    onUseData: () -> Unit,
    onEdit: () -> Unit,
    onRescan: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Thông tin đã quét",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )

            // Confidence badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            confidence > 0.8f -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                            confidence > 0.6f -> Color(0xFFFF9800).copy(alpha = 0.2f)
                            else -> Color(0xFFF44336).copy(alpha = 0.2f)
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "${(confidence * 100).toInt()}% chính xác",
                    color = when {
                        confidence > 0.8f -> Color(0xFF4CAF50)
                        confidence > 0.6f -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Kiểm tra và chỉnh sửa thông tin nếu cần",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Scanned info cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Merchant info
                InfoRow(
                    icon = Icons.Filled.Storefront,
                    label = "Cửa hàng",
                    value = transaction.title,
                    isHighlighted = true
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Amount
                InfoRow(
                    icon = Icons.Filled.AttachMoney,
                    label = "Số tiền",
                    value = formatCurrency(transaction.amount),
                    isHighlighted = true
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Date
                InfoRow(
                    icon = Icons.Filled.CalendarToday,
                    label = "Ngày",
                    value = transaction.date,
                    isHighlighted = false
                )

                // Category (if detected)
                if (transaction.category.isNotEmpty()) {
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    InfoRow(
                        icon = Icons.Filled.Category,
                        label = "Danh mục",
                        value = transaction.category,
                        isHighlighted = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Primary action - Use this data
            Button(
                onClick = onUseData,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Lưu giao dịch",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Secondary actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Edit button
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chỉnh sửa")
                }

                // Rescan button
                OutlinedButton(
                    onClick = onRescan,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Quét lại")
                }
            }

            // Close button
            TextButton(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Đóng")
            }
        }
    }
}

@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    isHighlighted: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = if (isHighlighted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                value,
                color = if (isHighlighted) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 16.sp,
                fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

// Helper function
fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${formatter.format(amount.toLong())}đ"
}