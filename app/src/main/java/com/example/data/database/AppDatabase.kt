package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.*
import com.example.data.entity.*

@Database(
    entities = [
        StudentEntity::class,
        BatchEntity::class,
        SharedBillingGroupEntity::class,
        BillingCycleEntity::class,
        PaymentEntity::class,
        PaymentAllocationEntity::class,
        BreakRecordEntity::class,
        FeeChangeHistoryEntity::class,
        StatusChangeHistoryEntity::class,
        AuditLogEntity::class,
        AppSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun batchDao(): BatchDao
    abstract fun sharedBillingGroupDao(): SharedBillingGroupDao
    abstract fun billingCycleDao(): BillingCycleDao
    abstract fun paymentDao(): PaymentDao
    abstract fun paymentAllocationDao(): PaymentAllocationDao
    abstract fun breakRecordDao(): BreakRecordDao
    abstract fun feeChangeHistoryDao(): FeeChangeHistoryDao
    abstract fun statusChangeHistoryDao(): StatusChangeHistoryDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tuition_accounting.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
