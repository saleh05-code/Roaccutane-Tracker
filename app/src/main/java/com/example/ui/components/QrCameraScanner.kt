package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

@Composable
fun QrCameraScannerDialog(
    onDismissRequest: () -> Unit,
    onQrScanned: (String) -> Unit,
    titleText: String = "مسح رمز الطبيب (QR)",
    hintText: String = "قم بتوجيه الكاميرا مباشرة نحو كود الـ QR الخاص بالطبيب للربط التلقائي"
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            titleText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        CameraPreviewWithScanner(
                            onQrCodeScanned = { rawCode ->
                                val clean = if (rawCode.startsWith("DOC_PAIR:")) {
                                    val parts = rawCode.split(":")
                                    if (parts.size >= 2) parts[1] else rawCode
                                } else {
                                    rawCode
                                }
                                onQrScanned(clean.trim())
                                onDismissRequest()
                            }
                        )

                        // Target Box Overlay
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        )

                        // Animated scanning line
                        val infiniteTransition = rememberInfiniteTransition(label = "scan_line")
                        val scanLineOffset by infiniteTransition.animateFloat(
                            initialValue = -100f,
                            targetValue = 100f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 1800, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scan_offset"
                        )

                        Box(
                            modifier = Modifier
                                .width(200.dp)
                                .height(2.dp)
                                .offset(y = scanLineOffset.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        hintText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "إذن الكاميرا مطلوب",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            "يحتاج التطبيق إلى إذن الوصول للكاميرا لمسح كود الربط الخاص بالطبيب.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { launcher.launch(Manifest.permission.CAMERA) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("منح إذن الكاميرا")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إلغاء أو إدخال الكود يدوياً")
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewWithScanner(
    onQrCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor, QrAnalyzer(onQrCodeScanned))

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    android.util.Log.e("QrScanner", "Use case binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

private class QrAnalyzer(
    private val onScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to java.lang.Boolean.TRUE,
                DecodeHintType.CHARACTER_SET to "UTF-8"
            )
        )
    }
    @Volatile
    private var hasFoundCode = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (hasFoundCode) {
            imageProxy.close()
            return
        }

        try {
            val bitmap = imageProxy.toBitmap()
            val rotation = imageProxy.imageInfo.rotationDegrees
            val rotatedBitmap = if (rotation != 0) {
                val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }

            val intArray = IntArray(rotatedBitmap.width * rotatedBitmap.height)
            rotatedBitmap.getPixels(intArray, 0, rotatedBitmap.width, 0, 0, rotatedBitmap.width, rotatedBitmap.height)
            val source = com.google.zxing.RGBLuminanceSource(rotatedBitmap.width, rotatedBitmap.height, intArray)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            val result = reader.decodeWithState(binaryBitmap)
            if (result != null && result.text.isNotBlank()) {
                hasFoundCode = true
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onScanned(result.text)
                }
            }
        } catch (_: Exception) {
            // Frame does not contain valid QR code, fallback to raw buffer check
            try {
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val buffer = mediaImage.planes[0].buffer
                    val data = ByteArray(buffer.remaining())
                    buffer.get(data)
                    val width = imageProxy.width
                    val height = imageProxy.height
                    val source = PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false)
                    val bBitmap = BinaryBitmap(HybridBinarizer(source))
                    val res = reader.decodeWithState(bBitmap)
                    if (res != null && res.text.isNotBlank()) {
                        hasFoundCode = true
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            onScanned(res.text)
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore frame
            }
        } finally {
            reader.reset()
            imageProxy.close()
        }
    }
}
