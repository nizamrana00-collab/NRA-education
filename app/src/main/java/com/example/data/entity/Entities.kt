package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val grade: String,
    val guardianName: String,
    val phone: String,
    val address: String,
    val admissionDate: String, // YYYY-MM-DD
    val batchId: String? = null,
    val sharedGroupId: String? = null,
    val monthlyFee: Double,
    val billingDayAnchor: Int, // 1 to 31
    val status: String = "ACTIVE", // ACTIVE, ON_BREAK, INACTIVE
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "batches")
data class BatchEntity(
    @PrimaryKey val id: String,
    val name: String,
    val grade: String,
    val schedule: String,
    val defaultFee: Double,
    val isArchived: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "shared_billing_groups")
data class SharedBillingGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val agreedTotalFee: Double,
    val billingDayAnchor: Int,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "billing_cycles",
    indices = [
        Index(value = ["studentId"]),
        Index(value = ["sharedGroupId"]),
        Index(value = ["dueDate"]),
        Index(value = ["status"])
    ]
)
data class BillingCycleEntity(
    @PrimaryKey val id: String,
    val studentId: String? = null,
    val sharedGroupId: String? = null,
    val periodStart: String, // YYYY-MM-DD
    val periodEnd: String,   // YYYY-MM-DD
    val dueDate: String,     // YYYY-MM-DD
    val originalFee: Double,
    val openingBalance: Double = 0.0,
    val adjustments: Double = 0.0,
    val totalCharge: Double, // originalFee + openingBalance + adjustments
    val allocatedAmount: Double = 0.0,
    val closingBalance: Double, // totalCharge - allocatedAmount
    val status: String, // UPCOMING, UNPAID, PARTIAL, PAID, OVERDUE, ADJUSTED
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["studentId"]),
        Index(value = ["sharedGroupId"]),
        Index(value = ["paymentDate"])
    ]
)
data class PaymentEntity(
    @PrimaryKey val id: String,
    val studentId: String? = null,
    val sharedGroupId: String? = null,
    val amountReceived: Double,
    val paymentDate: String, // YYYY-MM-DD or YYYY-MM-DD HH:mm
    val paymentMethod: String, // CASH, BKASH, NAGAD, BANK_TRANSFER, OTHER
    val payerName: String = "",
    val referenceId: String = "",
    val notes: String = "",
    val unallocatedCredit: Double = 0.0,
    val isReversed: Boolean = false,
    val reversalReason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "payment_allocations",
    indices = [
        Index(value = ["paymentId"]),
        Index(value = ["cycleId"])
    ]
)
data class PaymentAllocationEntity(
    @PrimaryKey val id: String,
    val paymentId: String,
    val cycleId: String,
    val amount: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "break_records",
    indices = [
        Index(value = ["studentId"]),
        Index(value = ["batchId"])
    ]
)
data class BreakRecordEntity(
    @PrimaryKey val id: String,
    val studentId: String? = null,
    val batchId: String? = null,
    val startDate: String,
    val endDate: String? = null,
    val eligibleDays: Int = 0,
    val reason: String = "",
    val extensionDecision: String = "DO_NOT_EXTEND", // EXTEND, DO_NOT_EXTEND, DECIDE_LATER
    val isApplied: Boolean = false,
    val decisionDate: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "fee_change_history",
    indices = [Index(value = ["studentId"])]
)
data class FeeChangeHistoryEntity(
    @PrimaryKey val id: String,
    val studentId: String,
    val oldFee: Double,
    val newFee: Double,
    val effectiveDate: String,
    val reason: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "status_change_history",
    indices = [Index(value = ["studentId"])]
)
data class StatusChangeHistoryEntity(
    @PrimaryKey val id: String,
    val studentId: String,
    val oldStatus: String,
    val newStatus: String,
    val effectiveDate: String,
    val reason: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val action: String, // CREATE, UPDATE, DELETE, REVERSE, EXTEND
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
