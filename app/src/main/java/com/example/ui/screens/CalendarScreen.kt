package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.accounting.AccountingEngine
import com.example.data.entity.BillingCycleEntity
import com.example.data.entity.StudentEntity
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.OverdueRedBg
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PurplePrimary
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
    unpaidCycles: List<BillingCycleEntity>,
    students: List<StudentEntity>,
    useBengaliNumerals: Boolean,
    onSelectStudent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    val studentMap = remember(students) { students.associateBy { it.id } }

    val daysInMonth = currentYearMonth.lengthOfMonth()
    val firstDayOfWeek = currentYearMonth.atDay(1).dayOfWeek.value % 7 // Sunday = 0, Monday = 1 ...

    // Filter cycles with due date in this month
    val cyclesInMonth = remember(unpaidCycles, currentYearMonth) {
        val ymStr = String.format("%04d-%02d", currentYearMonth.year, currentYearMonth.monthValue)
        unpaidCycles.filter { it.dueDate.startsWith(ymStr) }
    }

    val cyclesByDay = remember(cyclesInMonth) {
        cyclesInMonth.groupBy {
            try {
                AccountingEngine.parseDate(it.dueDate).dayOfMonth
            } catch (e: Exception) { 1 }
        }
    }

    var selectedDay by remember { mutableStateOf<Int?>(LocalDate.now().dayOfMonth) }
    val selectedDayCycles = remember(selectedDay, cyclesByDay) {
        if (selectedDay == null) emptyList() else cyclesByDay[selectedDay] ?: emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Month Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "পূর্ববর্তী মাস")
            }
            Text(
                text = "${currentYearMonth.month.name} ${currentYearMonth.year}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            IconButton(onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "পরবর্তী মাস")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Weekday labels
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি").forEach { dayLabel ->
                Text(
                    text = dayLabel,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar Grid (Card)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                var dayCounter = 1
                for (week in 0 until 6) {
                    if (dayCounter > daysInMonth) break
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            if ((week == 0 && col < firstDayOfWeek) || dayCounter > daysInMonth) {
                                Box(modifier = Modifier.weight(1f).height(44.dp))
                            } else {
                                val d = dayCounter
                                val hasDue = cyclesByDay.containsKey(d)
                                val isSelected = selectedDay == d

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) PurplePrimary
                                            else if (hasDue) OverdueRedBg
                                            else Color.Transparent
                                        )
                                        .clickable { selectedDay = d },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (useBengaliNumerals) AccountingEngine.toBengaliDigits(d.toString()) else d.toString(),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (hasDue || isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else if (hasDue) OverdueRed else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        if (hasDue && !isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(OverdueRed)
                                            )
                                        }
                                    }
                                }
                                dayCounter++
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected day details
        Text(
            text = "নির্বাচিত দিন (${selectedDay ?: 1} তারিখ) এর বকেয়া ও শেষ সময়সীমা:",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedDayCycles.isEmpty()) {
            Text(
                text = "এই দিনে কোনো শিক্ষার্থীর পেমেন্ট শেষ সময়সীমা নেই।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(selectedDayCycles) { cycle ->
                    val stu = cycle.studentId?.let { studentMap[it] }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (cycle.studentId != null) onSelectStudent(cycle.studentId)
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(stu?.name ?: "যৌথ বিলিং", fontWeight = FontWeight.Bold)
                                Text("মেয়াদ: ${cycle.periodStart} হতে ${cycle.periodEnd}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                text = AccountingEngine.formatCurrency(cycle.closingBalance, useBengaliNumerals),
                                fontWeight = FontWeight.Bold,
                                color = OverdueRed
                            )
                        }
                    }
                }
            }
        }
    }
}
