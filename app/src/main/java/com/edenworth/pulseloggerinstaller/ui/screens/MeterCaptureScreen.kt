package com.edenworth.pulseloggerinstaller.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.CircularProgressIndicator
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.exifinterface.media.ExifInterface
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.Slider
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.LaunchedEffect

@Composable
fun ZoomableImage(
    bitmap: Bitmap,
    onPositionedCallback: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var imageWidth by remember { mutableStateOf(0) }
    var imageHeight by remember { mutableStateOf(0) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // Apply zoom but limit it between 1x and 5x
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    
                    // Apply pan but limit it to prevent too much blank space
                    val maxX = (imageWidth * scale - imageWidth) / 2f
                    val maxY = (imageHeight * scale - imageHeight) / 2f
                    
                    if (maxX > 0) {
                        offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                    }
                    if (maxY > 0) {
                        offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                    }
                }
            }
            .pointerInput(Unit) {
                // Double tap to reset zoom
                detectTapGestures(
                    onDoubleTap = {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    }
                )
            }
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
                .onGloballyPositioned { coordinates ->
                    imageWidth = coordinates.size.width
                    imageHeight = coordinates.size.height
                    onPositionedCallback(imageWidth, imageHeight)
                }
        )
    }
}

@Composable
fun MeterCaptureScreen() {
    val context = LocalContext.current
    var meterId by remember { mutableStateOf("") }
    var meterReading by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var cropLeft by remember { mutableStateOf(0.25f) }
    var cropTop by remember { mutableStateOf(0.25f) }
    var cropWidth by remember { mutableStateOf(0.5f) }
    var cropHeight by remember { mutableStateOf(0.2f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var selectedField by remember { mutableStateOf(-1) } // -1: None, 0: Meter ID, 1: Meter Reading
    var lastOcrResult by remember { mutableStateOf("") }
    var imageWidthPx by remember { mutableStateOf(1) }
    var imageHeightPx by remember { mutableStateOf(1) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = photoUri
        if (success && uri != null) {
            try {
                bitmap = getOrientedBitmap(context, uri)
                errorMessage = ""
                selectedField = -1
                meterId = ""
                meterReading = ""
            } catch (e: Exception) {
                errorMessage = "Failed to load image: ${e.localizedMessage}"
                bitmap = null
            }
        } else if (!success) {
            errorMessage = "Photo capture failed."
        }
    }

    // Reset offset when field is selected
    LaunchedEffect(selectedField) {
        if (selectedField >= 0 && imageWidthPx > 0 && imageHeightPx > 0) {
            offsetX = imageWidthPx / 2f - 57.5.dp.toPx(context) / 2f
            offsetY = imageHeightPx / 2f - 17.5.dp.toPx(context) / 2f
            Log.d("MeterCapture", "Box reset to position: $offsetX, $offsetY in image of $imageWidthPx x $imageHeightPx")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Capture Meter Details", modifier = Modifier.padding(bottom = 16.dp))
        Button(onClick = {
            val uri = createImageUri(context)
            photoUri = uri
            takePictureLauncher.launch(uri)
        }) {
            Text("Take Photo of Meter Face")
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        // Field selection row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { selectedField = 0 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedField == 0) Color(0xFF6200EE) else Color.Gray
                )
            ) {
                Text("Select Meter ID")
            }
            Button(
                onClick = { selectedField = 1 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedField == 1) Color(0xFF6200EE) else Color.Gray
                )
            ) {
                Text("Select Reading")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Fields with individual OCR buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = meterId,
                onValueChange = { meterId = it },
                label = { Text("Meter ID") },
                enabled = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { 
                    if (bitmap != null) {
                        selectedField = 0 
                        offsetX = imageWidthPx / 2f - 57.5.dp.toPx(context) / 2f
                        offsetY = imageHeightPx / 2f - 17.5.dp.toPx(context) / 2f
                    }
                },
                enabled = bitmap != null
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                    contentDescription = "Select Region for Meter ID"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = meterReading,
                onValueChange = { meterReading = it },
                label = { Text("Meter Reading") },
                enabled = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { 
                    if (bitmap != null) {
                        selectedField = 1 
                        offsetX = imageWidthPx / 2f - 57.5.dp.toPx(context) / 2f
                        offsetY = imageHeightPx / 2f - 17.5.dp.toPx(context) / 2f
                    }
                },
                enabled = bitmap != null
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                    contentDescription = "Select Region for Meter Reading"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (bitmap != null) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)) {
                
                ZoomableImage(
                    bitmap = bitmap!!,
                    contentDescription = "Meter Photo",
                    modifier = Modifier.matchParentSize(),
                    onPositionedCallback = { width, height ->
                        imageWidthPx = width
                        imageHeightPx = height
                        // Ensure offset is initialized properly
                        if (offsetX == 0f && offsetY == 0f) {
                            offsetX = width / 2f - 57.5.dp.toPx(context) / 2f
                            offsetY = height / 2f - 17.5.dp.toPx(context) / 2f
                            Log.d("MeterCapture", "Initial box position: $offsetX, $offsetY in image of $width x $height")
                        }
                    }
                )
                
                // Only show the crop rectangle if a field is selected
                if (selectedField >= 0) {
                    // Draw crop rectangle overlay with both fill and stroke
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val boxW = 115.dp.toPx(context)
                        val boxH = 35.dp.toPx(context)
                        
                        // Semi-transparent fill
                        drawRect(
                            color = if (selectedField == 0) 
                                Color.Blue.copy(alpha = 0.2f) 
                            else 
                                Color.Green.copy(alpha = 0.2f),
                            topLeft = Offset(offsetX, offsetY),
                            size = Size(boxW, boxH),
                        )
                        
                        // Bold stroke
                        drawRect(
                            color = if (selectedField == 0) Color.Blue else Color.Green,
                            topLeft = Offset(offsetX, offsetY),
                            size = Size(boxW, boxH),
                            style = Stroke(width = 6f)
                        )
                        
                        Log.d("MeterCapture", "Drawing box at: $offsetX, $offsetY with size $boxW x $boxH")
                    }
                    
                    // Draggable overlay with visible border for debugging
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                            .size(115.dp, 35.dp)
                            .border(2.dp, Color.Red)  // Red border to help debug
                            .draggable(
                                orientation = androidx.compose.foundation.gestures.Orientation.Horizontal,
                                state = rememberDraggableState { delta ->
                                    offsetX = (offsetX + delta).coerceIn(0f, (imageWidthPx - 115.dp.toPx(context)))
                                    Log.d("MeterCapture", "Dragged horizontally to: $offsetX, $offsetY")
                                }
                            )
                            .draggable(
                                orientation = androidx.compose.foundation.gestures.Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    offsetY = (offsetY + delta).coerceIn(0f, (imageHeightPx - 35.dp.toPx(context)))
                                    Log.d("MeterCapture", "Dragged vertically to: $offsetX, $offsetY")
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Add a visible element inside for testing
                        Text(
                            text = if (selectedField == 0) "ID" else "Reading",
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(2.dp)
                        )
                    }
                }
                
                // Add information text about zoom
                Text(
                    text = "Pinch to zoom, double-tap to reset",
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Only show OCR button if a field is selected
            if (selectedField >= 0) {
                Button(
                    onClick = {
                        val bmp = bitmap!!
                        // Calculate scale factors
                        val scaleX = bmp.width.toFloat() / imageWidthPx
                        val scaleY = bmp.height.toFloat() / imageHeightPx
                        val boxW = 115.dp.toPx(context)
                        val boxH = 35.dp.toPx(context)
                        // Map UI crop box to bitmap coordinates
                        val cropLeftPx = (offsetX * scaleX).toInt().coerceIn(0, bmp.width - 1)
                        val cropTopPx = (offsetY * scaleY).toInt().coerceIn(0, bmp.height - 1)
                        val cropWidthPx = (boxW * scaleX).toInt().coerceAtMost(bmp.width - cropLeftPx)
                        val cropHeightPx = (boxH * scaleY).toInt().coerceAtMost(bmp.height - cropTopPx)
                        val cropRect = android.graphics.Rect(
                            cropLeftPx,
                            cropTopPx,
                            cropLeftPx + cropWidthPx,
                            cropTopPx + cropHeightPx
                        )
                        val croppedBmp = Bitmap.createBitmap(bmp, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
                        val enhancedBmp = enhanceBitmapContrastGrayscale(croppedBmp)
                        val image = InputImage.fromBitmap(enhancedBmp, 0)
                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        scope.launch {
                            isProcessing = true
                            try {
                                val result = recognizer.process(image).await()
                                val text = result.text
                                lastOcrResult = text
                                val regex = Regex("\\d{3,}")
                                val numbers = regex.findAll(text).map { it.value }.toList()
                                val largest = numbers.maxByOrNull { it.length } ?: ""
                                val extractedText = if (largest.isNotBlank()) largest else text
                                
                                // Update only the selected field
                                if (selectedField == 0) {
                                    meterId = extractedText
                                } else {
                                    meterReading = extractedText
                                }
                                errorMessage = ""
                            } catch (e: Exception) {
                                errorMessage = "OCR failed: ${e.localizedMessage}"
                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    enabled = !isProcessing
                ) {
                    Text("Read ${if (selectedField == 0) "Meter ID" else "Meter Reading"}")
                }
            }
            
            if (lastOcrResult.isNotBlank()) {
                Text("Last OCR Result: $lastOcrResult", color = Color.Gray)
            }
            
            if (isProcessing) {
                CircularProgressIndicator()
                Text("Processing...")
            }
        } else {
            Text("No image captured yet.")
        }
        
        if (errorMessage.isNotBlank()) {
            Text("Error: $errorMessage", color = Color.Red)
        }
    }
}

private fun createImageUri(context: Context): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_${timeStamp}_"
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val image = File.createTempFile(imageFileName, ".jpg", storageDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", image)
}

private fun isMeterImage(text: String): Boolean {
    val meterKeywords = listOf("meter", "dial", "reading", "kWh", "electricity")
    return meterKeywords.any { keyword -> text.contains(keyword, ignoreCase = true) }
}

fun getOrientedBitmap(context: Context, uri: Uri): Bitmap? {
    val inputStream = context.contentResolver.openInputStream(uri)
    val bitmap = BitmapFactory.decodeStream(inputStream)
    inputStream?.close()
    val exifStream = context.contentResolver.openInputStream(uri)
    val exif = exifStream?.let { ExifInterface(it) }
    val orientation = exif?.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    ) ?: ExifInterface.ORIENTATION_NORMAL
    exifStream?.close()
    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> bitmap?.let { rotateBitmap(it, 90f) }
        ExifInterface.ORIENTATION_ROTATE_180 -> bitmap?.let { rotateBitmap(it, 180f) }
        ExifInterface.ORIENTATION_ROTATE_270 -> bitmap?.let { rotateBitmap(it, 270f) }
        else -> bitmap
    }
}

fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = android.graphics.Matrix()
    matrix.postRotate(degrees)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

fun enhanceBitmapContrastGrayscale(src: Bitmap): Bitmap {
    val width = src.width
    val height = src.height
    val bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmpGrayscale)
    val paint = android.graphics.Paint()
    val colorMatrix = android.graphics.ColorMatrix()
    colorMatrix.setSaturation(0f) // Grayscale
    // Increase contrast
    val contrast = 2.0f // 1.0 = no change
    val scale = contrast
    val translate = (-0.5f * scale + 0.5f) * 255f
    val contrastMatrix = android.graphics.ColorMatrix(
        floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
    )
    colorMatrix.postConcat(contrastMatrix)
    paint.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(src, 0f, 0f, paint)
    return bmpGrayscale
}

// Helper extension to convert dp to px
fun Dp.toPx(context: Context): Float =
    this.value * context.resources.displayMetrics.density 