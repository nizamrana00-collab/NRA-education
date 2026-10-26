package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.SoftLavender

@Composable
fun BackupScreen(
    onExportBackup: ((String) -> Unit) -> Unit,
    onRestoreBackup: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var exportedJson by remember { mutableStateOf<String?>(null) }
    var importInputText by remember { mutableStateOf("") }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "ডাটা ব্যাকআপ, এক্সপোর্ট ও নিরাপত্তা",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "আপনার মূল্যবান টিউশন হিসাবের সম্পূর্ণ ডাটাবেস সংরক্ষণ ও পুনরুদ্ধার করুন",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Offline security reassurance card
            item {
                Surface(
                    color = SoftLavender,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "১০০% নিরাপদ অফলাইন ডাটাবেস",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = PurplePrimary)
                            )
                            Text(
                                text = "আপনার সকল শিক্ষার্থী, লেনদেন ও হিসাব আপনার ডিভাইসের নিরাপদ SQLite Room ডাটাবেসে স্থায়ীভাবে সংরক্ষিত। ইন্টারনেট সংযোগ ছাড়াও সকল ফিচার পূর্ণাঙ্গভাবে কাজ করে।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Export Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("পূর্ণাঙ্গ ডাটাবেস এক্সপোর্ট (JSON)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = "একটি ক্লিকেই সম্পূর্ণ শিক্ষার্থী তালিকা, ফি সাইকেল এবং সকল পেমেন্টের সমন্বিত ব্যাকআপ ফাইল তৈরি করুন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                onExportBackup { json ->
                                    exportedJson = json
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Tuition_Accounting_Backup", json)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "ব্যাকআপ ডাটা ক্লিপবোর্ডে কপি হয়েছে!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ব্যাকআপ ফাইল তৈরি ও কপি করুন")
                        }

                        if (exportedJson != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "ব্যাকআপ সফল হয়েছে! (${exportedJson!!.length} অক্ষর)",
                                        style = MaterialTheme.typography.labelMedium.copy(color = PaidGreen, fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = exportedJson!!.take(200) + "...",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Restore Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ব্যাকআপ রিস্টোর ও ইমপোর্ট", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = "পূর্বে সংরক্ষিত JSON ব্যাকআপ কোড নিচে পেস্ট করে আপনার ডাটাবেস পুনরুদ্ধার করুন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = importInputText,
                            onValueChange = { importInputText = it },
                            placeholder = { Text("এখানে ব্যাকআপ JSON কোড পেস্ট করুন...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                if (importInputText.isNotBlank()) {
                                    showRestoreConfirmDialog = true
                                }
                            },
                            enabled = importInputText.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ব্যাকআপ রিস্টোর যাচাই ও সম্পন্ন করুন")
                        }
                    }
                }
            }
        }
    }

    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("রিস্টোর নিশ্চিতকরণ") },
            text = {
                Text("আপনি কি এই ব্যাকআপ ফাইলটি আপনার ডাটাবেসে রিস্টোর করতে চান? এটি বিদ্যমান তালিকার সাথে নতুন রেকর্ডগুলো সুরক্ষিতভাবে সংযোজন করবে।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreBackup(importInputText)
                        showRestoreConfirmDialog = false
                        importInputText = ""
                        Toast.makeText(context, "রিস্টোর সম্পন্ন হয়েছে!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("হ্যাঁ, রিস্টোর করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
