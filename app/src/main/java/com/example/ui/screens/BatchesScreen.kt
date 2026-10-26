package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.accounting.AccountingEngine
import com.example.data.entity.BatchEntity
import com.example.data.entity.BillingCycleEntity
import com.example.data.entity.StudentEntity
import com.example.ui.components.AddBatchDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PurplePrimary

@Composable
fun BatchesScreen(
    batches: List<BatchEntity>,
    students: List<StudentEntity>,
    unpaidCycles: List<BillingCycleEntity>,
    useBengaliNumerals: Boolean,
    onAddBatch: (name: String, grade: String, schedule: String, defaultFee: Double, notes: String) -> Unit,
    onSelectStudent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedBatchId by remember { mutableStateOf<String?>(null) }

    // Map batch to students
    val studentsInBatch = remember(batches, students, selectedBatchId) {
        if (selectedBatchId == null) emptyList()
        else students.filter { it.batchId == selectedBatchId }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "নতুন ব্যাচ")
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "ব্যাচ ও গ্রুপ ব্যবস্থাপনা (${batches.size}টি ব্যাচ)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (batches.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Groups,
                    title = "কোনো ব্যাচ তৈরি করা হয়নি",
                    description = "দলগত বা ব্যাচভিত্তিক পড়ানোর জন্য নতুন ব্যাচ তৈরি করুন।",
                    actionButtonText = "নতুন ব্যাচ তৈরি করুন",
                    onActionClick = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(batches, key = { it.id }) { batch ->
                        val batchStudents = students.filter { it.batchId == batch.id }
                        val isExpanded = selectedBatchId == batch.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedBatchId = if (isExpanded) null else batch.id
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = batch.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "শ্রেণি: ${batch.grade} • সময়: ${batch.schedule.ifBlank { "নির্ধারিত নেই" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${batchStudents.size} জন শিক্ষার্থী",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "ডিফল্ট মাসিক ফি: ${AccountingEngine.formatCurrency(batch.defaultFee, useBengaliNumerals)}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = PurplePrimary, fontWeight = FontWeight.SemiBold)
                                )

                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "এই ব্যাচের শিক্ষার্থীরা:",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    if (batchStudents.isEmpty()) {
                                        Text("এই ব্যাচে কোনো শিক্ষার্থী যুক্ত নেই।", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        batchStudents.forEach { stu ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { onSelectStudent(stu.id) }
                                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(stu.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                                Text("ফি: ৳ ${stu.monthlyFee}", style = MaterialTheme.typography.labelSmall, color = PaidGreen)
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
    }

    if (showAddDialog) {
        AddBatchDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, grade, schedule, fee, notes ->
                onAddBatch(name, grade, schedule, fee, notes)
                showAddDialog = false
            }
        )
    }
}
