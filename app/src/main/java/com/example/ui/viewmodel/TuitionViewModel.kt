package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accounting.AccountingEngine
import com.example.accounting.MonthlyReportEngine
import com.example.accounting.LifetimeFinancialSummary
import com.example.auth.AuthManager
import com.example.data.entity.*
import com.example.data.repository.TuitionRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class AppScreen(val title: String) {
    DASHBOARD("হোম"),
    STUDENTS("শিক্ষার্থী"),
    STUDENT_DETAIL("শিক্ষার্থীর হিসাব"),
    BATCHES("ব্যাচ"),
    PAYMENTS("পেমেন্ট"),
    DUE_TRACKER("বকেয়া ট্র্যাকার"),
    REPORTS("রিপোর্ট"),
    CALENDAR("ক্যালেন্ডার"),
    RECONCILIATION("হিসাব সমন্বয়"),
    BACKUP("ব্যাকআপ ও এক্সপোর্ট"),
    SETTINGS("সেটিংস"),
    HELP("সাহায্য ও নিয়মাবলী")
}

data class DashboardMetrics(
    val totalStudents: Int = 0,
    val activeStudents: Int = 0,
    val onBreakStudents: Int = 0,
    val inactiveStudents: Int = 0,
    val currentMonthCharged: Double = 0.0,
    val currentMonthReceived: Double = 0.0,
    val totalOutstandingDues: Double = 0.0,
    val totalAdvanceCredit: Double = 0.0,
    val overdueCount: Int = 0,
    val overdueAmount: Double = 0.0,
    val dueNext7DaysCount: Int = 0,
    val dueNext7DaysAmount: Double = 0.0,
    val partiallyPaidCount: Int = 0
)

class TuitionViewModel(private val repository: TuitionRepository) : ViewModel() {

    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedStudentId = MutableStateFlow<String?>(null)
    val selectedStudentId: StateFlow<String?> = _selectedStudentId.asStateFlow()

    private val _selectedBatchId = MutableStateFlow<String?>(null)
    val selectedBatchId: StateFlow<String?> = _selectedBatchId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _useBengaliNumerals = MutableStateFlow(false)
    val useBengaliNumerals: StateFlow<Boolean> = _useBengaliNumerals.asStateFlow()

    // Auth & Cloud Sync States
    val currentUser: StateFlow<FirebaseUser?> = AuthManager.currentUser
    val isSigningIn: StateFlow<Boolean> = AuthManager.isSigningIn
    val authError: StateFlow<String?> = AuthManager.authError
    val isSyncing: StateFlow<Boolean> = repository.isSyncing
    val lastSyncTime: StateFlow<Long?> = repository.lastSyncTime

