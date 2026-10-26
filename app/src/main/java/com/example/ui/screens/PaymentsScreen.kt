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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.accounting.AccountingEngine
import com.example.data.entity.PaymentEntity
import com.example.data.entity.StudentEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PaymentDetailDialog
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PurplePrimary

@Composable
fun PaymentsScreen(
    payments: List<PaymentEntity>,
    students: List<StudentEntity>,
    useBengaliNumerals: Boolean,
    onOpenAddPayment: () -> Unit,
    onReversePayment: (paymentId: String, reason: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMethodFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    val studentMap = remember(students) { students.associateBy { it.id } }
    var selectedPaymentForDetail by remember { mutableStateOf<PaymentEntity?>(null) }

    val filteredPayments = remember(payments, selectedMethodFilter, searchQuery, studentMap) {
        payments.filter { p ->
            val stuName = p.studentId?.let { studentMap[it]?.name } ?: "যৌথ বিলিং"
            val matchesSearch = searchQuery.isBlank() ||
                    stuName.contains(searchQuery, ignoreCase = true) ||
                    p.referenceId.contains(searchQuery, ignoreCase = true) ||
                    p.payerName.contains(searchQuery, ignoreCase = true)

            val matchesMethod = when (selectedMethodFilter) {
                "CASH" -> p.paymentMethod == "CASH"
                "BKASH" -> p.paymentMethod == "BKASH"
                "NAGAD" -> p.paymentMethod == "NAGAD"
                "BANK_TRANSFER" -> p.paymentMethod == "BANK_TRANSFER"
                else -> true
            }

            matchesSearch && matchesMethod
        }
    }

    val totalCashReceived = filteredPayments.filter { !it.isReversed }.sumOf { it.amountReceived }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddPayment,
                shape = RoundedCornerShape(16.dp),
                containerColor = PaidGreen
            ) {
                Icon(Icons.Default.Add, contentDescription = "পেমেন্ট গ্রহণ", tint = Color.White)
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

            // Summary Card
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
                        Text("মোট প্রাপ্ত টাকা (নগদ প্রবাহ)", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = AccountingEngine.formatCurrency(totalCashReceived, useBengaliNumerals),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PaidGreen
                            )
                        )
                    }
                    Text(
                        text = "${filteredPayments.count { !it.isReversed }}টি লেনদেন",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("শিক্ষার্থী, ট্রানজেকশন আইডি বা রেফারেন্স...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Method Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "সকল মাধ্যম",
                    "CASH" to "নগদ",
                    "BKASH" to "বিকাশ",
                    "NAGAD" to "নগদ পে",
                    "BANK_TRANSFER" to "ব্যাংক"
                ).forEach { (m, label) ->
                    FilterChip(
                        selected = selectedMethodFilter == m,
                        onClick = { selectedMethodFilter = m },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredPayments.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Receipt,
                    title = "কোনো লেনদেন পাওয়া যায়নি",
                    description = "নতুন পেমেন্ট রেকর্ড করতে নিচের '+' বোতামটি চাপুন।",
                    actionButtonText = "পেমেন্ট রেকর্ড করুন",
                    onActionClick = onOpenAddPayment
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(filteredPayments, key = { it.id }) { payment ->
                        val stuName = payment.studentId?.let { studentMap[it]?.name } ?: "যৌথ বিলিং"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedPaymentForDetail = payment },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stuName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${payment.paymentDate} • মাধ্যম: ${when (payment.paymentMethod) {
                                            "CASH" -> "নগদ"
                                            "BKASH" -> "বিকাশ"
                                            "NAGAD" -> "নগদ পে"
                                            "BANK_TRANSFER" -> "ব্যাংক ট্রান্সফার"
                                            else -> payment.paymentMethod
                                        }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (payment.referenceId.isNotBlank()) {
                                        Text(
                                            text = "রেফারেন্স: ${payment.referenceId}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "+${AccountingEngine.formatCurrency(payment.amountReceived, useBengaliNumerals)}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (payment.isReversed) Color.Gray else PaidGreen
                                        )
                                    )
                                    if (payment.isReversed) {
                                        Text("বাতিলকৃত", style = MaterialTheme.typography.labelSmall, color = OverdueRed)
                                    } else if (payment.unallocatedCredit > 0) {
                                        Text(
                                            text = "অগ্রিম: ৳ ${payment.unallocatedCredit}",
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
    }

    selectedPaymentForDetail?.let { payment ->
        PaymentDetailDialog(
            payment = payment,
            studentName = payment.studentId?.let { studentMap[it]?.name },
            onDismiss = { selectedPaymentForDetail = null },
            onReversePayment = { reason ->
                onReversePayment(payment.id, reason)
                selectedPaymentForDetail = null
            }
        )
    }
}
