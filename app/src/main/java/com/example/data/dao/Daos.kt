package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(student: StudentEntity)

    @Update
    suspend fun update(student: StudentEntity)

    @Delete
    suspend fun delete(student: StudentEntity)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getById(id: String): StudentEntity?

    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllFlow(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students ORDER BY name ASC")
    suspend fun getAll(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE status = 'ACTIVE' ORDER BY name ASC")
    fun getActiveStudentsFlow(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE batchId = :batchId ORDER BY name ASC")
    fun getStudentsByBatchFlow(batchId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE batchId = :batchId")
    suspend fun getStudentsByBatch(batchId: String): List<StudentEntity>

    @Query("SELECT * FROM students WHERE sharedGroupId = :groupId")
    suspend fun getStudentsBySharedGroup(groupId: String): List<StudentEntity>

    @Query("SELECT * FROM students WHERE name LIKE '%' || :query || '%' OR guardianName LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%'")
    suspend fun searchStudents(query: String): List<StudentEntity>
}

@Dao
interface BatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(batch: BatchEntity)

    @Update
    suspend fun update(batch: BatchEntity)

    @Query("SELECT * FROM batches WHERE id = :id")
    suspend fun getById(id: String): BatchEntity?

    @Query("SELECT * FROM batches ORDER BY name ASC")
    fun getAllFlow(): Flow<List<BatchEntity>>

    @Query("SELECT * FROM batches ORDER BY name ASC")
    suspend fun getAll(): List<BatchEntity>
}

@Dao
interface SharedBillingGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: SharedBillingGroupEntity)

    @Update
    suspend fun update(group: SharedBillingGroupEntity)

    @Query("SELECT * FROM shared_billing_groups WHERE id = :id")
    suspend fun getById(id: String): SharedBillingGroupEntity?

    @Query("SELECT * FROM shared_billing_groups ORDER BY name ASC")
    fun getAllFlow(): Flow<List<SharedBillingGroupEntity>>

    @Query("SELECT * FROM shared_billing_groups")
    suspend fun getAll(): List<SharedBillingGroupEntity>
}

@Dao
interface BillingCycleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cycle: BillingCycleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cycles: List<BillingCycleEntity>)

    @Update
    suspend fun update(cycle: BillingCycleEntity)

    @Query("SELECT * FROM billing_cycles WHERE id = :id")
    suspend fun getById(id: String): BillingCycleEntity?

    @Query("SELECT * FROM billing_cycles WHERE studentId = :studentId ORDER BY periodStart DESC")
    fun getCyclesForStudentFlow(studentId: String): Flow<List<BillingCycleEntity>>

    @Query("SELECT * FROM billing_cycles WHERE studentId = :studentId ORDER BY periodStart DESC")
    suspend fun getCyclesForStudent(studentId: String): List<BillingCycleEntity>

    @Query("SELECT * FROM billing_cycles WHERE sharedGroupId = :groupId ORDER BY periodStart DESC")
    suspend fun getCyclesForSharedGroup(groupId: String): List<BillingCycleEntity>

    @Query("SELECT * FROM billing_cycles WHERE closingBalance > 0 ORDER BY dueDate ASC")
    fun getUnpaidCyclesFlow(): Flow<List<BillingCycleEntity>>

    @Query("SELECT * FROM billing_cycles WHERE closingBalance > 0 ORDER BY dueDate ASC")
    suspend fun getUnpaidCycles(): List<BillingCycleEntity>

    @Query("SELECT * FROM billing_cycles ORDER BY periodStart DESC")
    fun getAllCyclesFlow(): Flow<List<BillingCycleEntity>>

    @Query("SELECT * FROM billing_cycles ORDER BY periodStart DESC")
    suspend fun getAllCycles(): List<BillingCycleEntity>

    @Query("DELETE FROM billing_cycles WHERE studentId = :studentId")
    suspend fun deleteByStudentId(studentId: String)
}

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentEntity)

    @Update
    suspend fun update(payment: PaymentEntity)

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getById(id: String): PaymentEntity?

    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    fun getAllPaymentsFlow(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    suspend fun getAllPayments(): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE studentId = :studentId AND isReversed = 0 ORDER BY paymentDate DESC")
    suspend fun getPaymentsForStudent(studentId: String): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE sharedGroupId = :groupId AND isReversed = 0 ORDER BY paymentDate DESC")
    suspend fun getPaymentsForSharedGroup(groupId: String): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE studentId = :studentId AND amountReceived = :amount AND paymentDate = :date AND isReversed = 0 LIMIT 1")
    suspend fun findDuplicate(studentId: String, amount: Double, date: String): PaymentEntity?

    @Query("DELETE FROM payments WHERE studentId = :studentId")
    suspend fun deleteByStudentId(studentId: String)
}

