package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PurplePrimary

@Composable
fun SettingsScreen(
    useBengaliNumerals: Boolean,
    onToggleBengaliNumerals: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "সেটিংস ও পছন্দসমূহ",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "অ্যাপের প্রদর্শন ও হিসাবরক্ষণ সংক্রান্ত বিকল্পসমূহ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("ভাষা ও সংখ্যা প্রদর্শন", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("বাংলা সংখ্যা ব্যবহার করুন (উদাঃ ৳ ১,৫০০)", style = MaterialTheme.typography.bodyMedium)
                                Text("টাকা ও তারিখের সংখ্যা বাংলায় প্রদর্শন করবে", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = useBengaliNumerals,
                                onCheckedChange = { onToggleBengaliNumerals() }
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
                        Text("অ্যাকাউন্টিং পলিসি ও নিয়মাবলি", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text("• মুদ্রা: বাংলাদেশী টাকা (৳)", style = MaterialTheme.typography.bodyMedium)
                        Text("• ডিফল্ট টাইমজোন: Asia/Dhaka (বাংলাদেশ সময়)", style = MaterialTheme.typography.bodyMedium)
                        Text("• তারিখ ফরম্যাট: DD-MM-YYYY (দিন-মাস-বছর)", style = MaterialTheme.typography.bodyMedium)
                        Text("• সাইকেল নীতি: ভর্তির দিন নোঙর (Billing-Day Anchor)", style = MaterialTheme.typography.bodyMedium)
                        Text("• দশমিক নির্ভুলতা: শতভাগ ফ্লোটিং-পয়েন্ট মুক্ত সঠিক হিসাব", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("অ্যাপ পরিচিতি", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text("অ্যাপের নাম: আমার টিউশন হিসাব", style = MaterialTheme.typography.bodyMedium)
                        Text("সংস্করণ: ১.০ (অফলাইন ও ক্লাউড-প্রস্তুত নির্ভরযোগ্য স্থাপত্য)", style = MaterialTheme.typography.bodySmall)
                        Text("উদ্দেশ্য: বাংলাদেশের প্রাইভেট টিউটর ও কোচিং শিক্ষকদের জন্য হিসাব ও বকেয়া খতিয়ান", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
