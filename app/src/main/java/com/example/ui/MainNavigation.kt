package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.accounting.AccountingEngine
import com.example.data.entity.StudentEntity
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.TuitionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    viewModel: TuitionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedStudentId by viewModel.selectedStudentId.collectAsState()
    val students by viewModel.students.collectAsState()
    val batches by viewModel.batches.collectAsState()
    val unpaidCycles by viewModel.unpaidCycles.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val reconciliationReport by viewModel.reconciliationReport.collectAsState()
    val useBengaliNumerals by viewModel.useBengaliNumerals.collectAsState()
    val statusMessage by viewModel.actionStatusMessage.collectAsState()

    val currentUser by viewModel.currentUser.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    // Dialog state holders
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var paymentPreselectedStudentId by remember { mutableStateOf<String?>(null) }
    var showMoreMenuSheet by remember { mutableStateOf(false) }
    var showGoogleAccountDialog by remember { mutableStateOf(false) }

    // Student specific dialogs
    var showChangeFeeDialog by remember { mutableStateOf(false) }
    var showRecordBreakDialog by remember { mutableStateOf(false) }
    var showChangeStatusDialog by remember { mutableStateOf(false) }

    val activeStudent = remember(selectedStudentId, students) {
        students.firstOrNull { it.id == selectedStudentId }
    }

    // Back handling
    if (currentScreen != AppScreen.DASHBOARD) {
        BackHandler {
            if (currentScreen == AppScreen.STUDENT_DETAIL) {
                viewModel.navigateTo(AppScreen.STUDENTS)
            } else {
                viewModel.navigateTo(AppScreen.DASHBOARD)
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Tablet/Wide Navigation Rail
            if (isWideScreen) {
                NavigationRail(
                    header = {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    },
                    modifier = Modifier.fillMaxHeight()
                ) {
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.DASHBOARD,
                        onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "হোম") },
                        label = { Text("হোম") }
                    )
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.STUDENTS || currentScreen == AppScreen.STUDENT_DETAIL,
                        onClick = { viewModel.navigateTo(AppScreen.STUDENTS) },
                        icon = { Icon(Icons.Default.School, contentDescription = "শিক্ষার্থী") },
                        label = { Text("শিক্ষার্থী") }
                    )
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.PAYMENTS,
                        onClick = { viewModel.navigateTo(AppScreen.PAYMENTS) },
                        icon = { Icon(Icons.Default.Receipt, contentDescription = "পেমেন্ট") },
                        label = { Text("পেমেন্ট") }
                    )
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.DUE_TRACKER,
                        onClick = { viewModel.navigateTo(AppScreen.DUE_TRACKER) },
                        icon = { Icon(Icons.Default.HourglassTop, contentDescription = "বকেয়া") },
                        label = { Text("বকেয়া") }
                    )
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.BATCHES,
                        onClick = { viewModel.navigateTo(AppScreen.BATCHES) },
                        icon = { Icon(Icons.Default.Groups, contentDescription = "ব্যাচ") },
                        label = { Text("ব্যাচ") }
                    )
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.REPORTS,
                        onClick = { viewModel.navigateTo(AppScreen.REPORTS) },
                        icon = { Icon(Icons.Default.Analytics, contentDescription = "রিপোর্ট") },
                        label = { Text("রিপোর্ট") }
                    )
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.CALENDAR,
                        onClick = { viewModel.navigateTo(AppScreen.CALENDAR) },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "ক্যালেন্ডার") },
                        label = { Text("ক্যালেন্ডার") }
                    )
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.RECONCILIATION,
                        onClick = { viewModel.navigateTo(AppScreen.RECONCILIATION) },
                        icon = { Icon(Icons.Default.Shield, contentDescription = "সমন্বয়") },
                        label = { Text("সমন্বয়") }
                    )
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.BACKUP,
                        onClick = { viewModel.navigateTo(AppScreen.BACKUP) },
                        icon = { Icon(Icons.Default.Backup, contentDescription = "ব্যাকআপ") },
                        label = { Text("ব্যাকআপ") }
                    )
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.SETTINGS,
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "সেটিংস") },
                        label = { Text("সেটিংস") }
                    )
                }
            }

            // Main Scaffold
            Scaffold(
                topBar = {
                    if (currentScreen != AppScreen.STUDENT_DETAIL) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Transparent,
                            shadowElevation = 4.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(DeepPurple, VibrantPurple, RoyalIndigo)
                                        )
                                    )
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Title & App Branding
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White.copy(alpha = 0.22f),
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Image(
                                                    painter = painterResource(id = R.drawable.ic_nra_logo),
                                                    contentDescription = "NRA Logo",
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "NRA Education Center",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 17.sp
                                                ),
                                                color = Color.White,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "${currentScreen.title} • টিউশন হিসাব",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.sp
                                                ),
                                                color = Color.White.copy(alpha = 0.85f),
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    // Right controls: Google Cloud Account & Help
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Google Sign-In / Cloud Sync Badge Chip
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (currentUser != null) Color.White.copy(alpha = 0.22f) else Color.White,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .clickable { showGoogleAccountDialog = true }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (isSyncing) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(14.dp),
                                                        strokeWidth = 2.dp,
                                                        color = if (currentUser != null) Color.White else DeepPurple
                                                    )
                                                    Text(
                                                        text = "সিঙ্ক...",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (currentUser != null) Color.White else DeepPurple
                                                    )
                                                } else if (currentUser != null) {
                                                    Icon(
                                                        imageVector = Icons.Default.CloudDone,
                                                        contentDescription = "ক্লাউড সিঙ্ক হয়েছে",
                                                        tint = Color(0xFF69F0AE),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = currentUser?.displayName?.take(8) ?: "গুগল আইডি",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.CloudSync,
                                                        contentDescription = "গুগল লগইন",
                                                        tint = DeepPurple,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = "গুগল ব্যাকআপ",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = DeepPurple
                                                    )
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = { viewModel.navigateTo(AppScreen.HELP) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.HelpOutline,
                                                contentDescription = "সাহায্য",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.DASHBOARD,
                                onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                                icon = { Icon(Icons.Default.Home, contentDescription = "হোম") },
                                label = { Text("হোম") }
                            )
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.STUDENTS || currentScreen == AppScreen.STUDENT_DETAIL,
                                onClick = { viewModel.navigateTo(AppScreen.STUDENTS) },
                                icon = { Icon(Icons.Default.School, contentDescription = "শিক্ষার্থী") },
                                label = { Text("শিক্ষার্থী") }
                            )
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.PAYMENTS,
                                onClick = { viewModel.navigateTo(AppScreen.PAYMENTS) },
                                icon = { Icon(Icons.Default.Receipt, contentDescription = "পেমেন্ট") },
                                label = { Text("পেমেন্ট") }
                            )
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.DUE_TRACKER,
                                onClick = { viewModel.navigateTo(AppScreen.DUE_TRACKER) },
                                icon = { Icon(Icons.Default.HourglassTop, contentDescription = "বকেয়া") },
                                label = { Text("বকেয়া") }
                            )
                            NavigationBarItem(
                                selected = currentScreen in listOf(
                                    AppScreen.BATCHES, AppScreen.REPORTS, AppScreen.CALENDAR,
                                    AppScreen.RECONCILIATION, AppScreen.BACKUP, AppScreen.SETTINGS, AppScreen.HELP
                                ),
                                onClick = { showMoreMenuSheet = true },
                                icon = { Icon(Icons.Default.Menu, contentDescription = "আরও") },
                                label = { Text("আরও") }
                            )
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                modifier = Modifier
                    .weight(1f)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) { contentPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            })
                        }
                ) {
                    when (currentScreen) {
                        AppScreen.DASHBOARD -> DashboardScreen(
                            metrics = metrics,
                            students = students,
                            unpaidCycles = unpaidCycles,
                            recentPayments = payments,
                            useBengaliNumerals = useBengaliNumerals,
                            onNavigate = { viewModel.navigateTo(it) },
                            onSelectStudent = { viewModel.selectStudent(it) },
                            onOpenAddStudent = { showAddStudentDialog = true },
                            onOpenAddPayment = {
                                paymentPreselectedStudentId = null
                                showAddPaymentDialog = true
                            }
                        )

                        AppScreen.STUDENTS -> StudentsScreen(
                            students = students,
                            unpaidCycles = unpaidCycles,
                            searchQuery = "",
                            useBengaliNumerals = useBengaliNumerals,
                            onSelectStudent = { viewModel.selectStudent(it) },
                            onOpenAddStudent = { showAddStudentDialog = true },
                            onDeleteStudent = { stuId ->
                                viewModel.deleteStudent(stuId)
                            }
                        )

                        AppScreen.STUDENT_DETAIL -> {
                            if (activeStudent != null) {
                                val studentCycles = unpaidCycles.filter { it.studentId == activeStudent.id }
                                val studentPayments = payments.filter { it.studentId == activeStudent.id }
                                val studentBreaks = emptyList<com.example.data.entity.BreakRecordEntity>() // Loaded on demand

                                StudentDetailScreen(
                                    student = activeStudent,
                                    cycles = studentCycles,
                                    payments = studentPayments,
                                    breaks = studentBreaks,
                                    auditLogs = auditLogs.filter { it.entityId == activeStudent.id },
                                    useBengaliNumerals = useBengaliNumerals,
                                    onBack = { viewModel.navigateTo(AppScreen.STUDENTS) },
                                    onOpenAddPayment = {
                                        paymentPreselectedStudentId = activeStudent.id
                                        showAddPaymentDialog = true
                                    },
                                    onOpenChangeFee = { showChangeFeeDialog = true },
                                    onOpenRecordBreak = { showRecordBreakDialog = true },
                                    onOpenChangeStatus = { showChangeStatusDialog = true },
                                    onGenerateNextCycle = { viewModel.generateNextCycle(activeStudent.id) },
                                    onDeleteStudent = {
                                        viewModel.deleteStudent(activeStudent.id) {
                                            viewModel.navigateTo(AppScreen.STUDENTS)
                                        }
                                    }
                                )
                            } else {
                                viewModel.navigateTo(AppScreen.STUDENTS)
                            }
                        }

                        AppScreen.BATCHES -> BatchesScreen(
                            batches = batches,
                            students = students,
                            unpaidCycles = unpaidCycles,
                            useBengaliNumerals = useBengaliNumerals,
                            onAddBatch = { name, grade, schedule, fee, notes ->
                                viewModel.addBatch(name, grade, schedule, fee, notes)
                            },
                            onSelectStudent = { viewModel.selectStudent(it) }
                        )

                        AppScreen.PAYMENTS -> PaymentsScreen(
                            payments = payments,
                            students = students,
                            useBengaliNumerals = useBengaliNumerals,
                            onOpenAddPayment = {
                                paymentPreselectedStudentId = null
                                showAddPaymentDialog = true
                            },
                            onReversePayment = { pid, reason ->
                                viewModel.reversePayment(pid, reason)
                            }
                        )

                        AppScreen.DUE_TRACKER -> DueTrackerScreen(
                            unpaidCycles = unpaidCycles,
                            students = students,
                            useBengaliNumerals = useBengaliNumerals,
                            onSelectStudent = { viewModel.selectStudent(it) },
                            onOpenAddPaymentForStudent = { stuId ->
                                paymentPreselectedStudentId = stuId
                                showAddPaymentDialog = true
                            }
                        )

                        AppScreen.REPORTS -> ReportsScreen(
                            students = students,
                            batches = batches,
                            unpaidCycles = unpaidCycles,
                            payments = payments,
                            useBengaliNumerals = useBengaliNumerals
                        )

                        AppScreen.CALENDAR -> CalendarScreen(
                            unpaidCycles = unpaidCycles,
                            students = students,
                            useBengaliNumerals = useBengaliNumerals,
                            onSelectStudent = { viewModel.selectStudent(it) }
                        )

                        AppScreen.RECONCILIATION -> ReconciliationScreen(
                            report = reconciliationReport,
                            useBengaliNumerals = useBengaliNumerals,
                            onRunReconciliation = { viewModel.runReconciliation() }
                        )

                        AppScreen.BACKUP -> BackupScreen(
                            onExportBackup = { callback -> viewModel.exportBackup(callback) },
                            onRestoreBackup = { json -> viewModel.restoreBackup(json) }
                        )

                        AppScreen.SETTINGS -> SettingsScreen(
                            useBengaliNumerals = useBengaliNumerals,
                            onToggleBengaliNumerals = { viewModel.toggleBengaliNumerals() }
                        )

                        AppScreen.HELP -> HelpScreen()
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for "More" Navigation items on mobile
    if (showMoreMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreMenuSheet = false },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "অন্যান্য মেনু ও সুবিধা",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                listOf(
                    Triple(AppScreen.BATCHES, "ব্যাচ ব্যবস্থাপনা", Icons.Default.Groups),
                    Triple(AppScreen.REPORTS, "আর্থিক রিপোর্ট ও খতিয়ান", Icons.Default.Analytics),
                    Triple(AppScreen.CALENDAR, "ক্যালেন্ডার ও নোঙর তারিখ", Icons.Default.CalendarMonth),
                    Triple(AppScreen.RECONCILIATION, "হিসাব নিরীক্ষা ও সমন্বয়", Icons.Default.Shield),
                    Triple(AppScreen.BACKUP, "ডাটা ব্যাকআপ ও এক্সপোর্ট", Icons.Default.Backup),
                    Triple(AppScreen.SETTINGS, "সেটিংস ও পছন্দসমূহ", Icons.Default.Settings),
                    Triple(AppScreen.HELP, "ব্যবহার নির্দেশিকা ও নিয়মাবলী", Icons.Default.HelpOutline)
                ).forEach { (screen, title, icon) ->
                    ListItem(
                        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
                        leadingContent = { Icon(icon, contentDescription = null, tint = PurplePrimary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showMoreMenuSheet = false
                                viewModel.navigateTo(screen)
                            }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Global Dialogs
    if (showAddStudentDialog) {
        AddStudentDialog(
            batches = batches,
            onDismiss = { showAddStudentDialog = false },
            onConfirm = { name, grade, guardian, phone, address, admDate, fee, anchor, batchId, notes ->
                viewModel.addStudent(name, grade, guardian, phone, address, admDate, fee, anchor, batchId, notes)
                showAddStudentDialog = false
            }
        )
    }

    if (showAddPaymentDialog) {
        AddPaymentDialog(
            students = students,
            unpaidCycles = unpaidCycles,
            preSelectedStudentId = paymentPreselectedStudentId,
            onDismiss = {
                showAddPaymentDialog = false
                paymentPreselectedStudentId = null
            },
            onConfirm = { stuId, amount, date, method, payer, ref, notes, allocations ->
                viewModel.recordPayment(stuId, null, amount, date, method, payer, ref, notes, allocations)
                showAddPaymentDialog = false
                paymentPreselectedStudentId = null
            }
        )
    }

    if (showChangeFeeDialog && activeStudent != null) {
        ChangeFeeDialog(
            currentFee = activeStudent.monthlyFee,
            onDismiss = { showChangeFeeDialog = false },
            onConfirm = { newFee, date, reason ->
                viewModel.changeStudentFee(activeStudent.id, newFee, date, reason)
                showChangeFeeDialog = false
            }
        )
    }

    if (showRecordBreakDialog && activeStudent != null) {
        RecordBreakDialog(
            studentName = activeStudent.name,
            currentCycleEndDate = null,
            onDismiss = { showRecordBreakDialog = false },
            onConfirm = { start, end, days, reason, decision ->
                viewModel.recordBreak(activeStudent.id, null, start, end, days, reason, decision)
                showRecordBreakDialog = false
            }
        )
    }

    if (showChangeStatusDialog && activeStudent != null) {
        AlertDialog(
            onDismissRequest = { showChangeStatusDialog = false },
            title = { Text("স্ট্যাটাস পরিবর্তন") },
            text = {
                Column {
                    Text("বর্তমান স্ট্যাটাস: ${activeStudent.status}")
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(
                        "ACTIVE" to "সক্রিয় (Active)",
                        "ON_BREAK" to "ছুটি বা বিরতিতে (On Break)",
                        "INACTIVE" to "পড়াশোনা বন্ধ / নিষ্ক্রিয় (Inactive)"
                    ).forEach { (st, label) ->
                        TextButton(
                            onClick = {
                                viewModel.changeStudentStatus(activeStudent.id, st, java.time.LocalDate.now().toString(), "ব্যবহারকারী কর্তৃক পরিবর্তন")
                                showChangeStatusDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showChangeStatusDialog = false }) { Text("বাতিল") }
            }
        )
    }

    if (showGoogleAccountDialog) {
        GoogleAccountDialog(
            currentUser = currentUser,
            isSyncing = isSyncing,
            cloudSyncStatus = cloudSyncStatus,
            onDismiss = { showGoogleAccountDialog = false },
            onSignIn = {
                var currentContext = context
                var foundActivity: android.app.Activity? = null
                while (currentContext is android.content.ContextWrapper) {
                    if (currentContext is android.app.Activity) {
                        foundActivity = currentContext
                        break
                    }
                    currentContext = currentContext.baseContext
                }
                foundActivity?.let { act ->
                    viewModel.signInWithGoogle(act) { success ->
                        if (success) {
                            showGoogleAccountDialog = false
                        }
                    }
                }
            },
            onSignOut = {
                viewModel.signOut(context)
            },
            onSyncAll = {
                viewModel.syncAllToCloud()
            },
            onRestore = {
                viewModel.syncFromCloud()
            }
        )
    }
}

@Composable
fun GoogleAccountDialog(
    currentUser: com.google.firebase.auth.FirebaseUser?,
    isSyncing: Boolean,
    cloudSyncStatus: String?,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSyncAll: () -> Unit,
    onRestore: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PurplePrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = PurplePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "গুগল ক্লাউড ব্যাকআপ",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (currentUser != null) {
                    // Logged in View
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PaidGreenBg),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = PaidGreen,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = currentUser.displayName?.firstOrNull()?.uppercase() ?: "G",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser.displayName ?: "গুগল ব্যবহারকারী",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = currentUser.email ?: "সংযুক্ত গুগল একাউন্ট",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = PaidGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "গুগল ক্লাউডে ডাটা সংরক্ষিত",
                                        fontSize = 11.sp,
                                        color = PaidGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "📱 মোবাইল পরিবর্তন বা অ্যাপ আনইনস্টল করলেও কোনো হিসাব হারাবে না। নতুন ফোনে এই একই গুগল একাউন্টে লগইন করলেই সমস্ত ডাটা স্বয়ংক্রিয়ভাবে ফিরে আসবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    if (cloudSyncStatus != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = cloudSyncStatus,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = PurplePrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sync & Restore buttons
                    FilledTonalButton(
                        onClick = onSyncAll,
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ক্লাউডে সেভ হচ্ছে...")
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("এখনই সব তথ্য ক্লাউডে সেভ করুন")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onRestore,
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ক্লাউড হতে তথ্য রিফ্রেশ / রিস্টোর করুন")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = OverdueRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("লগআউট করুন", color = OverdueRed)
                    }
                } else {
                    // Not Logged in View
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DueOrangeBg),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = DueOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "মোবাইল পরিবর্তনের ঝুঁকি এড়ান",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DueOrange
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "বর্তমানে ডাটা কেবল এই ডিভাইসে সংরক্ষিত। মোবাইল পরিবর্তন বা অ্যাপ মুছে গেলে যেন ডাটা না হারায়, সেজন্য গুগল একাউন্টে লগইন করুন।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "গুগল লগইন করলে কী কী সুবিধা পাবেন:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    listOf(
                        "মোবাইল পাল্টালেও সব ছাত্র, পেমেন্ট ও বকেয়ার হিসাব অক্ষত থাকবে।",
                        "যেকোনো নতুন ডিভাইসে এক ক্লিকে সম্পূর্ণ হিসাব পুনরুদ্ধার।",
                        "কোনো ম্যানুয়াল ফাইল ব্যাকআপ নেওয়ার প্রয়োজন নেই।"
                    ).forEach { feature ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = PaidGreen, modifier = Modifier.size(16.dp))
                            Text(text = feature, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onSignIn,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google একাউন্ট দিয়ে লগইন করুন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

