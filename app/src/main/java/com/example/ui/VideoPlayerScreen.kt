package com.example.ui

import android.widget.Toast
import android.widget.VideoView
import android.widget.MediaController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.viewmodel.VaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val item by viewModel.selectedItem.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    if (item == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(VaultDarkCanvas),
            contentAlignment = Alignment.Center
        ) {
            Text("ভিডিও পাওয়া যায়নি।", color = Color.White)
        }
        return
    }

    val videoItem = item!!

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(VaultDarkCanvas),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = videoItem.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(VaultCardSlate)
                    ) {
                        DropdownMenuItem(
                            text = { Text("গ্যালারিতে রিস্টোর করুন", color = Color.White) },
                            leadingIcon = { Icon(Icons.Filled.Send, contentDescription = "Restore", tint = VaultAccentEmerald) },
                            onClick = {
                                showMenu = false
                                viewModel.exportItemFromVault(context, videoItem) { success ->
                                    if (success) {
                                        Toast.makeText(context, "ভিডিওটি রিস্টোর করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                        viewModel.navigateBack()
                                    } else {
                                        Toast.makeText(context, "রিস্টোর ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("স্থায়ীভাবে ডিলিট করুন", color = Color.Red) },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red) },
                            onClick = {
                                showMenu = false
                                viewModel.deleteItemPermanently(videoItem) { success ->
                                    Toast.makeText(context, "ভিডিওটি চিরতরে মুছে ফেলা হয়েছে।", Toast.LENGTH_SHORT).show()
                                    viewModel.navigateBack()
                                }
                            }
                        )
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
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Native video display container with playback bar overlay
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            val mediaController = MediaController(ctx)
                            mediaController.setAnchorView(this)
                            setMediaController(mediaController)
                            setVideoPath(videoItem.path)
                            setOnPreparedListener {
                                start() // Auto play
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "ভিডিওর আকার: ${formatSize(videoItem.size)}",
                color = Color.Gray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
