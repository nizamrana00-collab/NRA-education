package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.accounting.AccountingEngine
import com.example.data.entity.BillingCycleEntity
import com.example.data.entity.PaymentEntity
import com.example.data.entity.StudentEntity
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.DashboardMetrics
import java.time.LocalDate

@Composable
fun DashboardScreen(
    metrics: DashboardMetrics,
    students: List<StudentEntity>,
    unpaidCycles: List<BillingCycleEntity>,
    recentPayments: List<PaymentEntity>,
    useBengaliNumerals: Boolean,
    onNavigate: (AppScreen) -> Unit,
    onSelectStudent: (String) -> Unit,
    onOpenAddStudent: () -> Unit,
    onOpenAddPayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val studentMap = students.associateBy { it.id }
    val today = LocalDate.now()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Hero Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(DeepPurple, VibrantPurple, RoyalIndigo)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White.copy(alpha = 0.22f),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Image(
                                                painter = painterResource(id = R.drawable.ic_nra_logo),
                                                contentDescription = "NRA Logo",
                                                modifier = Modifier.size(38.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "NRA Education Center",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp
                                        ),
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "আজ ${AccountingEngine.formatDate(today, useBengaliNumerals)} • টিউশন ও শিক্ষার্থী হিসাব খতিয়ান",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            ) {
                                IconButton(onClick = { onNavigate(AppScreen.RECONCILIATION) }) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "সমন্বয়",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onOpenAddPayment,
                                colors = ButtonDefaults.buttonColors(containerColor = PaidGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("পেমেন্ট গ্রহণ", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            FilledTonalButton(
                                onClick = onOpenAddStudent,
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = DeepPurple, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("নতুন শিক্ষার্থী", color = DeepPurple, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }

        // Section: মূল অর্থনৈতিক সারসংক্ষেপ (Key Financial KPIs)
        item {
            Text(
                text = "চলতি মাসের হিসাব সারসংক্ষেপ",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "চলতি মাসে আদায়",
                    value = AccountingEngine.formatCurrency(metrics.currentMonthReceived, useBengaliNumerals),
                    subtitle = "প্রকৃত প্রাপ্ত টাকা",
                    icon = Icons.Default.CheckCircle,
                    gradientColors = listOf(Color(0xFF00897B), Color(0xFF26A69A)),
                    onClick = { onNavigate(AppScreen.REPORTS) },
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "মোট বকেয়া",
                    value = AccountingEngine.formatCurrency(metrics.totalOutstandingDues, useBengaliNumerals),
                    subtitle = if (metrics.overdueCount > 0) "${metrics.overdueCount}টি মেয়াদোত্তীর্ণ" else "সকল সাইকেল",
                    icon = Icons.Default.HourglassTop,
                    gradientColors = if (metrics.overdueCount > 0)
                        listOf(Color(0xFFD32F2F), Color(0xFFE57373))
                    else
                        listOf(Color(0xFFF57C00), Color(0xFFFFB74D)),
                    onClick = { onNavigate(AppScreen.DUE_TRACKER) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "নির্ধারিত ফি (চলতি মাস)",
                    value = AccountingEngine.formatCurrency(metrics.currentMonthCharged, useBengaliNumerals),
                    subtitle = "ধার্যকৃত মোট ফি",
                    icon = Icons.Default.ReceiptLong,
                    gradientColors = listOf(Color(0xFF3F51B5), Color(0xFF5C6BC0)),
                    onClick = { onNavigate(AppScreen.REPORTS) },
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "অগ্রিম / ক্রেডিট",
                    value = AccountingEngine.formatCurrency(metrics.totalAdvanceCredit, useBengaliNumerals),
                    subtitle = "অনির্ধারিত জমা",
                    icon = Icons.Default.AccountBalanceWallet,
                    gradientColors = listOf(Color(0xFF7B1FA2), Color(0xFFAB47BC)),
                    onClick = { onNavigate(AppScreen.PAYMENTS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Dedicated Shortcut Banner to Monthly Reports & Lifetime History Archive
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigate(AppScreen.REPORTS) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SoftLavender),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DeepPurple,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "মাসিক রিপোর্ট ও হিসাবের ইতিহাস",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                                color = DeepPurple
                            )
                            Text(
                                text = "প্রতি মাসের আদায়, ছাত্র সংখ্যা ও আজীবন সংরক্ষিত খতিয়ান",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "দেখুন",
                        tint = DeepPurple,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Section: শিক্ষার্থী পরিসংখ্যান (Student Overview KPIs)
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "মোট শিক্ষার্থী",
                    value = if (useBengaliNumerals) AccountingEngine.toBengaliDigits(metrics.totalStudents.toString()) else metrics.totalStudents.toString(),
                    subtitle = "${metrics.activeStudents} সক্রিয় • ${metrics.onBreakStudents} বিরতি",
                    icon = Icons.Default.School,
                    gradientColors = listOf(Color(0xFF5E35B1), Color(0xFF7E57C2)),
                    onClick = { onNavigate(AppScreen.STUDENTS) },
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "আগামী ৭ দিনে বকেয়া",
                    value = AccountingEngine.formatCurrency(metrics.dueNext7DaysAmount, useBengaliNumerals),
                    subtitle = "${metrics.dueNext7DaysCount} জন শিক্ষার্থীর",
                    icon = Icons.Default.CalendarToday,
                    gradientColors = listOf(Color(0xFF0097A7), Color(0xFF4DD0E1)),
                    onClick = { onNavigate(AppScreen.DUE_TRACKER) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Section: জরুরি বকেয়া তালিকা (Immediate Dues)
        item {
            SectionHeader(
                title = "জরুরি বকেয়া তালিকা",
                subtitle = "পরিশোধের অপেক্ষায় থাকা সাইকেলসমূহ",
                actionText = "সকল বকেয়া (${unpaidCycles.size})",
                onActionClick = { onNavigate(AppScreen.DUE_TRACKER) }
            )
        }

        if (unpaidCycles.isEmpty()) {
            item {
                Surface(
                    color = PaidGreenBg,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = PaidGreen, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "কোনো বকেয়া পাওনা নেই!",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = PaidGreen
                            )
                            Text(
                                text = "সকল শিক্ষার্থীর বিলিং হিসাব সম্পূর্ণ পরিশোধিত রয়েছে।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        } else {
            items(unpaidCycles.take(3)) { cycle ->
                val stu = cycle.studentId?.let { studentMap[it] }
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stu?.name ?: "যৌথ বিলিং",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "মেয়াদ: ${cycle.periodStart} হতে ${cycle.periodEnd}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "পরিশোধের তারিখ: ${cycle.dueDate}",
                                style = MaterialTheme.typography.labelSmall,
                                color = OverdueRed
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
                            FilledTonalButton(
                                onClick = onOpenAddPayment,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("পেমেন্ট নিন", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Section: সাম্প্রতিক পেমেন্ট কার্যক্রম (Recent Payment Activity)
        item {
            SectionHeader(
                title = "সাম্প্রতিক পেমেন্ট কার্যক্রম",
                subtitle = "সর্বশেষ গৃহীত লেনদেনসমূহ",
                actionText = "সকল পেমেন্ট",
                onActionClick = { onNavigate(AppScreen.PAYMENTS) }
            )
        }

        if (recentPayments.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "এখনও কোনো পেমেন্ট রেকর্ড করা হয়নি। উপরে 'পেমেন্ট গ্রহণ' চাপুন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(recentPayments.take(3)) { p ->
                val stu = p.studentId?.let { studentMap[it] }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PaidGreenBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = PaidGreen)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stu?.name ?: "যৌথ বিলিং",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${p.paymentDate} • ${p.paymentMethod}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "+${AccountingEngine.formatCurrency(p.amountReceived, useBengaliNumerals)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PaidGreen
                                )
                            )
                            if (p.unallocatedCredit > 0) {
                                Text(
                                    text = "অগ্রিম: ৳ ${p.unallocatedCredit}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PurplePrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
