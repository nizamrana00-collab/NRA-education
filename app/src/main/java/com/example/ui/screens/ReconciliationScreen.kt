package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.accounting.AccountingEngine
import com.example.data.repository.TuitionRepository
import com.example.ui.theme.*

@Composable
fun ReconciliationScreen(
    report: TuitionRepository.ReconciliationReport?,
    useBengaliNumerals: Boolean,
    onRunReconciliation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "হিসাব নিরীক্ষা ও সমন্বয় (Audit & Reconciliation)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "গাণিতিক নির্ভুলতা ও ডাটাবেস অখণ্ডতা যাচাই",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRunReconciliation) {
                Icon(Icons.Default.Refresh, contentDescription = "পুনরায় যাচাই করুন", tint = PurplePrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (report == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // Status Banner
                item {
                    val isOk = report.isReconciled
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOk) PaidGreenBg else OverdueRedBg
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isOk) PaidGreen else OverdueRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = if (isOk) "সব হিসাব শতভাগ সমন্বিত ও নির্ভুল!" else "হিসাবে অসঙ্গতি চিহ্নিত হয়েছে!",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOk) PaidGreen else OverdueRed
                                    )
                                )
                                Text(
                                    text = if (isOk)
                                        "গৃহীত পেমেন্ট, বরাদ্দকৃত অর্থ, অগ্রিম জমা এবং সাইকেল সমাপনী উদ্বৃত্তের সকল সমীকরণ সম্পূর্ণ সঠিক রয়েছে।"
                                    else
                                        report.discrepancyMessage ?: "কিছু সাইকেলের সংরক্ষিত উদ্বৃত্তে হিসাবগত অসঙ্গতি লক্ষ্য করা গেছে।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Balance equation check
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("১. পেমেন্ট বনাম বরাদ্দ অখণ্ডতা সমীকরণ", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text(
                                text = "নীতি: [মোট নগদ প্রাপ্তি] = [মোট বরাদ্দকৃত টাকা] + [মোট অগ্রিম/অনির্ধারিত জমা]",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider()

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("মোট গৃহীত পেমেন্ট:")
                                Text(AccountingEngine.formatCurrency(report.totalCashReceived, useBengaliNumerals), fontWeight = FontWeight.Bold, color = PaidGreen)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("সাইকেলে মোট বরাদ্দকৃত:")
                                Text(AccountingEngine.formatCurrency(report.totalAllocatedAmount, useBengaliNumerals), fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("মোট অগ্রিম/অনির্ধারিত ক্রেডিট:")
                                Text(AccountingEngine.formatCurrency(report.totalUnallocatedCredit, useBengaliNumerals), fontWeight = FontWeight.SemiBold, color = PurplePrimary)
                            }
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("বরাদ্দ ও অগ্রিমের সমষ্টি:", fontWeight = FontWeight.Bold)
                                Text(
                                    AccountingEngine.formatCurrency(report.totalAllocatedAmount + report.totalUnallocatedCredit, useBengaliNumerals),
                                    fontWeight = FontWeight.Bold,
                                    color = PaidGreen
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("২. সাইকেল উদ্বৃত্ত অখণ্ডতা সমীকরণ", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text(
                                text = "নীতি: [সমাপনী বকেয়া] = [প্রারম্ভিক বকেয়া] + [ধার্যকৃত ফি] + [সমন্বয়] − [পরিশোধিত বরাদ্দ]",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider()

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("মোট ধার্যকৃত চার্জ:")
                                Text(AccountingEngine.formatCurrency(report.totalCycleCharges, useBengaliNumerals), fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("মোট সংগৃহীত বরাদ্দ:")
                                Text("-${AccountingEngine.formatCurrency(report.totalAllocatedAmount, useBengaliNumerals)}", color = PaidGreen)
                            }
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("প্রত্যাশিত মোট বকেয়া:", fontWeight = FontWeight.Bold)
                                Text(
                                    AccountingEngine.formatCurrency(report.totalOutstandingDues, useBengaliNumerals),
                                    fontWeight = FontWeight.Bold,
                                    color = OverdueRed
                                )
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = onRunReconciliation,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("পুনরায় সম্পূর্ণ ডাটাবেস নিরীক্ষা চালান")
                    }
                }
            }
        }
    }
}
