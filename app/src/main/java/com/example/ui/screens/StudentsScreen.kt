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
import com.example.data.entity.BillingCycleEntity
import com.example.data.entity.StudentEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.PaidGreen

@Composable
fun StudentsScreen(
    students: List<StudentEntity>,
    unpaidCycles: List<BillingCycleEntity>,
    searchQuery: String,
    useBengaliNumerals: Boolean,
    onSelectStudent: (String) -> Unit,
    onOpenAddStudent: () -> Unit,
    onDeleteStudent: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var studentToDelete by remember { mutableStateOf<StudentEntity?>(null) }

    // Map studentId to outstanding balance
    val duesMap = remember(unpaidCycles) {
        unpaidCycles.groupBy { it.studentId }.mapValues { (_, cycles) ->
            cycles.sumOf { it.closingBalance }
        }
    }

    val filteredStudents = remember(students, searchQuery, selectedStatusFilter) {
        students.filter { s ->
            val matchesQuery = searchQuery.isBlank() ||
                    s.name.contains(searchQuery, ignoreCase = true) ||
                    s.grade.contains(searchQuery, ignoreCase = true) ||
                    s.guardianName.contains(searchQuery, ignoreCase = true) ||
                    s.phone.contains(searchQuery, ignoreCase = true)

            val matchesStatus = when (selectedStatusFilter) {
                "ACTIVE" -> s.status == "ACTIVE"
                "ON_BREAK" -> s.status == "ON_BREAK"
                "INACTIVE" -> s.status == "INACTIVE"
                else -> true
            }

            matchesQuery && matchesStatus
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddStudent,
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "নতুন শিক্ষার্থী")
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

            // Status Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "সকল (${students.size})",
                    "ACTIVE" to "সক্রিয় (${students.count { it.status == "ACTIVE" }})",
                    "ON_BREAK" to "বিরতি (${students.count { it.status == "ON_BREAK" }})",
                    "INACTIVE" to "নিষ্ক্রিয় (${students.count { it.status == "INACTIVE" }})"
                ).forEach { (status, label) ->
                    FilterChip(
                        selected = selectedStatusFilter == status,
                        onClick = { selectedStatusFilter = status },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredStudents.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.School,
                    title = "কোনো শিক্ষার্থী পাওয়া যায়নি",
                    description = if (searchQuery.isNotBlank()) "অনুসন্ধানের সাথে কোনো শিক্ষার্থীর নাম মিলছে না।" else "আপনার প্রথম শিক্ষার্থী যোগ করতে নিচের বোতামটি চাপুন।",
                    actionButtonText = if (students.isEmpty()) "নতুন শিক্ষার্থী যোগ করুন" else null,
                    onActionClick = onOpenAddStudent
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(filteredStudents, key = { it.id }) { student ->
                        val dueAmount = duesMap[student.id] ?: 0.0

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectStudent(student.id) },
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
                                            text = student.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${student.grade} • অভিভাবক: ${student.guardianName.ifBlank { "উল্লেখ নেই" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        StatusBadge(status = student.status)
                                        if (onDeleteStudent != null) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { studentToDelete = student },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = "শিক্ষার্থী মুছুন",
                                                    tint = OverdueRed,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "মাসিক ফি: ${AccountingEngine.formatCurrency(student.monthlyFee, useBengaliNumerals)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            text = "বিলিং নোঙর দিন: প্রতি মাসের ${if (useBengaliNumerals) AccountingEngine.toBengaliDigits(student.billingDayAnchor.toString()) else student.billingDayAnchor} তারিখ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        if (dueAmount > 0) {
                                            Text(
                                                text = "বকেয়া: ${AccountingEngine.formatCurrency(dueAmount, useBengaliNumerals)}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = OverdueRed
                                                )
                                            )
                                        } else {
                                            Text(
                                                text = "পরিশোধিত",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = PaidGreen
                                                )
                                            )
                                        }
                                        if (student.phone.isNotBlank()) {
                                            Text(
                                                text = "ফোন: ${student.phone}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

    // Delete confirmation dialog
    studentToDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = OverdueRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("শিক্ষার্থী মুছে ফেলবেন?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("আপনি কি নিশ্চিত যে '${s.name}'-এর সমস্ত তথ্য ও ফি রেকর্ড স্থায়ীভাবে মুছে ফেলতে চান?\n\nভুলবশত সেভ করা শিক্ষার্থীর হিসাব বাদ দিতে এটি ব্যবহার করুন।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = s.id
                        studentToDelete = null
                        onDeleteStudent?.invoke(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OverdueRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("হ্যাঁ, মুছে ফেলুন")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { studentToDelete = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("বাতিল")
                }
            }
        )
    }
}