@Dao
interface PaymentAllocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(allocation: PaymentAllocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(allocations: List<PaymentAllocationEntity>)

    @Query("SELECT * FROM payment_allocations WHERE paymentId = :paymentId")
    suspend fun getAllocationsForPayment(paymentId: String): List<PaymentAllocationEntity>

    @Query("SELECT * FROM payment_allocations WHERE cycleId = :cycleId")
    suspend fun getAllocationsForCycle(cycleId: String): List<PaymentAllocationEntity>

    @Query("SELECT * FROM payment_allocations")
    suspend fun getAll(): List<PaymentAllocationEntity>
}

@Dao
interface BreakRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(breakRecord: BreakRecordEntity)

    @Update
    suspend fun update(breakRecord: BreakRecordEntity)

    @Query("SELECT * FROM break_records WHERE studentId = :studentId ORDER BY startDate DESC")
    fun getBreaksForStudentFlow(studentId: String): Flow<List<BreakRecordEntity>>

    @Query("SELECT * FROM break_records WHERE studentId = :studentId ORDER BY startDate DESC")
    suspend fun getBreaksForStudent(studentId: String): List<BreakRecordEntity>

    @Query("SELECT * FROM break_records ORDER BY startDate DESC")
    fun getAllFlow(): Flow<List<BreakRecordEntity>>

    @Query("SELECT * FROM break_records ORDER BY startDate DESC")
    suspend fun getAll(): List<BreakRecordEntity>

    @Query("DELETE FROM break_records WHERE studentId = :studentId")
    suspend fun deleteByStudentId(studentId: String)
}

@Dao
interface FeeChangeHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: FeeChangeHistoryEntity)

    @Query("SELECT * FROM fee_change_history WHERE studentId = :studentId ORDER BY effectiveDate DESC")
    suspend fun getHistoryForStudent(studentId: String): List<FeeChangeHistoryEntity>

    @Query("SELECT * FROM fee_change_history WHERE studentId = :studentId ORDER BY effectiveDate DESC")
    fun getHistoryForStudentFlow(studentId: String): Flow<List<FeeChangeHistoryEntity>>

    @Query("DELETE FROM fee_change_history WHERE studentId = :studentId")
    suspend fun deleteByStudentId(studentId: String)
}

@Dao
interface StatusChangeHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: StatusChangeHistoryEntity)

    @Query("SELECT * FROM status_change_history WHERE studentId = :studentId ORDER BY effectiveDate DESC")
    suspend fun getHistoryForStudent(studentId: String): List<StatusChangeHistoryEntity>

    @Query("SELECT * FROM status_change_history WHERE studentId = :studentId ORDER BY effectiveDate DESC")
    fun getHistoryForStudentFlow(studentId: String): Flow<List<StatusChangeHistoryEntity>>

    @Query("DELETE FROM status_change_history WHERE studentId = :studentId")
    suspend fun deleteByStudentId(studentId: String)
}

@Dao
interface AuditLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllFlow(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    suspend fun getAll(): List<AuditLogEntity>
}

@Dao
interface AppSettingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(setting: AppSettingEntity)

    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    suspend fun get(key: String): String?
}
