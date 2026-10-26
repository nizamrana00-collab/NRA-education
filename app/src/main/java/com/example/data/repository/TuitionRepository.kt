package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.accounting.AccountingEngine
import com.example.data.database.AppDatabase
import com.example.data.entity.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

class TuitionRepository(
    private val db: AppDatabase,
    private val context: Context
) {
    private val TAG = "TuitionRepository"
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private val firestore: FirebaseFirestore by lazy {
        val databaseId = try {
            context.getString(R.string.firestore_database_id)
        } catch (e: Exception) {
            "ai-studio-android-aecc43ff-980d-45e3-b1af-473788d64fbb"
        }
        FirebaseFirestore.getInstance(databaseId)
    }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    val allStudents: Flow<List<StudentEntity>> = db.studentDao().getAllFlow()
    val allBatches: Flow<List<BatchEntity>> = db.batchDao().getAllFlow()
    val allSharedGroups: Flow<List<SharedBillingGroupEntity>> = db.sharedBillingGroupDao().getAllFlow()
    val unpaidCycles: Flow<List<BillingCycleEntity>> = db.billingCycleDao().getUnpaidCyclesFlow()
    val allPayments: Flow<List<PaymentEntity>> = db.paymentDao().getAllPaymentsFlow()
    val allBreaks: Flow<List<BreakRecordEntity>> = db.breakRecordDao().getAllFlow()
    val auditLogs: Flow<List<AuditLogEntity>> = db.auditLogDao().getAllFlow()

    // ----------------- CLOUD FIRESTORE SYNC -----------------

    private fun getCurrentUserId(): String? = Firebase.auth.currentUser?.uid

    private fun syncDocToCloud(collectionName: String, docId: String, data: Map<String, Any?>) {
        val uid = getCurrentUserId() ?: return
        ioScope.launch {
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection(collectionName)
                    .document(docId)
                    .set(data, SetOptions.merge())
                    .await()
                _lastSyncTime.value = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.w(TAG, "Cloud sync failed for $collectionName/$docId: ${e.message}")
            }
        }
    }

    private fun deleteDocFromCloud(collectionName: String, docId: String) {
        val uid = getCurrentUserId() ?: return
        ioScope.launch {
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection(collectionName)
                    .document(docId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Cloud delete failed for $collectionName/$docId: ${e.message}")
            }
        }
    }

    /**
     * Download and merge all cloud data into local Room DB (e.g. when logging in on a new device)
     */
    suspend fun syncFromCloud(userId: String): Pair<Int, Int> {
        _isSyncing.value = true
        var studentsCount = 0
        var paymentsCount = 0
        try {
            val userDoc = firestore.collection("users").document(userId)

            // 1. Batches
            val batchesSnap = userDoc.collection("batches").get().await()
            for (doc in batchesSnap.documents) {
                val b = BatchEntity(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    grade = doc.getString("grade") ?: "",
                    schedule = doc.getString("schedule") ?: "",
                    defaultFee = doc.getDouble("defaultFee") ?: 0.0,
                    isArchived = doc.getBoolean("isArchived") ?: false,
                    notes = doc.getString("notes") ?: ""
                )
                db.batchDao().insert(b)
            }

            // 2. Students
            val studentsSnap = userDoc.collection("students").get().await()
            for (doc in studentsSnap.documents) {
                val s = StudentEntity(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    grade = doc.getString("grade") ?: "",
                    guardianName = doc.getString("guardianName") ?: "",
                    phone = doc.getString("phone") ?: "",
                    address = doc.getString("address") ?: "",
                    admissionDate = doc.getString("admissionDate") ?: LocalDate.now().toString(),
                    batchId = doc.getString("batchId"),
                    sharedGroupId = doc.getString("sharedGroupId"),
                    monthlyFee = doc.getDouble("monthlyFee") ?: 0.0,
                    billingDayAnchor = doc.getLong("billingDayAnchor")?.toInt() ?: 1,
                    status = doc.getString("status") ?: "ACTIVE",
                    notes = doc.getString("notes") ?: ""
                )
                db.studentDao().insert(s)
                studentsCount++
            }

            // 3. Billing Cycles
            val cyclesSnap = userDoc.collection("cycles").get().await()
            for (doc in cyclesSnap.documents) {
                val c = BillingCycleEntity(
                    id = doc.id,
                    studentId = doc.getString("studentId"),
                    sharedGroupId = doc.getString("sharedGroupId"),
                    periodStart = doc.getString("periodStart") ?: "",
                    periodEnd = doc.getString("periodEnd") ?: "",
                    dueDate = doc.getString("dueDate") ?: "",
                    originalFee = doc.getDouble("originalFee") ?: 0.0,
                    openingBalance = doc.getDouble("openingBalance") ?: 0.0,
                    adjustments = doc.getDouble("adjustments") ?: 0.0,
                    totalCharge = doc.getDouble("totalCharge") ?: 0.0,
                    allocatedAmount = doc.getDouble("allocatedAmount") ?: 0.0,
                    closingBalance = doc.getDouble("closingBalance") ?: 0.0,
                    status = doc.getString("status") ?: "UNPAID",
                    notes = doc.getString("notes") ?: ""
                )
                db.billingCycleDao().insert(c)
            }

            // 4. Payments
            val paymentsSnap = userDoc.collection("payments").get().await()
            for (doc in paymentsSnap.documents) {
                val p = PaymentEntity(
                    id = doc.id,
                    studentId = doc.getString("studentId"),
                    sharedGroupId = doc.getString("sharedGroupId"),
                    amountReceived = doc.getDouble("amountReceived") ?: 0.0,
                    paymentDate = doc.getString("paymentDate") ?: "",
                    paymentMethod = doc.getString("paymentMethod") ?: "CASH",
                    payerName = doc.getString("payerName") ?: "",
                    referenceId = doc.getString("referenceId") ?: "",
                    notes = doc.getString("notes") ?: "",
                    unallocatedCredit = doc.getDouble("unallocatedCredit") ?: 0.0,
                    isReversed = doc.getBoolean("isReversed") ?: false,
                    reversalReason = doc.getString("reversalReason")
                )
                db.paymentDao().insert(p)
                paymentsCount++
            }

            // 5. Breaks
            val breaksSnap = userDoc.collection("breaks").get().await()
            for (doc in breaksSnap.documents) {
                val brk = BreakRecordEntity(
                    id = doc.id,
                    studentId = doc.getString("studentId"),
                    batchId = doc.getString("batchId"),
                    startDate = doc.getString("startDate") ?: "",
                    endDate = doc.getString("endDate"),
                    eligibleDays = doc.getLong("eligibleDays")?.toInt() ?: 0,
                    reason = doc.getString("reason") ?: "",
                    extensionDecision = doc.getString("extensionDecision") ?: "DO_NOT_EXTEND",
                    isApplied = doc.getBoolean("isApplied") ?: false,
                    decisionDate = doc.getString("decisionDate"),
                    notes = doc.getString("notes") ?: ""
                )
                db.breakRecordDao().insert(brk)
            }

            _lastSyncTime.value = System.currentTimeMillis()
            logAudit("DATABASE", "CLOUD_SYNC", "SYNC_IN", "গুগল ক্লাউড হতে $studentsCount শিক্ষার্থী ও $paymentsCount পেমেন্ট সিঙ্ক সম্পন্ন")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing from cloud", e)
        } finally {
            _isSyncing.value = false
        }
        return Pair(studentsCount, paymentsCount)
    }

    /**
     * Upload entire local database to Firestore
     */
    suspend fun syncAllToCloud() {
        val uid = getCurrentUserId() ?: return
        _isSyncing.value = true
        try {
            val userDoc = firestore.collection("users").document(uid)

            // Save user profile metadata
            userDoc.set(
                mapOf(
                    "userId" to uid,
                    "email" to (Firebase.auth.currentUser?.email ?: ""),
                    "displayName" to (Firebase.auth.currentUser?.displayName ?: ""),
                    "lastSyncTime" to com.google.firebase.Timestamp.now()
                ),
                SetOptions.merge()
            ).await()

            // Students
            val students = db.studentDao().getAll()
            for (s in students) {
                userDoc.collection("students").document(s.id).set(
                    mapOf(
                        "id" to s.id,
                        "name" to s.name,
                        "grade" to s.grade,
                        "guardianName" to s.guardianName,
                        "phone" to s.phone,
                        "address" to s.address,
                        "admissionDate" to s.admissionDate,
                        "batchId" to s.batchId,
                        "sharedGroupId" to s.sharedGroupId,
                        "monthlyFee" to s.monthlyFee,
                        "billingDayAnchor" to s.billingDayAnchor,
                        "status" to s.status,
                        "notes" to s.notes
                    ),
                    SetOptions.merge()
                )
            }

            // Batches
            val batches = db.batchDao().getAll()
            for (b in batches) {
                userDoc.collection("batches").document(b.id).set(
                    mapOf(
                        "id" to b.id,
                        "name" to b.name,
                        "grade" to b.grade,
                        "schedule" to b.schedule,
                        "defaultFee" to b.defaultFee,
                        "notes" to b.notes
                    ),
                    SetOptions.merge()
                )
            }

            // Cycles
            val cycles = db.billingCycleDao().getAllCycles()
            for (c in cycles) {
                userDoc.collection("cycles").document(c.id).set(
                    mapOf(
                        "id" to c.id,
                        "studentId" to c.studentId,
                        "sharedGroupId" to c.sharedGroupId,
                        "periodStart" to c.periodStart,
                        "periodEnd" to c.periodEnd,
                        "dueDate" to c.dueDate,
                        "originalFee" to c.originalFee,
                        "openingBalance" to c.openingBalance,
                        "adjustments" to c.adjustments,
                        "totalCharge" to c.totalCharge,
                        "allocatedAmount" to c.allocatedAmount,
                        "closingBalance" to c.closingBalance,
                        "status" to c.status,
                        "notes" to c.notes
                    ),
                    SetOptions.merge()
                )
            }

            // Payments
            val payments = db.paymentDao().getAllPayments()
            for (p in payments) {
                userDoc.collection("payments").document(p.id).set(
                    mapOf(
                        "id" to p.id,
                        "studentId" to p.studentId,
                        "amountReceived" to p.amountReceived,
                        "paymentDate" to p.paymentDate,
                        "paymentMethod" to p.paymentMethod,
                        "payerName" to p.payerName,
                        "referenceId" to p.referenceId,
                        "unallocatedCredit" to p.unallocatedCredit,
                        "isReversed" to p.isReversed,
                        "reversalReason" to p.reversalReason,
                        "notes" to p.notes
                    ),
                    SetOptions.merge()
                )
            }

            // Breaks
            val breaks = db.breakRecordDao().getAll()
            for (brk in breaks) {
                userDoc.collection("breaks").document(brk.id).set(
                    mapOf(
                        "id" to brk.id,
                        "studentId" to brk.studentId,
                        "batchId" to brk.batchId,
                        "startDate" to brk.startDate,
                        "endDate" to brk.endDate,
                        "eligibleDays" to brk.eligibleDays,
                        "reason" to brk.reason,
                        "extensionDecision" to brk.extensionDecision,
                        "isApplied" to brk.isApplied,
                        "decisionDate" to brk.decisionDate,
                        "notes" to brk.notes
                    ),
                    SetOptions.merge()
                )
            }

            _lastSyncTime.value = System.currentTimeMillis()
            logAudit("DATABASE", "CLOUD_SYNC", "SYNC_OUT", "গুগল ক্লাউডে সম্পূর্ণ ডাটা ব্যাকআপ আপলোড সম্পন্ন")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing to cloud", e)
        } finally {
            _isSyncing.value = false
        }
    }

    // ----------------- STUDENT OPERATIONS -----------------

    suspend fun createStudent(
        name: String,
        grade: String,
        guardianName: String,
        phone: String,
        address: String,
        admissionDate: String,
        monthlyFee: Double,
        billingDayAnchor: Int,
        batchId: String? = null,
        sharedGroupId: String? = null,
        notes: String = ""
    ): StudentEntity {
        val studentId = "STU-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        val student = StudentEntity(
            id = studentId,
            name = name.trim(),
            grade = grade.trim(),
            guardianName = guardianName.trim(),
            phone = phone.trim(),
            address = address.trim(),
            admissionDate = admissionDate,
            batchId = batchId,
            sharedGroupId = sharedGroupId,
            monthlyFee = monthlyFee,
            billingDayAnchor = billingDayAnchor,
            status = "ACTIVE",
            notes = notes
        )
        db.studentDao().insert(student)

        // Generate initial billing cycle if not in a shared group
        if (sharedGroupId == null && monthlyFee > 0) {
            val start = AccountingEngine.parseDate(admissionDate)
            val end = AccountingEngine.calculateCycleEnd(start, billingDayAnchor)
            val cycleId = "CYC-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
            val initialCycle = BillingCycleEntity(
                id = cycleId,
                studentId = studentId,
                sharedGroupId = null,
                periodStart = start.format(AccountingEngine.DATE_FORMATTER),
                periodEnd = end.format(AccountingEngine.DATE_FORMATTER),
                dueDate = end.format(AccountingEngine.DATE_FORMATTER),
                originalFee = monthlyFee,
                openingBalance = 0.0,
                adjustments = 0.0,
                totalCharge = monthlyFee,
                allocatedAmount = 0.0,
                closingBalance = monthlyFee,
                status = "UNPAID",
                notes = "প্রাথমিক ভর্তি সাইকেল"
            )
            db.billingCycleDao().insert(initialCycle)

            syncDocToCloud("cycles", initialCycle.id, mapOf(
                "id" to initialCycle.id,
                "studentId" to initialCycle.studentId,
                "periodStart" to initialCycle.periodStart,
                "periodEnd" to initialCycle.periodEnd,
                "dueDate" to initialCycle.dueDate,
                "originalFee" to initialCycle.originalFee,
                "totalCharge" to initialCycle.totalCharge,
                "allocatedAmount" to initialCycle.allocatedAmount,
                "closingBalance" to initialCycle.closingBalance,
                "status" to initialCycle.status
            ))
        }

        syncDocToCloud("students", student.id, mapOf(
            "id" to student.id,
            "name" to student.name,
            "grade" to student.grade,
            "guardianName" to student.guardianName,
            "phone" to student.phone,
            "address" to student.address,
            "admissionDate" to student.admissionDate,
            "batchId" to student.batchId,
            "monthlyFee" to student.monthlyFee,
            "billingDayAnchor" to student.billingDayAnchor,
            "status" to student.status
        ))

        logAudit("STUDENT", studentId, "CREATE", "নতুন শিক্ষার্থী ভর্তি: ${student.name}")
        return student
    }

    suspend fun updateStudent(student: StudentEntity) {
        db.studentDao().update(student.copy(updatedAt = System.currentTimeMillis()))
        syncDocToCloud("students", student.id, mapOf(
            "name" to student.name,
            "grade" to student.grade,
            "guardianName" to student.guardianName,
            "phone" to student.phone,
            "monthlyFee" to student.monthlyFee,
            "billingDayAnchor" to student.billingDayAnchor,
            "status" to student.status,
            "batchId" to student.batchId
        ))
        logAudit("STUDENT", student.id, "UPDATE", "শিক্ষার্থী তথ্য হালনাগাদ: ${student.name}")
    }

    suspend fun deleteStudent(studentId: String): Boolean {
        val student = db.studentDao().getById(studentId) ?: return false
        val studentCycles = db.billingCycleDao().getCyclesForStudent(studentId)
        val studentPayments = db.paymentDao().getPaymentsForStudent(studentId)

        // 1. Delete associated data from local Room DB
        db.billingCycleDao().deleteByStudentId(studentId)
        db.paymentDao().deleteByStudentId(studentId)
        db.breakRecordDao().deleteByStudentId(studentId)
        db.feeChangeHistoryDao().deleteByStudentId(studentId)
        db.statusChangeHistoryDao().deleteByStudentId(studentId)
        db.studentDao().deleteById(studentId)

        // 2. Delete from cloud Firestore
        deleteDocFromCloud("students", studentId)
        for (c in studentCycles) {
            deleteDocFromCloud("cycles", c.id)
        }
        for (p in studentPayments) {
            deleteDocFromCloud("payments", p.id)
        }

        logAudit("STUDENT", studentId, "DELETE", "শিক্ষার্থী তথ্য স্থায়ীভাবে মুছে ফেলা হয়েছে: ${student.name}")
        return true
    }

    suspend fun changeStudentFee(studentId: String, newFee: Double, effectiveDate: String, reason: String) {
        val student = db.studentDao().getById(studentId) ?: return
        val history = FeeChangeHistoryEntity(
            id = "FEE-" + UUID.randomUUID().toString().substring(0, 8),
            studentId = studentId,
            oldFee = student.monthlyFee,
            newFee = newFee,
            effectiveDate = effectiveDate,
            reason = reason
        )
        db.feeChangeHistoryDao().insert(history)
        db.studentDao().update(student.copy(monthlyFee = newFee, updatedAt = System.currentTimeMillis()))
        syncDocToCloud("students", studentId, mapOf("monthlyFee" to newFee))
        logAudit("STUDENT", studentId, "UPDATE_FEE", "ফি পরিবর্তন: ৳ ${student.monthlyFee} -> ৳ $newFee ($reason)")
    }

    suspend fun changeStudentStatus(studentId: String, newStatus: String, effectiveDate: String, reason: String) {
        val student = db.studentDao().getById(studentId) ?: return
        val history = StatusChangeHistoryEntity(
            id = "STAT-" + UUID.randomUUID().toString().substring(0, 8),
            studentId = studentId,
            oldStatus = student.status,
            newStatus = newStatus,
            effectiveDate = effectiveDate,
            reason = reason
        )
        db.statusChangeHistoryDao().insert(history)
        db.studentDao().update(student.copy(status = newStatus, updatedAt = System.currentTimeMillis()))
        syncDocToCloud("students", studentId, mapOf("status" to newStatus))
        logAudit("STUDENT", studentId, "UPDATE_STATUS", "স্ট্যাটাস পরিবর্তন: ${student.status} -> $newStatus ($reason)")
    }

    suspend fun getStudentById(id: String): StudentEntity? = db.studentDao().getById(id)
    suspend fun getStudents(): List<StudentEntity> = db.studentDao().getAll()

    // ----------------- BATCH & SHARED GROUP OPERATIONS -----------------

    suspend fun createBatch(name: String, grade: String, schedule: String, defaultFee: Double, notes: String = ""): BatchEntity {
        val batch = BatchEntity(
            id = "BAT-" + UUID.randomUUID().toString().substring(0, 8).uppercase(),
            name = name.trim(),
            grade = grade.trim(),
            schedule = schedule.trim(),
            defaultFee = defaultFee,
            notes = notes
        )
        db.batchDao().insert(batch)
        syncDocToCloud("batches", batch.id, mapOf(
            "id" to batch.id,
            "name" to batch.name,
            "grade" to batch.grade,
            "schedule" to batch.schedule,
            "defaultFee" to batch.defaultFee
        ))
        logAudit("BATCH", batch.id, "CREATE", "নতুন ব্যাচ তৈরি: ${batch.name}")
        return batch
    }

    suspend fun updateBatch(batch: BatchEntity) {
        db.batchDao().update(batch)
        syncDocToCloud("batches", batch.id, mapOf(
            "name" to batch.name,
            "grade" to batch.grade,
            "schedule" to batch.schedule,
            "defaultFee" to batch.defaultFee
        ))
        logAudit("BATCH", batch.id, "UPDATE", "ব্যাচ হালনাগাদ: ${batch.name}")
    }

    suspend fun createSharedGroup(name: String, agreedTotalFee: Double, billingDayAnchor: Int, notes: String = ""): SharedBillingGroupEntity {
        val group = SharedBillingGroupEntity(
            id = "GRP-" + UUID.randomUUID().toString().substring(0, 8).uppercase(),
            name = name.trim(),
            agreedTotalFee = agreedTotalFee,
            billingDayAnchor = billingDayAnchor,
            notes = notes
        )
        db.sharedBillingGroupDao().insert(group)

        val today = LocalDate.now()
        val end = AccountingEngine.calculateCycleEnd(today, billingDayAnchor)
        val cycle = BillingCycleEntity(
            id = "CYC-" + UUID.randomUUID().toString().substring(0, 8).uppercase(),
            studentId = null,
            sharedGroupId = group.id,
            periodStart = today.format(AccountingEngine.DATE_FORMATTER),
            periodEnd = end.format(AccountingEngine.DATE_FORMATTER),
            dueDate = end.format(AccountingEngine.DATE_FORMATTER),
            originalFee = agreedTotalFee,
            openingBalance = 0.0,
            adjustments = 0.0,
            totalCharge = agreedTotalFee,
            allocatedAmount = 0.0,
            closingBalance = agreedTotalFee,
            status = "UNPAID",
            notes = "যৌথ বিলিং প্রাথমিক সাইকেল"
        )
        db.billingCycleDao().insert(cycle)
        logAudit("SHARED_GROUP", group.id, "CREATE", "যৌথ বিলিং গ্রুপ তৈরি: ${group.name} (৳ $agreedTotalFee)")
        return group
    }

    // ----------------- BILLING CYCLE GENERATION -----------------

    suspend fun getCyclesForStudent(studentId: String): List<BillingCycleEntity> =
        db.billingCycleDao().getCyclesForStudent(studentId)

    suspend fun getCyclesForSharedGroup(groupId: String): List<BillingCycleEntity> =
        db.billingCycleDao().getCyclesForSharedGroup(groupId)

    /**
     * CRITICAL USER RULE:
     * A student's fee cycle should only generate up to the current RUNNING month!
     * Never auto-generate advance future months!
     */
    suspend fun generateNextCycleForStudent(studentId: String): BillingCycleEntity? {
        val student = db.studentDao().getById(studentId) ?: return null
        val existingCycles = db.billingCycleDao().getCyclesForStudent(studentId)
        if (existingCycles.isEmpty()) return null

        val lastCycle = existingCycles.first() // Ordered by periodStart DESC
        val nextStart = AccountingEngine.calculateNextCycleStart(AccountingEngine.parseDate(lastCycle.periodEnd))

        // RULE: Stop! Do not generate advance future cycles beyond current date!
        val today = LocalDate.now()
        if (nextStart.isAfter(today)) {
            Log.d(TAG, "Skipping cycle generation: nextStart ($nextStart) is in advance of today ($today)")
            return null
        }

        val nextEnd = AccountingEngine.calculateCycleEnd(nextStart, student.billingDayAnchor)

        // Idempotency: verify cycle doesn't already exist for this start
        val alreadyExists = existingCycles.any { it.periodStart == nextStart.format(AccountingEngine.DATE_FORMATTER) }
        if (alreadyExists) return null

        val cycleId = "CYC-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        val nextCycle = BillingCycleEntity(
            id = cycleId,
            studentId = studentId,
            sharedGroupId = null,
            periodStart = nextStart.format(AccountingEngine.DATE_FORMATTER),
            periodEnd = nextEnd.format(AccountingEngine.DATE_FORMATTER),
            dueDate = nextEnd.format(AccountingEngine.DATE_FORMATTER),
            originalFee = student.monthlyFee,
            openingBalance = 0.0,
            adjustments = 0.0,
            totalCharge = student.monthlyFee,
            allocatedAmount = 0.0,
            closingBalance = student.monthlyFee,
            status = "UNPAID",
            notes = "চলতি মাসের সাইকেল"
        )
        db.billingCycleDao().insert(nextCycle)
        syncDocToCloud("cycles", nextCycle.id, mapOf(
            "id" to nextCycle.id,
            "studentId" to nextCycle.studentId,
            "periodStart" to nextCycle.periodStart,
            "periodEnd" to nextCycle.periodEnd,
            "dueDate" to nextCycle.dueDate,
            "originalFee" to nextCycle.originalFee,
            "totalCharge" to nextCycle.totalCharge,
            "allocatedAmount" to nextCycle.allocatedAmount,
            "closingBalance" to nextCycle.closingBalance,
            "status" to nextCycle.status
        ))
        logAudit("CYCLE", cycleId, "CREATE", "চলতি মাসের সাইকেল তৈরি: $nextStart হতে $nextEnd (শিক্ষার্থী: ${student.name})")
        return nextCycle
    }

    // ----------------- BREAK & EXTENSION OPERATIONS -----------------

    /**
     * Finds the specific cycle that matches the break start date.
     */
    suspend fun findCycleMatchingBreakDate(studentId: String, startDateStr: String): BillingCycleEntity? {
        val cycles = db.billingCycleDao().getCyclesForStudent(studentId)
        if (cycles.isEmpty()) return null

        // 1. Exact match: periodStart <= startDate <= periodEnd
        val exact = cycles.firstOrNull { c ->
            c.periodStart <= startDateStr && c.periodEnd >= startDateStr
        }
        if (exact != null) return exact

        // 2. Same year-month match
        val ym = if (startDateStr.length >= 7) startDateStr.substring(0, 7) else ""
        val monthMatch = cycles.firstOrNull { c -> c.periodStart.startsWith(ym) || c.periodEnd.startsWith(ym) }
        if (monthMatch != null) return monthMatch

        // 3. Current active/unpaid cycle or latest
        return cycles.firstOrNull { it.closingBalance > 0 } ?: cycles.firstOrNull()
    }

    /**
     * Records a break and attaches the extension strictly to the matching month's cycle.
     */
    suspend fun recordBreak(
        studentId: String?,
        batchId: String?,
        startDate: String,
        endDate: String?,
        eligibleDays: Int,
        reason: String,
        decision: String // "EXTEND", "DO_NOT_EXTEND", "DECIDE_LATER"
    ): BreakRecordEntity {
        val breakId = "BRK-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        val breakRecord = BreakRecordEntity(
            id = breakId,
            studentId = studentId,
            batchId = batchId,
            startDate = startDate,
            endDate = endDate,
            eligibleDays = eligibleDays,
            reason = reason,
            extensionDecision = decision,
            isApplied = (decision == "EXTEND"),
            decisionDate = if (decision != "DECIDE_LATER") LocalDate.now().format(AccountingEngine.DATE_FORMATTER) else null
        )
        db.breakRecordDao().insert(breakRecord)

        // If user approved extension, update the EXACT cycle matching this break's date!
        if (decision == "EXTEND" && eligibleDays > 0) {
            val studentIdsToUpdate = mutableListOf<String>()
            if (studentId != null) {
                studentIdsToUpdate.add(studentId)
            }
            if (batchId != null) {
                val batchStudents = db.studentDao().getStudentsByBatch(batchId)
                for (s in batchStudents) {
                    if (!studentIdsToUpdate.contains(s.id)) {
                        studentIdsToUpdate.add(s.id)
                    }
                }
            }

            for (sId in studentIdsToUpdate) {
                val targetCycle = findCycleMatchingBreakDate(sId, startDate)
                if (targetCycle != null) {
                    val currentEnd = AccountingEngine.parseDate(targetCycle.periodEnd)
                    val newEnd = AccountingEngine.calculateExtendedDate(currentEnd, eligibleDays)
                    val noteSuffix = " (ছুটির কারণে $eligibleDays দিন বর্ধিত)"
                    val updatedNotes = if (!targetCycle.notes.contains(noteSuffix)) {
                        (targetCycle.notes + noteSuffix).trim()
                    } else {
                        targetCycle.notes
                    }
                    val updated = targetCycle.copy(
                        periodEnd = newEnd.format(AccountingEngine.DATE_FORMATTER),
                        dueDate = newEnd.format(AccountingEngine.DATE_FORMATTER),
                        notes = updatedNotes
                    )
                    db.billingCycleDao().update(updated)

                    syncDocToCloud("cycles", updated.id, mapOf(
                        "periodEnd" to updated.periodEnd,
                        "dueDate" to updated.dueDate,
                        "notes" to updated.notes
                    ))

                    // Shift any already-generated subsequent cycles so they do not overlap
                    // and strictly maintain the full 1-month duration
                    adjustSubsequentCyclesAfterExtension(sId, newEnd, updated.id)
                }
            }
        }

        syncDocToCloud("breaks", breakRecord.id, mapOf(
            "id" to breakRecord.id,
            "studentId" to breakRecord.studentId,
            "batchId" to breakRecord.batchId,
            "startDate" to breakRecord.startDate,
            "endDate" to breakRecord.endDate,
            "eligibleDays" to breakRecord.eligibleDays,
            "reason" to breakRecord.reason,
            "extensionDecision" to breakRecord.extensionDecision,
            "isApplied" to breakRecord.isApplied
        ))

        logAudit("BREAK", breakId, "CREATE", "ছুটি/বিরতি রেকর্ড: $reason ($eligibleDays দিন, সিদ্ধান্ত: $decision)")
        return breakRecord
    }

    /**
     * Shifts any existing subsequent cycles forward after a holiday extension,
     * guaranteeing every cycle is strictly a full 1 month.
     */
    private suspend fun adjustSubsequentCyclesAfterExtension(
        studentId: String,
        extendedCycleEnd: LocalDate,
        extendedCycleId: String
    ) {
        val allCycles = db.billingCycleDao().getCyclesForStudent(studentId)
        val sortedCycles = allCycles.sortedBy { it.periodStart }
        val cycleIndex = sortedCycles.indexOfFirst { it.id == extendedCycleId }
        if (cycleIndex == -1 || cycleIndex >= sortedCycles.size - 1) return

        var previousEnd = extendedCycleEnd
        for (i in (cycleIndex + 1) until sortedCycles.size) {
            val c = sortedCycles[i]
            val newStart = AccountingEngine.calculateNextCycleStart(previousEnd)
            val newEnd = AccountingEngine.calculateCycleEnd(newStart)
            val updated = c.copy(
                periodStart = newStart.format(AccountingEngine.DATE_FORMATTER),
                periodEnd = newEnd.format(AccountingEngine.DATE_FORMATTER),
                dueDate = newEnd.format(AccountingEngine.DATE_FORMATTER)
            )
            db.billingCycleDao().update(updated)
            syncDocToCloud("cycles", updated.id, mapOf(
                "periodStart" to updated.periodStart,
                "periodEnd" to updated.periodEnd,
                "dueDate" to updated.dueDate
            ))
            previousEnd = newEnd
        }
    }

    // ----------------- PAYMENT ENTRY & ALLOCATIONS -----------------

    suspend fun recordPayment(
        studentId: String?,
        sharedGroupId: String?,
        amountReceived: Double,
        paymentDate: String,
        paymentMethod: String,
        payerName: String,
        referenceId: String,
        notes: String,
        allocations: List<Pair<String, Double>>
    ): PaymentEntity {
        require(amountReceived > 0) { "পেমেন্টের পরিমাণ ০ এর চেয়ে বেশি হতে হবে" }

        val totalAllocated = allocations.sumOf { it.second }
        require(AccountingEngine.roundMoney(totalAllocated) <= AccountingEngine.roundMoney(amountReceived)) {
            "মোট বরাদ্দকৃত টাকা পেমেন্টের পরিমাণের চেয়ে বেশি হতে পারে না"
        }

        val unallocated = AccountingEngine.roundMoney(amountReceived - totalAllocated)
        val paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).uppercase()

        val payment = PaymentEntity(
            id = paymentId,
            studentId = studentId,
            sharedGroupId = sharedGroupId,
            amountReceived = amountReceived,
            paymentDate = paymentDate,
            paymentMethod = paymentMethod,
            payerName = payerName,
            referenceId = referenceId,
            notes = notes,
            unallocatedCredit = unallocated
        )
        db.paymentDao().insert(payment)

        for ((cycleId, allocAmount) in allocations) {
            if (allocAmount <= 0) continue
            val cycle = db.billingCycleDao().getById(cycleId) ?: continue
            val newAllocated = AccountingEngine.roundMoney(cycle.allocatedAmount + allocAmount)
            val newClosing = AccountingEngine.calculateClosingBalance(
                cycle.openingBalance,
                cycle.originalFee,
                cycle.adjustments,
                newAllocated
            )
            val newStatus = when {
                newClosing <= 0.0 -> "PAID"
                newClosing < cycle.totalCharge -> "PARTIAL"
                else -> "UNPAID"
            }
            val updatedCycle = cycle.copy(
                allocatedAmount = newAllocated,
                closingBalance = newClosing,
                status = newStatus
            )
            db.billingCycleDao().update(updatedCycle)

            val allocationEntity = PaymentAllocationEntity(
                id = "ALC-" + UUID.randomUUID().toString().substring(0, 8).uppercase(),
                paymentId = paymentId,
                cycleId = cycleId,
                amount = allocAmount
            )
            db.paymentAllocationDao().insert(allocationEntity)

            syncDocToCloud("cycles", updatedCycle.id, mapOf(
                "allocatedAmount" to updatedCycle.allocatedAmount,
                "closingBalance" to updatedCycle.closingBalance,
                "status" to updatedCycle.status
            ))
        }

        syncDocToCloud("payments", payment.id, mapOf(
            "id" to payment.id,
            "studentId" to payment.studentId,
            "amountReceived" to payment.amountReceived,
            "paymentDate" to payment.paymentDate,
            "paymentMethod" to payment.paymentMethod,
            "referenceId" to payment.referenceId,
            "unallocatedCredit" to payment.unallocatedCredit,
            "isReversed" to payment.isReversed
        ))

        logAudit("PAYMENT", paymentId, "CREATE", "পেমেন্ট গ্রহণ: ৳ $amountReceived ($paymentMethod, বরাদ্দ: ৳ $totalAllocated, অগ্রিম: ৳ $unallocated)")
        return payment
    }

    suspend fun reversePayment(paymentId: String, reason: String) {
        val payment = db.paymentDao().getById(paymentId) ?: return
        if (payment.isReversed) return

        db.paymentDao().update(payment.copy(isReversed = true, reversalReason = reason))

        val allocations = db.paymentAllocationDao().getAllocationsForPayment(paymentId)
        for (alloc in allocations) {
            val cycle = db.billingCycleDao().getById(alloc.cycleId) ?: continue
            val newAllocated = AccountingEngine.roundMoney((cycle.allocatedAmount - alloc.amount).coerceAtLeast(0.0))
            val newClosing = AccountingEngine.calculateClosingBalance(
                cycle.openingBalance,
                cycle.originalFee,
                cycle.adjustments,
                newAllocated
            )
            val newStatus = when {
                newClosing <= 0.0 -> "PAID"
                newClosing < cycle.totalCharge -> "PARTIAL"
                else -> "UNPAID"
            }
            val updated = cycle.copy(
                allocatedAmount = newAllocated,
                closingBalance = newClosing,
                status = newStatus
            )
            db.billingCycleDao().update(updated)

            syncDocToCloud("cycles", updated.id, mapOf(
                "allocatedAmount" to updated.allocatedAmount,
                "closingBalance" to updated.closingBalance,
                "status" to updated.status
            ))
        }

        syncDocToCloud("payments", payment.id, mapOf(
            "isReversed" to true,
            "reversalReason" to reason
        ))

        logAudit("PAYMENT", paymentId, "REVERSE", "পেমেন্ট বাতিল/রিভার্স: ৳ ${payment.amountReceived} (কারণ: $reason)")
    }

    suspend fun checkPossibleDuplicatePayment(studentId: String, amount: Double, date: String): Boolean {
        return db.paymentDao().findDuplicate(studentId, amount, date) != null
    }

    // ----------------- RECONCILIATION & AUDIT -----------------

    data class ReconciliationReport(
        val totalCashReceived: Double,
        val totalAllocatedAmount: Double,
        val totalUnallocatedCredit: Double,
        val totalCycleCharges: Double,
        val totalOutstandingDues: Double,
        val isReconciled: Boolean,
        val discrepancyMessage: String? = null
    )

    suspend fun runReconciliation(): ReconciliationReport {
        val payments = db.paymentDao().getAllPayments().filter { !it.isReversed }
        val cycles = db.billingCycleDao().getAllCycles()
        val allocations = db.paymentAllocationDao().getAll()

        val totalCashReceived = AccountingEngine.roundMoney(payments.sumOf { it.amountReceived })
        val totalAllocated = AccountingEngine.roundMoney(allocations.sumOf { it.amount })
        val totalUnallocated = AccountingEngine.roundMoney(payments.sumOf { it.unallocatedCredit })
        val totalCharges = AccountingEngine.roundMoney(cycles.sumOf { it.totalCharge })
        val totalDues = AccountingEngine.roundMoney(cycles.sumOf { it.closingBalance })

        val paymentBreakdownSum = AccountingEngine.roundMoney(totalAllocated + totalUnallocated)
        val isPaymentBalanced = kotlin.math.abs(totalCashReceived - paymentBreakdownSum) < 0.01

        var cycleInvariantPassed = true
        var discrepancyDetail: String? = null
        for (c in cycles) {
            val expected = AccountingEngine.calculateClosingBalance(c.openingBalance, c.originalFee, c.adjustments, c.allocatedAmount)
            if (kotlin.math.abs(c.closingBalance - expected) > 0.01) {
                cycleInvariantPassed = false
                discrepancyDetail = "সাইকেল ${c.id} এর উদ্বৃত্ত অসঙ্গতিপূর্ণ: প্রত্যাশিত $expected, সংরক্ষিত ${c.closingBalance}"
                break
            }
        }

        val fullyReconciled = isPaymentBalanced && cycleInvariantPassed
        val msg = if (!isPaymentBalanced) {
            "পেমেন্ট মোট (৳ $totalCashReceived) এবং বরাদ্দ+অগ্রিমের যোগফলে (৳ $paymentBreakdownSum) পার্থক্য রয়েছে"
        } else discrepancyDetail

        return ReconciliationReport(
            totalCashReceived = totalCashReceived,
            totalAllocatedAmount = totalAllocated,
            totalUnallocatedCredit = totalUnallocated,
            totalCycleCharges = totalCharges,
            totalOutstandingDues = totalDues,
            isReconciled = fullyReconciled,
            discrepancyMessage = msg
        )
    }

    private suspend fun logAudit(entityType: String, entityId: String, action: String, details: String) {
        val log = AuditLogEntity(
            id = "AUD-" + UUID.randomUUID().toString().substring(0, 8),
            entityType = entityType,
            entityId = entityId,
            action = action,
            details = details
        )
        db.auditLogDao().insert(log)
    }

    // ----------------- BACKUP & EXPORT JSON -----------------

    suspend fun exportFullBackupJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportTime", System.currentTimeMillis())

        val studentsArray = JSONArray()
        for (s in db.studentDao().getAll()) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("grade", s.grade)
            obj.put("guardianName", s.guardianName)
            obj.put("phone", s.phone)
            obj.put("address", s.address)
            obj.put("admissionDate", s.admissionDate)
            obj.put("batchId", s.batchId ?: "")
            obj.put("monthlyFee", s.monthlyFee)
            obj.put("billingDayAnchor", s.billingDayAnchor)
            obj.put("status", s.status)
            obj.put("notes", s.notes)
            studentsArray.put(obj)
        }
        root.put("students", studentsArray)

        val batchesArray = JSONArray()
        for (b in db.batchDao().getAll()) {
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("name", b.name)
            obj.put("grade", b.grade)
            obj.put("schedule", b.schedule)
            obj.put("defaultFee", b.defaultFee)
            batchesArray.put(obj)
        }
        root.put("batches", batchesArray)

        val cyclesArray = JSONArray()
        for (c in db.billingCycleDao().getAllCycles()) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("studentId", c.studentId ?: "")
            obj.put("sharedGroupId", c.sharedGroupId ?: "")
            obj.put("periodStart", c.periodStart)
            obj.put("periodEnd", c.periodEnd)
            obj.put("dueDate", c.dueDate)
            obj.put("originalFee", c.originalFee)
            obj.put("openingBalance", c.openingBalance)
            obj.put("adjustments", c.adjustments)
            obj.put("totalCharge", c.totalCharge)
            obj.put("allocatedAmount", c.allocatedAmount)
            obj.put("closingBalance", c.closingBalance)
            obj.put("status", c.status)
            cyclesArray.put(obj)
        }
        root.put("cycles", cyclesArray)

        val paymentsArray = JSONArray()
        for (p in db.paymentDao().getAllPayments()) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("studentId", p.studentId ?: "")
            obj.put("amountReceived", p.amountReceived)
            obj.put("paymentDate", p.paymentDate)
            obj.put("paymentMethod", p.paymentMethod)
            obj.put("payerName", p.payerName)
            obj.put("referenceId", p.referenceId)
            obj.put("unallocatedCredit", p.unallocatedCredit)
            obj.put("isReversed", p.isReversed)
            paymentsArray.put(obj)
        }
        root.put("payments", paymentsArray)

        return root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Pair<Int, Int> {
        val root = JSONObject(jsonString)
        var studentsImported = 0
        var paymentsImported = 0

        if (root.has("batches")) {
            val batchesArray = root.getJSONArray("batches")
            for (i in 0 until batchesArray.length()) {
                val b = batchesArray.getJSONObject(i)
                val batch = BatchEntity(
                    id = b.getString("id"),
                    name = b.getString("name"),
                    grade = b.optString("grade", ""),
                    schedule = b.optString("schedule", ""),
                    defaultFee = b.optDouble("defaultFee", 0.0)
                )
                db.batchDao().insert(batch)
            }
        }

        if (root.has("students")) {
            val studentsArray = root.getJSONArray("students")
            for (i in 0 until studentsArray.length()) {
                val s = studentsArray.getJSONObject(i)
                val student = StudentEntity(
                    id = s.getString("id"),
                    name = s.getString("name"),
                    grade = s.optString("grade", ""),
                    guardianName = s.optString("guardianName", ""),
                    phone = s.optString("phone", ""),
                    address = s.optString("address", ""),
                    admissionDate = s.optString("admissionDate", LocalDate.now().toString()),
                    batchId = if (s.optString("batchId").isNotBlank()) s.getString("batchId") else null,
                    monthlyFee = s.optDouble("monthlyFee", 0.0),
                    billingDayAnchor = s.optInt("billingDayAnchor", 1),
                    status = s.optString("status", "ACTIVE")
                )
                db.studentDao().insert(student)
                studentsImported++
            }
        }

        if (root.has("cycles")) {
            val cyclesArray = root.getJSONArray("cycles")
            for (i in 0 until cyclesArray.length()) {
                val c = cyclesArray.getJSONObject(i)
                val cycle = BillingCycleEntity(
                    id = c.getString("id"),
                    studentId = if (c.optString("studentId").isNotBlank()) c.getString("studentId") else null,
                    sharedGroupId = if (c.optString("sharedGroupId").isNotBlank()) c.getString("sharedGroupId") else null,
                    periodStart = c.getString("periodStart"),
                    periodEnd = c.getString("periodEnd"),
                    dueDate = c.getString("dueDate"),
                    originalFee = c.getDouble("originalFee"),
                    openingBalance = c.optDouble("openingBalance", 0.0),
                    adjustments = c.optDouble("adjustments", 0.0),
                    totalCharge = c.getDouble("totalCharge"),
                    allocatedAmount = c.optDouble("allocatedAmount", 0.0),
                    closingBalance = c.getDouble("closingBalance"),
                    status = c.getString("status")
                )
                db.billingCycleDao().insert(cycle)
            }
        }

        if (root.has("payments")) {
            val paymentsArray = root.getJSONArray("payments")
            for (i in 0 until paymentsArray.length()) {
                val p = paymentsArray.getJSONObject(i)
                val payment = PaymentEntity(
                    id = p.getString("id"),
                    studentId = if (p.optString("studentId").isNotBlank()) p.getString("studentId") else null,
                    amountReceived = p.getDouble("amountReceived"),
                    paymentDate = p.getString("paymentDate"),
                    paymentMethod = p.optString("paymentMethod", "CASH"),
                    payerName = p.optString("payerName", ""),
                    referenceId = p.optString("referenceId", ""),
                    unallocatedCredit = p.optDouble("unallocatedCredit", 0.0),
                    isReversed = p.optBoolean("isReversed", false)
                )
                db.paymentDao().insert(payment)
                paymentsImported++
            }
        }

        logAudit("DATABASE", "BACKUP_RESTORE", "RESTORE", "ব্যাকআপ হতে $studentsImported শিক্ষার্থী ও $paymentsImported পেমেন্ট রিস্টোর করা হয়েছে")
        return Pair(studentsImported, paymentsImported)
    }
}