    val cloudSyncStatus: StateFlow<String?> = combine(
        currentUser,
        isSyncing,
        lastSyncTime
    ) { user, syncing, lastSync ->
        when {
            syncing -> "গুগল ক্লাউডে ডাটা সিঙ্ক হচ্ছে..."
            user != null && lastSync != null -> "সর্বশেষ ক্লাউড ব্যাকআপ: " + java.time.Instant.ofEpochMilli(lastSync).atZone(java.time.ZoneId.systemDefault()).toLocalTime().toString().take(5)
            user != null -> "গুগল ক্লাউডে ডাটা সংরক্ষিত"
            else -> null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val students: StateFlow<List<StudentEntity>> = repository.allStudents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val batches: StateFlow<List<BatchEntity>> = repository.allBatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unpaidCycles: StateFlow<List<BillingCycleEntity>> = repository.unpaidCycles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<PaymentEntity>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lifetimeFinancialSummary: StateFlow<LifetimeFinancialSummary> = combine(
        payments,
        students,
        useBengaliNumerals
    ) { payList, stuList, bengaliNumerals ->
        MonthlyReportEngine.generateHistoricalReports(payList, stuList, bengaliNumerals)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        LifetimeFinancialSummary(0.0, 0, 0, 0, emptyList())
    )

    private val _reconciliationReport = MutableStateFlow<TuitionRepository.ReconciliationReport?>(null)
    val reconciliationReport: StateFlow<TuitionRepository.ReconciliationReport?> = _reconciliationReport.asStateFlow()

    private val _actionStatusMessage = MutableStateFlow<String?>(null)
    val actionStatusMessage: StateFlow<String?> = _actionStatusMessage.asStateFlow()

    // Computed Dashboard Metrics Flow
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        students,
        unpaidCycles,
        payments
    ) { stuList, cycles, payList ->
        val today = LocalDate.now()
        val currentYearMonth = String.format("%04d-%02d", today.year, today.monthValue)

        val totalStudents = stuList.size
        val active = stuList.count { it.status == "ACTIVE" }
        val onBreak = stuList.count { it.status == "ON_BREAK" }
        val inactive = stuList.count { it.status == "INACTIVE" }

        val currentMonthCharged = cycles
            .filter { it.periodStart.startsWith(currentYearMonth) }
            .sumOf { it.totalCharge }

        val currentMonthReceived = payList
            .filter { !it.isReversed && it.paymentDate.startsWith(currentYearMonth) }
            .sumOf { it.amountReceived }

        val totalOutstandingDues = cycles.sumOf { it.closingBalance }
        val totalAdvance = payList.filter { !it.isReversed }.sumOf { it.unallocatedCredit }

        val overdueCycles = cycles.filter {
            try {
                AccountingEngine.parseDate(it.dueDate).isBefore(today)
            } catch (e: Exception) { false }
        }
        val overdueCount = overdueCycles.size
        val overdueAmount = overdueCycles.sumOf { it.closingBalance }

        val next7 = today.plusDays(7)
        val dueNext7Cycles = cycles.filter {
            try {
                val d = AccountingEngine.parseDate(it.dueDate)
                !d.isBefore(today) && !d.isAfter(next7)
            } catch (e: Exception) { false }
        }
        val dueNext7Count = dueNext7Cycles.size
        val dueNext7Amount = dueNext7Cycles.sumOf { it.closingBalance }

        val partialCount = cycles.count { it.status == "PARTIAL" }

        DashboardMetrics(
            totalStudents = totalStudents,
            activeStudents = active,
            onBreakStudents = onBreak,
            inactiveStudents = inactive,
            currentMonthCharged = AccountingEngine.roundMoney(currentMonthCharged),
            currentMonthReceived = AccountingEngine.roundMoney(currentMonthReceived),
            totalOutstandingDues = AccountingEngine.roundMoney(totalOutstandingDues),
            totalAdvanceCredit = AccountingEngine.roundMoney(totalAdvance),
            overdueCount = overdueCount,
            overdueAmount = AccountingEngine.roundMoney(overdueAmount),
            dueNext7DaysCount = dueNext7Count,
            dueNext7DaysAmount = AccountingEngine.roundMoney(dueNext7Amount),
            partiallyPaidCount = partialCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    init {
        AuthManager.initAuthStateListener()
        runReconciliation()
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectStudent(studentId: String) {
        _selectedStudentId.value = studentId
        _currentScreen.value = AppScreen.STUDENT_DETAIL
    }

    fun selectBatch(batchId: String) {
        _selectedBatchId.value = batchId
        _currentScreen.value = AppScreen.BATCHES
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleBengaliNumerals() {
        _useBengaliNumerals.value = !_useBengaliNumerals.value
    }

    fun clearStatusMessage() {
        _actionStatusMessage.value = null
    }

    // Google Sign-In & Cloud Sync Actions
    fun signInWithGoogle(activity: Activity, onComplete: (Boolean) -> Unit = {}) {
        AuthManager.signInWithGoogle(
            activity = activity,
            scope = viewModelScope,
            onSuccess = { user ->
                _actionStatusMessage.value = "গুগল অ্যাকাউন্টে সাইন-ইন সফল: ${user.displayName ?: user.email}"
                // Automatically restore/sync any existing data from Google Cloud
                viewModelScope.launch {
                    val (stuCount, payCount) = repository.syncFromCloud(user.uid)
                    if (stuCount > 0 || payCount > 0) {
                        _actionStatusMessage.value = "ক্লাউড হতে $stuCount শিক্ষার্থী ও $payCount পেমেন্ট পুনরুদ্ধার হয়েছে!"
                    } else {
                        // First time: upload local data to cloud
                        repository.syncAllToCloud()
                    }
                    runReconciliation()
                    onComplete(true)
                }
            },
            onError = { err ->
                _actionStatusMessage.value = err
                onComplete(false)
            }
        )
    }

    fun signOut(context: Context) {
        AuthManager.signOut(context, viewModelScope) {
            _actionStatusMessage.value = "গুগল অ্যাকাউন্ট থেকে সাইন-আউট করা হয়েছে।"
        }
    }

    fun syncAllToCloud() {
        val user = currentUser.value
        if (user == null) {
            _actionStatusMessage.value = "ক্লাউডে সেভ করতে প্রথমে গুগল অ্যাকাউন্ট দিয়ে সাইন-ইন করুন।"
            return
        }
        viewModelScope.launch {
            repository.syncAllToCloud()
            _actionStatusMessage.value = "সকল হিসাব গুগল ক্লাউডে নিরাপদে সেভ হয়েছে!"
        }
    }

    fun syncFromCloud() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val (stu, pay) = repository.syncFromCloud(user.uid)
            _actionStatusMessage.value = "গুগল ক্লাউড হতে $stu শিক্ষার্থী ও $pay পেমেন্ট সফলভাবে লোড হয়েছে!"
            runReconciliation()
        }
    }

    // Student Actions
    fun addStudent(
        name: String,
        grade: String,
        guardianName: String,
        phone: String,
        address: String,
        admissionDate: String,
        monthlyFee: Double,
        billingDayAnchor: Int,
        batchId: String?,
        notes: String = ""
    ) {
        viewModelScope.launch {
            try {
                val student = repository.createStudent(
                    name = name,
                    grade = grade,
                    guardianName = guardianName,
                    phone = phone,
                    address = address,
                    admissionDate = admissionDate,
                    monthlyFee = monthlyFee,
                    billingDayAnchor = billingDayAnchor,
                    batchId = batchId,
                    notes = notes
                )
                _actionStatusMessage.value = "শিক্ষার্থী ${student.name} সফলভাবে যুক্ত হয়েছে!"
                runReconciliation()
            } catch (e: Exception) {
                _actionStatusMessage.value = "ত্রুটি: ${e.message}"
            }
        }
    }

    fun deleteStudent(studentId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val success = repository.deleteStudent(studentId)
                if (success) {
                    _actionStatusMessage.value = "শিক্ষার্থী সফলভাবে মুছে ফেলা হয়েছে!"
                    runReconciliation()
                    onSuccess()
                } else {
                    _actionStatusMessage.value = "শিক্ষার্থী খুঁজে পাওয়া যায়নি"
                }
            } catch (e: Exception) {
                _actionStatusMessage.value = "মোছার সময় ত্রুটি: ${e.message}"
            }
        }
    }

    fun changeStudentFee(studentId: String, newFee: Double, effectiveDate: String, reason: String) {
        viewModelScope.launch {
            try {
                repository.changeStudentFee(studentId, newFee, effectiveDate, reason)
                _actionStatusMessage.value = "ফি সফলভাবে পরিবর্তন করা হয়েছে!"
                runReconciliation()
            } catch (e: Exception) {
                _actionStatusMessage.value = "ত্রুটি: ${e.message}"
            }
        }
    }

    fun changeStudentStatus(studentId: String, newStatus: String, effectiveDate: String, reason: String) {
        viewModelScope.launch {
            try {
                repository.changeStudentStatus(studentId, newStatus, effectiveDate, reason)
                _actionStatusMessage.value = "স্ট্যাটাস সফলভাবে পরিবর্তন করা হয়েছে!"
            } catch (e: Exception) {
                _actionStatusMessage.value = "ত্রুটি: ${e.message}"
            }
        }
    }

    // Batch Actions
    fun addBatch(name: String, grade: String, schedule: String, defaultFee: Double, notes: String = "") {
        viewModelScope.launch {
            try {
                val b = repository.createBatch(name, grade, schedule, defaultFee, notes)
                _actionStatusMessage.value = "ব্যাচ '${b.name}' সফলভাবে যুক্ত হয়েছে!"
            } catch (e: Exception) {
                _actionStatusMessage.value = "ত্রুটি: ${e.message}"
            }
        }
    }

    // Break & Vacation Action with Auto Month-Matching
    fun recordBreak(
        studentId: String?,
        batchId: String?,
        startDate: String,
        endDate: String?,
        eligibleDays: Int,
        reason: String,
        decision: String
    ) {
        viewModelScope.launch {
            try {
                repository.recordBreak(studentId, batchId, startDate, endDate, eligibleDays, reason, decision)
                _actionStatusMessage.value = "ছুটি/বিরতি নির্ধারিত মাসের সাইকেলে সফলভাবে যুক্ত হয়েছে!"
                runReconciliation()
            } catch (e: Exception) {
                _actionStatusMessage.value = "ত্রুটি: ${e.message}"
            }
        }
    }

    suspend fun getCycleMatchingBreakDate(studentId: String, startDateStr: String): BillingCycleEntity? {
        return repository.findCycleMatchingBreakDate(studentId, startDateStr)
    }

    // Payment Action
    fun recordPayment(
        studentId: String?,
        sharedGroupId: String?,
        amountReceived: Double,
        paymentDate: String,
        paymentMethod: String,
        payerName: String,
        referenceId: String,
        notes: String,
        allocations: List<Pair<String, Double>>
    ) {
        viewModelScope.launch {
            try {
                val payment = repository.recordPayment(
                    studentId = studentId,
                    sharedGroupId = sharedGroupId,
                    amountReceived = amountReceived,
                    paymentDate = paymentDate,
                    paymentMethod = paymentMethod,
                    payerName = payerName,
                    referenceId = referenceId,
                    notes = notes,
                    allocations = allocations
                )
                _actionStatusMessage.value = "পেমেন্ট সফলভাবে রেকর্ড হয়েছে (৳ ${payment.amountReceived})!"
                runReconciliation()
            } catch (e: Exception) {
                _actionStatusMessage.value = "ত্রুটি: ${e.message}"
            }
        }
    }

    fun reversePayment(paymentId: String, reason: String) {
        viewModelScope.launch {
            try {
                repository.reversePayment(paymentId, reason)
                _actionStatusMessage.value = "পেমেন্ট বাতিল করা হয়েছে!"
                runReconciliation()
            } catch (e: Exception) {
                _actionStatusMessage.value = "ত্রুটি: ${e.message}"
            }
        }
    }

    /**
     * Rule: Only generate cycles up to the running current month!
     */
    fun generateNextCycle(studentId: String) {
        viewModelScope.launch {
            try {
                val cycle = repository.generateNextCycleForStudent(studentId)
                if (cycle != null) {
                    _actionStatusMessage.value = "চলতি মাসের সাইকেল সফলভাবে তৈরি হয়েছে (${cycle.periodStart} হতে ${cycle.periodEnd})!"
                } else {
                    _actionStatusMessage.value = "পরবর্তী সাইকেল তৈরি হয়নি: নিয়ম অনুযায়ী অগ্রিম ভবিষ্যৎ মাসের সাইকেল আগাম তৈরি হবে না, শুধুমাত্র চলতি রানিং মাস শুরু হলেই স্বয়ংক্রিয়ভাবে তৈরি হবে।"
                }
                runReconciliation()
            } catch (e: Exception) {
                _actionStatusMessage.value = "ত্রুটি: ${e.message}"
            }
        }
    }

    fun runReconciliation() {
        viewModelScope.launch {
            val report = repository.runReconciliation()
            _reconciliationReport.value = report
        }
    }

    // Backup & Restore
    fun exportBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = repository.exportFullBackupJson()
                onResult(json)
                _actionStatusMessage.value = "ব্যাকআপ সফলভাবে এক্সপোর্ট হয়েছে!"
            } catch (e: Exception) {
                _actionStatusMessage.value = "এক্সপোর্ট ব্যর্থ: ${e.message}"
            }
        }
    }

    fun restoreBackup(jsonString: String) {
        viewModelScope.launch {
            try {
                val (studentsCount, paymentsCount) = repository.importBackupJson(jsonString)
                _actionStatusMessage.value = "সফলভাবে $studentsCount জন শিক্ষার্থী ও $paymentsCount টি পেমেন্ট রিস্টোর হয়েছে!"
                runReconciliation()
            } catch (e: Exception) {
                _actionStatusMessage.value = "রিস্টোর ব্যর্থ: ${e.message}"
            }
        }
    }
}
