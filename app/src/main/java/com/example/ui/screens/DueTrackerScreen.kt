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
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun DueTrackerScreen(
    unpaidCycles: List<BillingCycleEntity>,
    students: List<StudentEntity>,
    useBengaliNumerals: Boolean,
    onSelectStudent: (String) -> Unit,
    onOpenAddPaymentForStudent: (studentId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    val studentMap = remember(students) { students.associateBy { it.id } }
    val today = remember { LocalDate.now() }

    val filteredCycles = remember(unpaidCycles, selectedFilter, searchQuery, studentMap) {
        unpaidCycles.filter { cycle ->
            val stu = cycle.studentId?.let { studentMap[it] }
            val studentName = stu?.name ?: "যৌথ বিলিং"

            val matchesSearch = searchQuery.isBlank() ||
                    studentName.contains(searchQuery, ignoreCase = true) ||
                    (stu?.phone?.contains(searchQuery, ignoreCase = true) == true) ||
                    (stu?.grade?.contains(searchQuery, ignoreCase = true) == true)

            val dueDate = try { AccountingEngine.parseDate(cycle.dueDate) } catch (e: Exception) { today }

            val matchesFilter = when (selectedFilter) {
                "TODAY" -> dueDate == today
                "NEXT_7_DAYS" -> !dueDate.isBefore(today) && !dueDate.isAfter(today.plusDays(7))
                "OVERDUE" -> dueDate.isBefore(today)
                "PARTIAL" -> cycle.status == "PARTIAL"
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    val totalFilteredDues = filteredCycles.sumOf { it.closingBalance }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Summary Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("মোট বকেয়া পাওনা", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = AccountingEngine.formatCurrency(totalFilteredDues, useBengaliNumerals),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = OverdueRed
                        )
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${filteredCycles.size}টি সাইকেল",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("শিক্ষার্থী, শ্রেণি বা ফোন দিয়ে খুঁজুন...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (searchQuery.isNotBlank()) {
                { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = null) } }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "ALL" to "সকল বকেয়া",
                "OVERDUE" to "মেয়াদোত্তীর্ণ",
                "NEXT_7_DAYS" to "আগামী ৭ দিন",
                "TODAY" to "আজকের",
                "PARTIAL" to "আংশিক"
            ).forEach { (filterKey, label) ->
                FilterChip(
                    selected = selectedFilter == filterKey,
                    onClick = { selectedFilter = filterKey },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredCycles.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.CheckCircle,
                title = "কোনো বকেয়া রেকর্ড নেই",
                description = "নির্বাচিত ফিল্টারে কোনো অপরিশোধিত ফি সাইকেল পাওয়া যায়নি।"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(filteredCycles, key = { it.id }) { cycle ->
                    val stu = cycle.studentId?.let { studentMap[it] }
                    val dueDate = try { AccountingEngine.parseDate(cycle.dueDate) } catch (e: Exception) { today }
                    val isOverdue = dueDate.isBefore(today)
                    val daysDiff = ChronoUnit.DAYS.between(today, dueDate)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (cycle.studentId != null) onSelectStudent(cycle.studentId)
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
                                        text = stu?.name ?: "যৌথ বিলিং",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${stu?.grade ?: ""} • সাইকেল: ${cycle.periodStart} হতে ${cycle.periodEnd}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusBadge(status = cycle.status)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isOverdue) "${kotlin.math.abs(daysDiff)} দিন অতিবাহিত (মেয়াদোত্তীর্ণ)"
                                        else if (daysDiff == 0L) "আজই শেষ দিন"
                                        else "বাকি $daysDiff দিন (তারিখ: ${cycle.dueDate})",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isOverdue) OverdueRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                    Text(
                                        text = "মোট ধার্য: ৳ ${cycle.totalCharge} • আদায়: ৳ ${cycle.allocatedAmount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = AccountingEngine.formatCurrency(cycle.closingBalance, useBengaliNumerals),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = OverdueRed
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (cycle.studentId != null) {
                                        Button(
                                            onClick = { onOpenAddPaymentForStudent(cycle.studentId) },
                                            colors = ButtonDefaults.buttonColors(containerColor = PaidGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text("পেমেন্ট নিন", style = MaterialTheme.typography.labelSmall)
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
