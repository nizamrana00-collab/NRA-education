package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HelpScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "ব্যবহার নির্দেশিকা ও অ্যাকাউন্টিং নীতিমালা",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "কীভাবে 'আমার টিউশন হিসাব' আপনার হিসাবরক্ষণ নিখুঁত রাখে",
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
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("১. বিলিং সাইকেল ও নোঙর দিন (Anchor Day)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = "সব শিক্ষার্থী মাসের ১ তারিখে ভর্তি হয় না। কেউ ১৫ বা ২৩ তারিখে ভর্তি হতে পারে। আমাদের অ্যাপ প্রতি শিক্ষার্থীর ভর্তির দিনটিকে 'নোঙর দিন' হিসেবে ধরে পরবর্তী মাসগুলোর চক্র তৈরি করে। কোনো মাসে (যেমন ফেব্রুয়ারি ২৮/২৯ দিন) দিন কম থাকলেও পরবর্তী মাসে তা পুনরায় মূল নোঙর দিনে ফিরে আসে।",
                            style = MaterialTheme.typography.bodySmall
                        )
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
                        Text("২. পুরাতন বকেয়া ও নতুন সাইকেল বিচ্ছিন্নকরণ", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = "কোনো শিক্ষার্থী দেরিতে বেতন দিলে পূর্ববর্তী মাসের বকেয়া আলাদা থাকে এবং নতুন মাসের সাইকেল যথানিয়মে শুরু হয়। সেপ্টেম্বর বকেয়া থাকা অবস্থায় অক্টোবর নতুন সাইকেল শুরু হলে সেপ্টেম্বর বকেয়া অপরিবর্তিত থাকে এবং অক্টোবর পেমেন্ট দিয়ে সেপ্টেম্বর ক্লিয়ার করলে অক্টোবর বকেয়া অক্ষুণ্ণ থাকে।",
                            style = MaterialTheme.typography.bodySmall
                        )
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
                        Text("৩. ছুটি বা বিরতিতে মেয়াদ বর্ধিতকরণ", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = "কোনো বিরতি রেকর্ড করার সাথে সাথে স্বয়ংক্রিয়ভাবে মেয়াদ বাড়ে না। অ্যাপ শিক্ষককে জিজ্ঞাসা করে: 'মেয়াদ বাড়াব', 'মেয়াদ বাড়াব না', অথবা 'পরে সিদ্ধান্ত নেব'। শিক্ষক 'মেয়াদ বাড়াব' অনুমোদন করলেই কেবল সাইকেলের সমাপ্তি দিন বাড়ানো হয়।",
                            style = MaterialTheme.typography.bodySmall
                        )
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
                        Text("৪. অগ্রিম ও অতিরিক্ত জমা (Advance Credit)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = "যদি বকেয়া ১,০০০ টাকা থাকে এবং অভিভাবক ১,৫০০ টাকা দেন, তবে ১,০০০ টাকা সাইকেলে বরাদ্দ হবে এবং অবশিষ্ট ৫০০ টাকা স্পষ্টভাবে 'অগ্রিম বা অনির্ধারিত জমা' হিসেবে দেখানো হবে। কখনোই টাকা হারিয়ে যাবে না।",
                            style = MaterialTheme.typography.bodySmall
                        )
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
                        Text("৫. হিসাব সমন্বয় ও নিরীক্ষা (Reconciliation)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = "অ্যাপের 'হিসাব সমন্বয়' স্ক্রিন প্রতি মুহূর্তে নিশ্চিত করে যে: [মোট পেমেন্ট] = [বরাদ্দকৃত অর্থ] + [অগ্রিম জমা] এবং প্রতিটি সাইকেলের [সমাপনী বকেয়া] = [চার্জ] - [বরাদ্দ]।",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
