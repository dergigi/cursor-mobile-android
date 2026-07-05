package com.cursor.mobile.presentation.annotation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.ByteArrayOutputStream

data class DrawingPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnotationScreen(
    imageUri: Uri?,
    onBack: () -> Unit,
    onAnnotated: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var paths by remember { mutableStateOf(listOf<DrawingPath>()) }
    var currentPath by remember { mutableStateOf(listOf<Offset>()) }
    var selectedColor by remember { mutableStateOf(Color.Red) }
    var strokeWidth by remember { mutableStateOf(8f) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imageUri) {
        imageUri?.let { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    bitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Annotate", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (paths.isNotEmpty()) {
                            paths = paths.dropLast(1)
                            currentPath = emptyList()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = {
                        paths = emptyList()
                        currentPath = emptyList()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                    TextButton(onClick = {
                        // Create annotated bitmap
                        val width = bitmap?.width ?: 800
                        val height = bitmap?.height ?: 600
                        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(result)
                        bitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }

                        // Draw annotations
                        val paint = android.graphics.Paint().apply {
                            style = android.graphics.Paint.Style.STROKE
                            this.strokeWidth = strokeWidth * (width.toFloat() / 800f)
                            strokeCap = android.graphics.Paint.Cap.ROUND
                            strokeJoin = android.graphics.Paint.Join.ROUND
                            isAntiAlias = true
                        }

                        paths.forEach { path ->
                            paint.color = android.graphics.Color.argb(
                                255,
                                (path.color.red * 255).toInt(),
                                (path.color.green * 255).toInt(),
                                (path.color.blue * 255).toInt()
                            )
                            paint.strokeWidth = path.strokeWidth * (width.toFloat() / 800f)

                            if (path.points.size >= 2) {
                                val p = android.graphics.Path()
                                p.moveTo(
                                    path.points.first().x * (width.toFloat() / 800f),
                                    path.points.first().y * (height.toFloat() / 600f)
                                )
                                for (i in 1 until path.points.size) {
                                    p.lineTo(
                                        path.points[i].x * (width.toFloat() / 800f),
                                        path.points[i].y * (height.toFloat() / 600f)
                                    )
                                }
                                canvas.drawPath(p, paint)
                            } else if (path.points.size == 1) {
                                canvas.drawCircle(
                                    path.points.first().x * (width.toFloat() / 800f),
                                    path.points.first().y * (height.toFloat() / 600f),
                                    paint.strokeWidth / 2,
                                    paint
                                )
                            }
                        }

                        onAnnotated(result)
                    }) {
                        Text("Done", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // Color picker & stroke width
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Color.Red,
                            Color(0xFFFF9800),
                            Color.Yellow,
                            Color.Green,
                            Color.Cyan,
                            Color.White
                        ).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .pointerInput(Unit) {
                                        detectDragGestures { _, _ -> selectedColor = color }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColor == color) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Size", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = strokeWidth,
                            onValueChange = { strokeWidth = it },
                            valueRange = 2f..20f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath = listOf(offset)
                            },
                            onDrag = { change, _ ->
                                currentPath = currentPath + change.position
                            },
                            onDragEnd = {
                                if (currentPath.isNotEmpty()) {
                                    paths = paths + DrawingPath(
                                        points = currentPath,
                                        color = selectedColor,
                                        strokeWidth = strokeWidth
                                    )
                                }
                                currentPath = emptyList()
                            }
                        )
                    }
            ) {
                // Draw existing paths
                paths.forEach { path ->
                    if (path.points.size >= 2) {
                        for (i in 0 until path.points.size - 1) {
                            drawLine(
                                color = path.color,
                                start = path.points[i],
                                end = path.points[i + 1],
                                strokeWidth = path.strokeWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // Draw current path
                if (currentPath.size >= 2) {
                    for (i in 0 until currentPath.size - 1) {
                        drawLine(
                            color = selectedColor,
                            start = currentPath[i],
                            end = currentPath[i + 1],
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Instruction text
            if (paths.isEmpty() && currentPath.isEmpty()) {
                Text(
                    "Draw on the image to annotate",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
                )
            }
        }
    }
}
