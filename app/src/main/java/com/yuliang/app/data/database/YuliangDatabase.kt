package com.yuliang.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MonthlyPlanEntity::class, CategoryEntity::class, TransactionEntity::class, FixedExpenseTemplateEntity::class, FixedExpenseInstanceEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class YuliangDatabase : RoomDatabase() {
    abstract fun monthlyPlanDao(): MonthlyPlanDao
    abstract fun categoryDao(): CategoryDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun fixedExpenseDao(): FixedExpenseDao

    companion object {
        fun create(context: Context): YuliangDatabase = Room.databaseBuilder(
            context.applicationContext, YuliangDatabase::class.java, "yuliang.db"
        ).build()
    }
}
