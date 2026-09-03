package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.Dialog
import com.example.data.SecretNote
import com.example.viewmodel.VaultViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SecretNotesTab(
    notesList: List<SecretNote>,
    viewModel: VaultViewModel,
    onOpenNoteEditor: (SecretNote?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var noteToDelete by remember { mutableStateOf<SecretNote?>(null) }

    val categories = listOf(
        "ALL" to "সব নোট",
        "PASSWORD" to "পাসওয়ার্ড",
        "BANK" to "ব্যাংক ও কার্ড",
        "DIARY" to "ডায়েরি",
        "NOTE" to "সাধারণ"
    )

    val filteredNotes = notesList.filter { note ->
        val matchesCategory = (selectedCategory == "ALL" || note.category == selectedCategory)
        val matchesQuery = searchQuery.isBlank() ||
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.content.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("নোট বা পাসওয়ার্ড খুঁজুন...", color = Color.Gray, fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = VaultAccentEmerald)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = VaultAccentEmerald,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedContainerColor = VaultCardSlate,
                unfocusedContainerColor = VaultCardSlate
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .testTag("secret_notes_search_field")
        )

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { (catKey, catLabel) ->
                val isSelected = selectedCategory == catKey
                val chipColor = if (isSelected) VaultAccentEmerald else VaultCardSlate
                val textColor = if (isSelected) Color.White else Color.LightGray

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(chipColor)
                        .clickable { selectedCategory = catKey }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = catLabel,
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (filteredNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Notes",
                        tint = Color.Gray,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "কোনো মিল পাওয়া যায়নি" else "কোনো গোপন নোট নেই।",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "আপনার ব্যক্তিগত পাসওয়ার্ড, ব্যাংক তথ্য বা ডায়রি লিখে রাখতে নিচের '+' বাটনে চাপুন।",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(filteredNotes, key = { it.id }) { note ->
                    SecretNoteCard(
                        note = note,
                        onNoteClick = { onOpenNoteEditor(note) },
                        onCopyClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(note.title, note.content)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "নোট ক্লিপবোর্ডে কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        },
                        onDeleteClick = { noteToDelete = note }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("নোট মুছে ফেলতে চান?", color = Color.White) },
            text = { Text("'${note.title}' স্থায়ীভাবে মুছে যাবে।", color = Color.LightGray) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSecretNote(note) {
                        Toast.makeText(context, "নোট মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                    noteToDelete = null
                }) {
                    Text("মুছে ফেলুন", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("বাতিল", color = Color.LightGray)
                }
            },
            containerColor = VaultCardSlate
        )
    }
}

@Composable
fun SecretNoteCard(
    note: SecretNote,
    onNoteClick: () -> Unit,
    onCopyClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColor = try {
        Color(android.graphics.Color.parseColor(note.colorHex))
    } catch (e: Exception) {
        VaultCardSlate
    }

    val (badgeText, badgeColor) = when (note.category) {
        "PASSWORD" -> "পাসওয়ার্ড" to Color(0xFFF59E0B)
        "BANK" -> "ব্যাংক ও কার্ড" to Color(0xFF10B981)
        "DIARY" -> "ডায়েরি" to Color(0xFF8B5CF6)
        else -> "নোট" to Color(0xFF3B82F6)
    }

    val formattedDate = remember(note.updatedAt) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(note.updatedAt))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onNoteClick() }
            .testTag("secret_note_card_${note.id}"),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Category Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    color = badgeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = note.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Content snippet
            Text(
                text = note.content,
                color = Color.LightGray.copy(alpha = 0.9f),
                fontSize = 12.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Date & Actions Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formattedDate,
                    color = Color.Gray,
                    fontSize = 10.sp
                )

                Row {
                    IconButton(
                        onClick = onCopyClick,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "Copy Note",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete Note",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NoteEditorDialog(
    note: SecretNote?,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, category: String, colorHex: String) -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var category by remember { mutableStateOf(note?.category ?: "NOTE") }
    var selectedColorHex by remember { mutableStateOf(note?.colorHex ?: "#1B1B22") }

    val categories = listOf(
        "NOTE" to "নোট",
        "PASSWORD" to "পাসওয়ার্ড",
        "BANK" to "ব্যাংক",
        "DIARY" to "ডায়েরি"
    )

    val colorPalette = listOf(
        "#1B1B22", // Slate dark
        "#142926", // Deep Emerald
        "#1B2236", // Deep Blue
        "#2C182A", // Deep Purple
        "#332313"  // Deep Amber
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = VaultDarkCanvas,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = if (note == null) "নতুন গোপন নোট" else "নোট সম্পাদনা",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("নোটের শিরোনাম...", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = VaultAccentEmerald,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedContainerColor = VaultCardSlate,
                        unfocusedContainerColor = VaultCardSlate
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { (catKey, catLabel) ->
                        val isSelected = category == catKey
                        val bg = if (isSelected) VaultAccentEmerald else VaultCardSlate
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .clickable { category = catKey }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = catLabel,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Content Input (multiline)
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("গোপন পাসওয়ার্ড, ব্যাংকিং তথ্য বা ব্যক্তিগত ডায়েরি লিখুন...", color = Color.Gray) },
                    minLines = 6,
                    maxLines = 10,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = VaultAccentEmerald,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedContainerColor = VaultCardSlate,
                        unfocusedContainerColor = VaultCardSlate
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Color Themes selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("রং:", color = Color.Gray, fontSize = 13.sp)
                    colorPalette.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) VaultAccentEmerald else Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("বাতিল", color = Color.LightGray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (content.isNotBlank() || title.isNotBlank()) {
                                onSave(title, content, category, selectedColorHex)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VaultAccentEmerald),
                        enabled = title.isNotBlank() || content.isNotBlank()
                    ) {
                        Text("সংরক্ষণ করুন", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
