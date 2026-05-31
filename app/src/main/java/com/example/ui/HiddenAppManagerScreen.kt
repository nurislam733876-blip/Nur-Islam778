package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AppInfo
import com.example.viewmodel.VaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenAppManagerScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appsList by viewModel.installedApps.collectAsState()
    val isLoading by viewModel.isLoadingApps.collectAsState()
    val searchText by viewModel.appHiderSearchText.collectAsState()
    val hidOnly by viewModel.queryHiddenOnly.collectAsState()

    // Trigger filter checks
    val filteredApps = remember(appsList, searchText, hidOnly) {
        appsList.filter {
            val matchesSearch = it.label.contains(searchText, ignoreCase = true) || 
                                it.packageName.contains(searchText, ignoreCase = true)
            val matchesHidden = !hidOnly || it.isVaulted
            matchesSearch && matchesHidden
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(VaultDarkCanvas),
        topBar = {
            TopAppBar(
                title = { Text("অ্যাপ হাইড সেটিংস", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                .padding(16.dp)
        ) {
            // Android security sandbox disclaimer helper card
            Card(
                colors = CardDefaults.cardColors(containerColor = VaultAccentBlue.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Filled.Info, contentDescription = "info", tint = VaultAccentBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "অ্যাপ সিকিউরিটি গাইড",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "অ্যান্ড্রয়েড সিকিউরিটি কাঠামোর কারণে এখানে বেছে নেওয়া অ্যাপগুলো ভল্টে লক থাকবে। আপনি সরাসরি ভল্ট বা ক্যালকুলেটর ড্যাশবোর্ড থেকে অ্যাপ চালাতে পারবেন।",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Search app bars input
            TextField(
                value = searchText,
                onValueChange = { viewModel.appHiderSearchText.value = it },
                placeholder = { Text("অ্যাপের নাম দিয়ে খুঁজুন...", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search icon", tint = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = VaultCardSlate,
                    unfocusedContainerColor = VaultCardSlate,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .testTag("app_search_field")
            )

            // Hide queries toggle option switches
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "শুধুমাত্র হাইডকৃত অ্যাপস দেখুন",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = hidOnly,
                    onCheckedChange = { viewModel.queryHiddenOnly.value = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VaultAccentEmerald,
                        checkedTrackColor = VaultAccentEmerald.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.testTag("hidden_apps_switch")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main app packages lists
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = VaultAccentEmerald)
                }
            } else if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "কোনো অ্যাপ খুঁজে পাওয়া যায়নি।",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(filteredApps) { app ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (app.isVaulted) VaultAccentEmerald.copy(alpha = 0.08f) else VaultCardSlate
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.toggleAppHideState(app.packageName, app.label)
                                    val logMsg = if (app.isVaulted) "আন-হাইড" else "হাইড/ভল্ট সুরক্ষিত"
                                    Toast.makeText(context, "${app.label} $logMsg করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dynamic placeholder app launcher icon style
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(
                                            if (app.isVaulted) VaultAccentEmerald.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = app.label.firstOrNull()?.toString() ?: "A",
                                        color = if (app.isVaulted) VaultAccentEmerald else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Check box toggle Indicator
                                val tickColor = if (app.isVaulted) VaultAccentEmerald else Color.LightGray.copy(alpha = 0.15f)
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(tickColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (app.isVaulted) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = "Hidden",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
