package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.viewmodel.VaultViewModel

@Composable
fun SecuritySettingsDialog(
    viewModel: VaultViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDecoy by viewModel.isDecoyMode.collectAsState()
    val currentDecoyPass by viewModel.decoyPasscode.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = Decoy Passcode, 1 = Change Master PIN
    var newDecoyPin by remember { mutableStateOf("") }
    var newMasterPin by remember { mutableStateOf("") }
    var confirmMasterPin by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = VaultDarkCanvas,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "Security",
                        tint = VaultAccentEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "সিকিউরিটি ও প্রাইভেসি সেটিংস",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode status banner
                if (isDecoy) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE11D48).copy(alpha = 0.2f))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = "Decoy Active", tint = Color(0xFFE11D48), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ডিকয় (নকল) মোড বর্তমানে চালু আছে। আসল ফাইল সুরক্ষিত রাখা হয়েছে।",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Sub-tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(VaultCardSlate)
                        .padding(4.dp)
                ) {
                    val tabs = listOf("ডিকয় (নকল) পিন", "মূল পাসকোড")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = activeTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) VaultAccentEmerald else Color.Transparent)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (activeTab == 0) {
                    // Decoy PIN View
                    Text(
                        text = "ডিকয় (নকল) ভল্ট কী?",
                        color = VaultAccentEmerald,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "কেউ যদি জোর করে ক্যালকুলেটরের পাসকোড জানতে চায়, তবে এই ডিকয় পিন দিন। ক্যালকুলেটরে এই পিন চাপলে সম্পূর্ণ খালি একটি নকল ভল্ট খুলবে, আপনার আসল ছবি ও ফাইল লুকানোই থাকবে!",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!currentDecoyPass.isNullOrEmpty()) {
                        Text(
                            text = "বর্তমান ডিকয় পিন: $currentDecoyPass",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = newDecoyPin,
                        onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) newDecoyPin = it },
                        placeholder = { Text("নতুন ৪ সংখ্যার ডিকয় পিন (যেমন: 0000)", color = Color.Gray, fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = VaultAccentEmerald,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = VaultCardSlate,
                            unfocusedContainerColor = VaultCardSlate
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("decoy_pin_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (newDecoyPin.length in 4..6) {
                                viewModel.setDecoyPasscode(newDecoyPin) {
                                    Toast.makeText(context, "ডিকয় পাসকোড সফলভাবে সেট হয়েছে!", Toast.LENGTH_LONG).show()
                                    onDismiss()
                                }
                            } else {
                                Toast.makeText(context, "ডিকয় পিন অন্তত ৪ সংখ্যার হতে হবে", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VaultAccentEmerald),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_decoy_pin_button")
                    ) {
                        Text("ডিকয় পাসকোড সেভ করুন", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Change Main PIN
                    Text(
                        text = "ক্যালকুলেটরের আসল পাসকোড পরিবর্তন করুন",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newMasterPin,
                        onValueChange = { if (it.length <= 8 && it.all { char -> char.isDigit() }) newMasterPin = it },
                        placeholder = { Text("নতুন ৪-৮ সংখ্যার পাসকোড", color = Color.Gray, fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
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

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = confirmMasterPin,
                        onValueChange = { if (it.length <= 8 && it.all { char -> char.isDigit() }) confirmMasterPin = it },
                        placeholder = { Text("পাসকোডটি পুনরায় লিখুন", color = Color.Gray, fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (newMasterPin.length in 4..8) {
                                if (newMasterPin == confirmMasterPin) {
                                    viewModel.updateMainPasscode(newMasterPin) {
                                        Toast.makeText(context, "মূল পাসকোড সফলভাবে পরিবর্তন হয়েছে!", Toast.LENGTH_LONG).show()
                                        onDismiss()
                                    }
                                } else {
                                    Toast.makeText(context, "দুটি পাসকোড মেলেনি!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "পাসকোড ৪ থেকে ৮ সংখ্যার হতে হবে", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VaultAccentEmerald),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("পাসকোড আপডেট করুন", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("বন্ধ করুন", color = Color.LightGray)
                }
            }
        }
    }
}
