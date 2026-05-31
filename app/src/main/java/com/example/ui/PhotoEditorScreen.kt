package com.example.ui

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.DrawPathPoint
import com.example.viewmodel.DrawStroke
import com.example.viewmodel.VaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val item by viewModel.selectedItem.collectAsState()
    val editedBitmap by viewModel.editedPhotoBitmapState.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val brushColor by viewModel.brushColor.collectAsState()
    val brushSize by viewModel.brushWidth.collectAsState()
    val strokesList by viewModel.drawStrokesList.collectAsState()
    val isSaving by viewModel.isSavingEdited.collectAsState()

    var containerWidth by remember { mutableStateOf(1f) }
    var containerHeight by remember { mutableStateOf(1f) }
    val activeStrokesList = remember { mutableStateListOf<DrawPathPoint>() }

    if (editedBitmap == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(VaultDarkCanvas),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = VaultAccentEmerald)
        }
        return
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(VaultDarkCanvas),
        topBar = {
            TopAppBar(
                title = { Text("ফটো এডিটর", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = VaultAccentEmerald,
                            modifier = Modifier.size(24.dp).padding(end = 12.dp)
                        )
                    } else {
                        IconButton(
                            onClick = {
                                viewModel.saveEditedImg(context) { success ->
                                    if (success) {
                                        Toast.makeText(context, "ছবি এডিট সফল ভাবে সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show()
                                        viewModel.navigateBack()
                                    } else {
                                        Toast.makeText(context, "সংরক্ষণ ব্যর্থ হয়েছে।", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.testTag("save_edit_button")
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = "Save", tint = VaultAccentEmerald)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VaultDarkCanvas,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(VaultDarkCanvas)
                .padding(12.dp)
        ) {
            // Main Canvas & Image editing viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .onGloballyPositioned { coordinates ->
                        containerWidth = coordinates.size.width.toFloat()
                        containerHeight = coordinates.size.height.toFloat()
                    }
                    .pointerInput(editedBitmap) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val bitmap = editedBitmap ?: return@detectDragGestures
                                activeStrokesList.clear()
                                // scale coordinate to bitmap resolution
                                val bx = (offset.x / containerWidth) * bitmap.width
                                val by = (offset.y / containerHeight) * bitmap.height
                                activeStrokesList.add(DrawPathPoint(bx, by))
                            },
                            onDrag = { change, _ ->
                                val bitmap = editedBitmap ?: return@detectDragGestures
                                change.consume()
                                val position = change.position
                                val bx = (position.x / containerWidth) * bitmap.width
                                val by = (position.y / containerHeight) * bitmap.height
                                activeStrokesList.add(DrawPathPoint(bx, by))
                            },
                            onDragEnd = {
                                if (activeStrokesList.isNotEmpty()) {
                                    viewModel.addDrawStroke(
                                        DrawStroke(
                                            points = activeStrokesList.toList(),
                                            color = brushColor,
                                            brushWidth = brushSize
                                        )
                                    )
                                    activeStrokesList.clear()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Background actual edited image
                Image(
                    bitmap = editedBitmap!!.asImageBitmap(),
                    contentDescription = "Editing image",
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay active live stroke (before finger is lifted)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (activeStrokesList.size >= 2) {
                        val path = Path()
                        // map back to view coordinates to display smoothly on overlay
                        val startScreenX = (activeStrokesList[0].x / editedBitmap!!.width) * size.width
                        val startScreenY = (activeStrokesList[0].y / editedBitmap!!.height) * size.height
                        path.moveTo(startScreenX, startScreenY)
                        for (i in 1 until activeStrokesList.size) {
                            val px = (activeStrokesList[i].x / editedBitmap!!.width) * size.width
                            val py = (activeStrokesList[i].y / editedBitmap!!.height) * size.height
                            path.lineTo(px, py)
                        }
                        drawPath(
                            path = path,
                            color = Color(brushColor),
                            style = Stroke(
                                width = brushSize * (size.width / editedBitmap!!.width),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Toolbar (Rotate, Undo)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ব্রাশ কালার ও সাইজ চুজ করুন:",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Custom aesthetic Text-based Undo Button to avoid Extended Icons bloat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (strokesList.isNotEmpty()) VaultAccentEmerald else Color.DarkGray)
                            .clickable(enabled = strokesList.isNotEmpty()) { viewModel.undoLastDrawStroke() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "UNDO",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = { viewModel.rotate90Degrees() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Rotate 90 degrees", tint = Color.White)
                    }
                }
            }

            // Brush color Picker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val colors = listOf(
                    android.graphics.Color.RED to Color.Red,
                    android.graphics.Color.GREEN to Color.Green,
                    android.graphics.Color.BLUE to Color.Blue,
                    android.graphics.Color.YELLOW to Color.Yellow,
                    android.graphics.Color.WHITE to Color.White,
                    android.graphics.Color.BLACK to Color.Black
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { (colorVal, colorCompose) ->
                        val isSelected = brushColor == colorVal
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(colorCompose)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) VaultAccentEmerald else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.brushColor.value = colorVal }
                        )
                    }
                }

                // Slider for brush size
                Slider(
                    value = brushSize,
                    onValueChange = { viewModel.brushWidth.value = it },
                    valueRange = 4f..40f,
                    colors = SliderDefaults.colors(
                        thumbColor = VaultAccentEmerald,
                        activeTrackColor = VaultAccentEmerald
                    ),
                    modifier = Modifier
                        .width(130.dp)
                        .padding(horizontal = 4.dp)
                )
            }

            Text(
                text = "ফিল্টারসমূহ প্রয়োগ করুন:",
                color = Color.LightGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Dynamic Filters Selector Row
            val filters = listOf("None", "Grayscale", "Sepia", "Vintage", "Warm", "Cool")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                items(filters) { filter ->
                    val isSelected = activeFilter == filter
                    val chipBg = if (isSelected) VaultAccentEmerald else VaultCardSlate
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(chipBg)
                            .clickable { viewModel.applyFilter(filter) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filterNameTranslate(filter),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun filterNameTranslate(englishName: String): String {
    return when (englishName) {
        "None" -> "মূল ছবি"
        "Grayscale" -> "সাদা-কালো"
        "Sepia" -> "সেপিয়া"
        "Vintage" -> "ভিন্টেজ"
        "Warm" -> "ভিভিড ওয়ার্ম"
        "Cool" -> "কুল ব্লু"
        else -> englishName
    }
}
