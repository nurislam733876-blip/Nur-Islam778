package com.example.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.VaultedItem
import com.example.viewmodel.AppInfo
import com.example.viewmodel.VaultScreen
import com.example.viewmodel.VaultViewModel
import java.io.File

// Vault Dashboard Dark Luxury Palette
val VaultDarkCanvas = Color(0xFF0F0F14)
val VaultCardSlate = Color(0xFF1B1B22)
val VaultAccentEmerald = Color(0xFF10B981)
val VaultAccentBlue = Color(0xFF3B82F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDashboardScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0=Photos, 1=Videos, 2=Files, 3=Apps
    
    val photos by viewModel.photosList.collectAsState()
    val videos by viewModel.videosList.collectAsState()
    val files by viewModel.filesList.collectAsState()
    val hiddenApps by viewModel.hiddenAppsList.collectAsState()
    
    // Pickers launchers
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileNameFromUri(context, uri)
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val typeString = when {
                mimeType.contains("image") -> "PHOTO"
                mimeType.contains("video") -> "VIDEO"
                else -> "FILE"
            }
            
            viewModel.importSelectedFile(context, uri, fileName, typeString) { success ->
                if (success) {
                    Toast.makeText(context, "ফাইল ভল্টে সুরক্ষিত করা হয়েছে!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "ফাইল যোগ করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(VaultDarkCanvas),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = "Lock Logo",
                            tint = VaultAccentEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "My Secret Vault",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.lockVault() },
                        modifier = Modifier.testTag("lock_action_button")
                    ) {
                        Icon(
                            Icons.Filled.ExitToApp,
                            contentDescription = "Lock instantly",
                            tint = Color.Red
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VaultDarkCanvas,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (selectedTab < 3) {
                FloatingActionButton(
                    onClick = {
                        val pickType = when (selectedTab) {
                            0 -> "image/*"
                            1 -> "video/*"
                            else -> "*/*"
                        }
                        filePickerLauncher.launch(pickType)
                    },
                    containerColor = VaultAccentEmerald,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_item_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Item")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(VaultDarkCanvas)
        ) {
            // Category Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val tabTitles = listOf("ছবি", "ভিডিও", "ফাইল", "অ্যাপস")
                val tabIcons = listOf(
                    Icons.Filled.Star,
                    Icons.Filled.PlayArrow,
                    Icons.Filled.Menu,
                    Icons.Filled.Settings
                )
                
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    val activeBrush = if (isSelected) VaultAccentEmerald else Color.Transparent
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(activeBrush)
                            .clickable { selectedTab = index }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = tabIcons[index],
                            contentDescription = title,
                            tint = if (isSelected) Color.White else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Tabs Content View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                when (selectedTab) {
                    0 -> PhotosGrid(photosList = photos, onPhotoClicked = { photo ->
                        viewModel.selectedItem.value = photo
                        viewModel.setupPhotoEditor(photo)
                        viewModel.navigateTo(VaultScreen.PhotoViewer)
                    })
                    1 -> VideosGrid(videosList = videos, onVideoClicked = { video ->
                        viewModel.selectedItem.value = video
                        viewModel.navigateTo(VaultScreen.VideoPlayer)
                    })
                    2 -> FilesList(filesList = files, context = context, viewModel = viewModel)
                    3 -> AppsDashboardTab(hiddenAppsList = hiddenApps, viewModel = viewModel, context = context)
                }
            }
        }
    }
}

@Composable
fun PhotosGrid(
    photosList: List<VaultedItem>,
    onPhotoClicked: (VaultedItem) -> Unit
) {
    if (photosList.isEmpty()) {
        EmptyStateView(
            message = "কোনো গোপন ছবি মেলেনি। নতুন ছবি সিকিউর করতে নিচের '+' বাটনে ক্লিক করুন।",
            icon = Icons.Filled.Star
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            items(photosList) { photo ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(VaultCardSlate)
                        .clickable { onPhotoClicked(photo) }
                ) {
                    AsyncImage(
                        model = File(photo.path),
                        contentDescription = photo.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun VideosGrid(
    videosList: List<VaultedItem>,
    onVideoClicked: (VaultedItem) -> Unit
) {
    if (videosList.isEmpty()) {
        EmptyStateView(
            message = "কোনো গোপন ভিডিও মেলেনি। ভিডিও লুকাতে নিচের '+' বাটনে ক্লিক করুন।",
            icon = Icons.Filled.PlayArrow
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            items(videosList) { video ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1.4f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(VaultCardSlate)
                        .clickable { onVideoClicked(video) }
                ) {
                    // Try to generate thumbnail or show video card
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "Play Video",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(6.dp)
                        ) {
                            Text(
                                text = video.name,
                                color = Color.White,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilesList(
    filesList: List<VaultedItem>,
    context: Context,
    viewModel: VaultViewModel
) {
    if (filesList.isEmpty()) {
        EmptyStateView(
            message = "কোনো গোপন ফাইল বা ডকুমেন্ট নেই। আপনার গুরুত্বপূর্ণ ফাইল লুকাতে নিচের '+' বাটনে চাপুন।",
            icon = Icons.Filled.Menu
        )
    } else {
        var itemMenuExpandedState by remember { mutableStateOf<VaultedItem?>(null) }
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            items(filesList) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = VaultCardSlate),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                Toast.makeText(context, "${item.name} সুরক্ষিত অবস্থায় রয়েছে।", Toast.LENGTH_SHORT).show()
                            },
                            onLongClick = {
                                itemMenuExpandedState = item
                            }
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = "File icon",
                            tint = VaultAccentBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatSize(item.size),
                                color = Color.LightGray.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = { itemMenuExpandedState = item }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = Color.LightGray)
                        }
                    }
                }
            }
        }

        // Context dialog for Files
        if (itemMenuExpandedState != null) {
            val fileItem = itemMenuExpandedState!!
            AlertDialog(
                onDismissRequest = { itemMenuExpandedState = null },
                title = { Text("ফাইল অপশন", color = Color.White) },
                text = { Text("আপনি কি এই ফাইলটি রিস্টোর বা ডিলিট করতে চান?", color = Color.White) },
                icon = { Icon(Icons.Filled.Build, contentDescription = "Options", tint = VaultAccentEmerald) },
                containerColor = VaultCardSlate,
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.exportItemFromVault(context, fileItem) { success ->
                                itemMenuExpandedState = null
                                if (success) {
                                    Toast.makeText(context, "ফাইল রিস্টোর ও গ্যালারিতে পাঠানো হয়েছে!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "রিস্টোর ব্যর্থ হয়েছে।", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text("রিস্টোর করুন (Gallery)", color = VaultAccentEmerald)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteItemPermanently(fileItem) { success ->
                                itemMenuExpandedState = null
                                Toast.makeText(context, "ফাইল চিরতরে মুছে ফেলা হয়েছে।", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("ডিলিট করুন", color = Color.Red)
                    }
                }
            )
        }
    }
}

@Composable
fun AppsDashboardTab(
    hiddenAppsList: List<com.example.data.HiddenApp>,
    viewModel: VaultViewModel,
    context: Context
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                viewModel.loadApps(context)
                viewModel.navigateTo(VaultScreen.HiddenAppManager)
            },
            colors = ButtonDefaults.buttonColors(containerColor = VaultAccentEmerald),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("manage_apps_button")
        ) {
            Icon(Icons.Filled.Settings, contentDescription = "Android")
            Spacer(modifier = Modifier.width(8.dp))
            Text("অ্যাপ হাইড সেটআপ করুন", fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (hiddenAppsList.isEmpty()) {
            EmptyStateView(
                message = "কোনো সুরক্ষিত অ্যাপ তালিকা নেই। উপরের বাটন ক্লিক করে মোবাইল থেকে অ্যাপস ভল্টে যোগ বা হাইড করুন।",
                icon = Icons.Filled.Lock
            )
        } else {
            Text(
                text = "বর্তমানে ভল্টকৃত অ্যাপস (${hiddenAppsList.size})",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(hiddenAppsList) { app ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = VaultCardSlate),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                    if (launchIntent != null) {
                                        context.startActivity(launchIntent)
                                    } else {
                                        Toast.makeText(context, "অ্যাপটি সরাসরি ওপেন করা যাচ্ছে না।", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "অ্যাপ চালু করতে ব্যর্থ।", Toast.LENGTH_SHORT).show()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(VaultAccentEmerald.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = app.label.firstOrNull()?.toString() ?: "A",
                                    color = VaultAccentEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.label,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = app.packageName,
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            // Unhide Button
                            IconButton(onClick = {
                                viewModel.toggleAppHideState(app.packageName, app.label)
                                Toast.makeText(context, "${app.label} আন-হাইড করা হয়েছে।", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Filled.Check, contentDescription = "Hidden", tint = VaultAccentEmerald)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Empty icon",
            tint = Color.LightGray.copy(alpha = 0.15f),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = Color.LightGray.copy(alpha = 0.5f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
    }
}

// Helper utilities
fun getFileNameFromUri(context: Context, uri: Uri): String {
    var name = "sec_file"
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return name
}

fun formatSize(sizeInBytes: Long): String {
    if (sizeInBytes < 1024) return "$sizeInBytes B"
    val exp = (Math.log(sizeInBytes.toDouble()) / Math.log(1024.0)).toInt()
    val suffix = "KMGTPE"[exp - 1] + "B"
    return String.format("%.2f %s", sizeInBytes / Math.pow(1024.0, exp.toDouble()), suffix)
}
