package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.accounting.AccountingEngine
import com.example.data.entity.BatchEntity
import com.example.data.entity.BillingCycleEntity
import com.example.data.entity.PaymentEntity
import com.example.data.entity.StudentEntity
import com.example.ui.theme.*
import java.time.LocalDate

@Composable
fun AddStudentDialog(
    batches: List<BatchEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        grade: String,
        guardianName: String,
        phone: String,
        address: String,
        admissionDate: String,
        monthlyFee: Double,
        billingDayAnchor: Int,
        batchId: String?,
        notes: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var guardianName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    val today = remember { LocalDate.now() }
    var admissionDate by remember { mutableStateOf(today.format(AccountingEngine.DATE_FORMATTER)) }
    var monthlyFeeStr by remember { mutableStateOf("1000") }
    var billingDayAnchorStr by remember { mutableStateOf(today.dayOfMonth.toString()) }
    var selectedBatchId by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Live preview of initial cycle
    val anchor = billingDayAnchorStr.toIntOrNull() ?: today.dayOfMonth
    val parsedStart = try { AccountingEngine.parseDate(admissionDate) } catch (e: Exception) { today }
    val previewEnd = remember(parsedStart, anchor) {
        AccountingEngine.calculateCycleEnd(parsedStart, anchor)
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "নতুন শিক্ষার্থী ভর্তি",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("শিক্ষার্থীর নাম *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = grade,
                        onValueChange = { grade = it },
                        label = { Text("শ্রেণি / বিষয় *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = monthlyFeeStr,
                        onValueChange = { monthlyFeeStr = it },
                        label = { Text("মাসিক ফি (৳) *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = admissionDate,
                        onValueChange = { admissionDate = it },
                        label = { Text("ভর্তির তারিখ (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = billingDayAnchorStr,
                        onValueChange = { billingDayAnchorStr = it },
                        label = { Text("বিলিং নোঙর দিন (১-৩১)") },
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                // Cycle preview box
                Surface(
                    color = SoftLavender,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "স্বয়ংক্রিয় সাইকেল প্রিভিউ:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = PurplePrimary
                        )
                        Text(
                            text = "প্রথম মাস: ${AccountingEngine.formatDate(parsedStart)} হতে ${AccountingEngine.formatDate(previewEnd)} (প্রতি মাসের $anchor তারিখে নবায়ন)",
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepPurple
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = guardianName,
                    onValueChange = { guardianName = it },
                    label = { Text("অভিভাবকের নাম") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("মোবাইল নম্বর") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (batches.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("ব্যাচ নির্ধারণ (ঐচ্ছিক):", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedBatchId == null,
                            onClick = { selectedBatchId = null },
                            label = { Text("ব্যক্তিগত") }
                        )
                        batches.forEach { b ->
                            FilterChip(
                                selected = selectedBatchId == b.id,
                                onClick = { selectedBatchId = b.id },
                                label = { Text(b.name) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("অতিরিক্ত মন্তব্য / ঠিকানা") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("বাতিল") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "শিক্ষার্থীর নাম দেওয়া আবশ্যক"
                                return@Button
                            }
                            val fee = monthlyFeeStr.toDoubleOrNull()
                            if (fee == null || fee < 0) {
                                errorMessage = "সঠিক মাসিক ফি লিখুন"
                                return@Button
                            }
                            val anchorVal = billingDayAnchorStr.toIntOrNull()
                            if (anchorVal == null || anchorVal !in 1..31) {
                                errorMessage = "বিলিং নোঙর দিন ১ হতে ৩১ এর মধ্যে হতে হবে"
                                return@Button
                            }
                            onConfirm(
                                name,
                                grade,
                                guardianName,
                                phone,
                                address,
                                admissionDate,
                                fee,
                                anchorVal,
                                selectedBatchId,
                                notes
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("সংরক্ষণ করুন")
                    }
                }
            }
        }
    }
}

@Composable
fun AddPaymentDialog(
    students: List<StudentEntity>,
    unpaidCycles: List<BillingCycleEntity>,
    preSelectedStudentId: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        studentId: String?,
        amountReceived: Double,
        paymentDate: String,
        paymentMethod: String,
        payerName: String,
        referenceId: String,
        notes: String,
        allocations: List<Pair<String, Double>>
    ) -> Unit
) {
    var selectedStudentId by remember { mutableStateOf(preSelectedStudentId ?: students.firstOrNull()?.id) }
    var amountReceivedStr by remember { mutableStateOf("") }
    val today = remember { LocalDate.now() }
    var paymentDate by remember { mutableStateOf(today.format(AccountingEngine.DATE_FORMATTER)) }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var payerName by remember { mutableStateOf("") }
    var referenceId by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Filter unpaid cycles for selected student
    val studentCycles = remember(selectedStudentId, unpaidCycles) {
        unpaidCycles.filter { it.studentId == selectedStudentId && it.closingBalance > 0 }
    }

    // Dynamic map of cycleId -> allocated amount string
    val allocationInputs = remember(selectedStudentId, studentCycles) {
        mutableStateMapOf<String, String>()
    }

    val amountReceived = amountReceivedStr.toDoubleOrNull() ?: 0.0
    val totalAllocated = allocationInputs.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
    val unallocatedCredit = (amountReceived - totalAllocated).coerceAtLeast(0.0)
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "পেমেন্ট গ্রহণ ও বণ্টন",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Student Selection
                if (preSelectedStudentId == null && students.isNotEmpty()) {
                    Text("শিক্ষার্থী নির্বাচন করুন:", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        students.take(5).forEach { s ->
                            FilterChip(
                                selected = selectedStudentId == s.id,
                                onClick = { selectedStudentId = s.id },
                                label = { Text(s.name) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountReceivedStr,
                        onValueChange = { amountReceivedStr = it },
                        label = { Text("গৃহীত টাকা (৳) *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = paymentDate,
                        onValueChange = { paymentDate = it },
                        label = { Text("তারিখ") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("পেমেন্ট মাধ্যম:", style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("CASH" to "নগদ", "BKASH" to "বিকাশ", "NAGAD" to "নগদ পে", "BANK_TRANSFER" to "ব্যাংক").forEach { (method, label) ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = referenceId,
                    onValueChange = { referenceId = it },
                    label = { Text("ট্রানজেকশন আইডি / রেফারেন্স (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
                // Cycles allocation section
                Text(
                    text = "বকেয়া ফি সাইকেলে বরাদ্দ করুন:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (studentCycles.isEmpty()) {
                    Surface(
                        color = PaidGreenBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "এই শিক্ষার্থীর কোন বকেয়া সাইকেল নেই। জমা দেওয়া সম্পূর্ণ টাকা (৳ ${amountReceived}) অগ্রিম হিসেবে সংরক্ষিত থাকবে।",
                            style = MaterialTheme.typography.bodySmall,
                            color = PaidGreen,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "সাইকেল তালিকা (${studentCycles.size}টি বকেয়া)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = {
                                // Auto-allocate up to amountReceived across cycles
                                var remaining = amountReceived
                                for (c in studentCycles) {
                                    val alloc = minOf(remaining, c.closingBalance)
                                    allocationInputs[c.id] = if (alloc > 0) alloc.toString() else "0"
                                    remaining = (remaining - alloc).coerceAtLeast(0.0)
                                }
                            }
                        ) {
                            Text("স্বয়ংক্রিয় বণ্টন", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    studentCycles.forEach { cycle ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${cycle.periodStart} হতে ${cycle.periodEnd}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "বকেয়া: ৳ ${cycle.closingBalance}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OverdueRed
                                    )
                                }
                                OutlinedTextField(
                                    value = allocationInputs[cycle.id] ?: "",
                                    onValueChange = { allocationInputs[cycle.id] = it },
                                    label = { Text("বরাদ্দ") },
                                    modifier = Modifier.width(110.dp),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                // Balance summary
                Surface(
                    color = SoftLavender,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("মোট বরাদ্দকৃত:", style = MaterialTheme.typography.bodySmall)
                            Text("৳ $totalAllocated", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("অগ্রিম / অনির্ধারিত ক্রেডিট:", style = MaterialTheme.typography.bodySmall)
                            Text("৳ $unallocatedCredit", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = PurplePrimary))
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("বাতিল") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (amountReceived <= 0) {
                                errorMessage = "সঠিক পেমেন্টের পরিমাণ দিন"
                                return@Button
                            }
                            if (totalAllocated > amountReceived) {
                                errorMessage = "মোট বরাদ্দ পেমেন্টের পরিমাণের চেয়ে বেশি হতে পারে না"
                                return@Button
                            }
                            val allocList = allocationInputs.mapNotNull { (cid, amtStr) ->
                                val a = amtStr.toDoubleOrNull() ?: 0.0
                                if (a > 0) Pair(cid, a) else null
                            }
                            onConfirm(
                                selectedStudentId,
                                amountReceived,
                                paymentDate,
                                paymentMethod,
                                payerName,
                                referenceId,
                                notes,
                                allocList
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("পেমেন্ট নিশ্চিত করুন")
                    }
                }
            }
        }
    }
}

@Composable
fun RecordBreakDialog(
    studentName: String,
    currentCycleEndDate: String?,
    onDismiss: () -> Unit,
    onConfirm: (
        startDate: String,
        endDate: String?,
        eligibleDays: Int,
        reason: String,
        decision: String
    ) -> Unit
) {
    val today = remember { LocalDate.now() }
    var startDate by remember { mutableStateOf(today.format(AccountingEngine.DATE_FORMATTER)) }
    var daysStr by remember { mutableStateOf("7") }
    var reason by remember { mutableStateOf("পরীক্ষার ছুটি") }
    // Decision options: EXTEND ("মেয়াদ বাড়াব"), DO_NOT_EXTEND ("মেয়াদ বাড়াব না"), DECIDE_LATER ("পরে সিদ্ধান্ত নেব")
    var decision by remember { mutableStateOf("EXTEND") }

    val days = daysStr.toIntOrNull() ?: 0
    val parsedCurrentEnd = try {
        if (currentCycleEndDate != null) AccountingEngine.parseDate(currentCycleEndDate) else today
    } catch (e: Exception) { today }
    val proposedExtendedEnd = parsedCurrentEnd.plusDays(days.toLong())

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "ছুটি / বিরতি রেকর্ড",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "শিক্ষার্থী: $studentName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = InfoCyanBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = InfoCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "স্বয়ংক্রিয় শনাক্তকরণ: ছুটির শুরুর তারিখ ($startDate) দেখে ওই নির্দিষ্ট মাসের ফি সাইকেলেই ছুটি যুক্ত হবে।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("শুরুর তারিখ") },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = daysStr,
                        onValueChange = { daysStr = it },
                        label = { Text("দিনের সংখ্যা") },
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("ছুটি বা বিরতির কারণ") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "বিলিং মেয়াদ কি বর্ধিত করবেন? (বাধ্যতামূলক সিদ্ধান্ত)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = decision == "EXTEND",
                            onClick = { decision = "EXTEND" }
                        )
                        Text("মেয়াদ বাড়াব (ছুটির দিন অনুযায়ী সাইকেল বাড়বে)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = decision == "DO_NOT_EXTEND",
                            onClick = { decision = "DO_NOT_EXTEND" }
                        )
                        Text("মেয়াদ বাড়াব না (নির্ধারিত তারিখেই থাকবে)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = decision == "DECIDE_LATER",
                            onClick = { decision = "DECIDE_LATER" }
                        )
                        Text("পরে সিদ্ধান্ত নেব")
                    }
                }

                if (decision == "EXTEND") {
                    Surface(
                        color = DueOrangeBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "প্রস্তাবিত তারিখ সমন্বয় প্রিভিউ:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = DueOrange
                            )
                            Text(
                                text = "বর্তমান সাইকেল শেষ: ${AccountingEngine.formatDate(parsedCurrentEnd)}\nছুটি পরবর্তী শেষ দিন: ${AccountingEngine.formatDate(proposedExtendedEnd)} (+$days দিন)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("বাতিল") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val end = try {
                                AccountingEngine.parseDate(startDate).plusDays(days.toLong()).format(AccountingEngine.DATE_FORMATTER)
                            } catch (e: Exception) { null }
                            onConfirm(startDate, end, days, reason, decision)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("নিশ্চিত করুন")
                    }
                }
            }
        }
    }
}

@Composable
fun ChangeFeeDialog(
    currentFee: Double,
    onDismiss: () -> Unit,
    onConfirm: (newFee: Double, effectiveDate: String, reason: String) -> Unit
) {
    var newFeeStr by remember { mutableStateOf(currentFee.toString()) }
    val today = remember { LocalDate.now() }
    var effectiveDate by remember { mutableStateOf(today.format(AccountingEngine.DATE_FORMATTER)) }
    var reason by remember { mutableStateOf("নতুন সেশনের ফি পরিবর্তন") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "ফি পরিবর্তন করুন",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("বর্তমান ফি: ৳ $currentFee", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newFeeStr,
                    onValueChange = { newFeeStr = it },
                    label = { Text("নতুন ফি (৳) *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = effectiveDate,
                    onValueChange = { effectiveDate = it },
                    label = { Text("কার্যকর হওয়ার তারিখ") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("ফি পরিবর্তনের কারণ") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Surface(
                    color = SoftLavender,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = "সুরক্ষা নীতি: পূর্বে পরিশোধিত বা প্রস্তুতকৃত সাইকেলসমূহ অপরিবর্তিত থাকবে। কার্যকর তারিখ হতে পরবর্তী সাইকেলে নতুন ফি প্রযুক্ত হবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepPurple,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("বাতিল") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val fee = newFeeStr.toDoubleOrNull()
                            if (fee == null || fee < 0) {
                                errorMessage = "সঠিক ফি লিখুন"
                                return@Button
                            }
                            onConfirm(fee, effectiveDate, reason)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("সংরক্ষণ করুন")
                    }
                }
            }
        }
    }
}

@Composable
fun AddBatchDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, grade: String, schedule: String, defaultFee: Double, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf("") }
    var feeStr by remember { mutableStateOf("1000") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "নতুন ব্যাচ তৈরি",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ব্যাচের নাম *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = grade,
                        onValueChange = { grade = it },
                        label = { Text("শ্রেণি / স্তর *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = feeStr,
                        onValueChange = { feeStr = it },
                        label = { Text("ডিফল্ট ফি (৳)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = schedule,
                    onValueChange = { schedule = it },
                    label = { Text("সময়সূচি (উদাঃ শনি-সোম বিকাল ৪টা)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("মন্তব্য") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("বাতিল") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "ব্যাচের নাম দেওয়া আবশ্যক"
                                return@Button
                            }
                            val f = feeStr.toDoubleOrNull() ?: 0.0
                            onConfirm(name, grade, schedule, f, notes)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("তৈরি করুন")
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentDetailDialog(
    payment: PaymentEntity,
    studentName: String?,
    onDismiss: () -> Unit,
    onReversePayment: (reason: String) -> Unit
) {
    var showReversePrompt by remember { mutableStateOf(false) }
    var reverseReason by remember { mutableStateOf("ভুল এন্ট্রি সংশোধন") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "পেমেন্ট রসিদ",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (payment.isReversed) {
                        Surface(
                            color = OverdueRedBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "বাতিলকৃত",
                                color = OverdueRed,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("রসিদ নম্বর: ${payment.id}", style = MaterialTheme.typography.bodySmall)
                Text("শিক্ষার্থী: ${studentName ?: "যৌথ বিলিং"}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Text("তারিখ: ${payment.paymentDate}", style = MaterialTheme.typography.bodySmall)
                Text("পেমেন্ট মাধ্যম: ${payment.paymentMethod}", style = MaterialTheme.typography.bodySmall)
                if (payment.referenceId.isNotBlank()) {
                    Text("রেফারেন্স আইডি: ${payment.referenceId}", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("মোট প্রাপ্ত টাকা:", style = MaterialTheme.typography.titleMedium)
                    Text("৳ ${payment.amountReceived}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = PaidGreen))
                }

                if (payment.unallocatedCredit > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("অগ্রিম / অতিরিক্ত জমা:", style = MaterialTheme.typography.bodySmall)
                        Text("৳ ${payment.unallocatedCredit}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = PurplePrimary))
                    }
                }

                if (payment.isReversed) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = OverdueRedBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "বাতিলের কারণ: ${payment.reversalReason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = OverdueRed,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (!payment.isReversed && showReversePrompt) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reverseReason,
                        onValueChange = { reverseReason = it },
                        label = { Text("বাতিলকরণের কারণ লিখুন") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onReversePayment(reverseReason) },
                        colors = ButtonDefaults.buttonColors(containerColor = OverdueRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("পেমেন্ট বাতিল নিশ্চিত করুন")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (!payment.isReversed && !showReversePrompt) {
                        TextButton(
                            onClick = { showReversePrompt = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = OverdueRed)
                        ) {
                            Text("পেমেন্ট বাতিল করুন")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                        Text("বন্ধ করুন")
                    }
                }
            }
        }
    }
}
