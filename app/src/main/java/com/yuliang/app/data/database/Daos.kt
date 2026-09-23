package com.yuliang.app.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyPlanDao {
    @Query("SELECT * FROM monthly_plans WHERE year=:year AND month=:month LIMIT 1")
    fun observe(year: Int, month: Int): Flow<MonthlyPlanEntity?>
    @Query("SELECT * FROM monthly_plans WHERE year=:year AND month=:month LIMIT 1")
    suspend fun get(year: Int, month: Int): MonthlyPlanEntity?
    @Upsert suspend fun upsert(plan: MonthlyPlanEntity): Long
    @Delete suspend fun delete(plan: MonthlyPlanEntity)
    @Query("SELECT * FROM monthly_plans ORDER BY year, month") suspend fun getAll(): List<MonthlyPlanEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAll(items: List<MonthlyPlanEntity>)
    @Query("DELETE FROM monthly_plans") suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isEnabled=1 ORDER BY sortOrder, id") fun observeEnabled(): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM categories ORDER BY sortOrder, id") fun observeAll(): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM categories ORDER BY sortOrder, id") suspend fun getAll(): List<CategoryEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(category: CategoryEntity): Long
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAll(items: List<CategoryEntity>)
    @Query("UPDATE categories SET isEnabled=0 WHERE id=:id") suspend fun archive(id: Long)
    @Query("DELETE FROM categories") suspend fun deleteAll()
}

@Dao
abstract class LedgerDao {
    @Insert abstract suspend fun insertTransaction(transaction: TransactionEntity): Long
    @Update abstract suspend fun updateTransaction(transaction: TransactionEntity)
    @Delete abstract suspend fun deleteTransaction(transaction: TransactionEntity)
    @Query("SELECT * FROM transactions WHERE occurredAt BETWEEN :start AND :end ORDER BY occurredAt DESC")
    abstract fun observeTransactions(start: Long, end: Long): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC, id DESC") abstract fun observeAll(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions ORDER BY occurredAt, id") abstract suspend fun getAll(): List<TransactionEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT) abstract suspend fun insertAll(items: List<TransactionEntity>)
    @Query("SELECT * FROM transactions WHERE id=:id") abstract suspend fun getTransaction(id: Long): TransactionEntity?
    @Query("DELETE FROM transactions WHERE id=:id") abstract suspend fun deleteById(id: Long): Int
    @Query("DELETE FROM transactions") abstract suspend fun deleteAll()
    @Query("SELECT COUNT(*) FROM transactions") abstract suspend fun countTransactions(): Int
}

@Dao
abstract class FixedExpenseDao {
    @Insert abstract suspend fun insertTemplate(template: FixedExpenseTemplateEntity): Long
    @Update abstract suspend fun updateTemplate(template: FixedExpenseTemplateEntity)
    @Query("SELECT * FROM fixed_expense_templates WHERE isEnabled=1 ORDER BY dueDay, id")
    abstract fun observeTemplates(): Flow<List<FixedExpenseTemplateEntity>>
    @Query("SELECT * FROM fixed_expense_templates WHERE isEnabled=1 AND repeatMonthly=1 ORDER BY dueDay, id")
    abstract suspend fun getRepeatingTemplates(): List<FixedExpenseTemplateEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun insertInstance(instance: FixedExpenseInstanceEntity): Long
    @Query("SELECT * FROM fixed_expense_instances WHERE year=:year AND month=:month ORDER BY id")
    abstract fun observeInstances(year: Int, month: Int): Flow<List<FixedExpenseInstanceEntity>>
    @Query("SELECT * FROM fixed_expense_instances WHERE id=:id") abstract suspend fun getInstance(id: Long): FixedExpenseInstanceEntity?
    @Query("SELECT * FROM fixed_expense_templates WHERE id=:id") abstract suspend fun getTemplate(id: Long): FixedExpenseTemplateEntity?
    @Query("SELECT * FROM fixed_expense_templates ORDER BY id") abstract suspend fun getAllTemplates(): List<FixedExpenseTemplateEntity>
    @Query("SELECT * FROM fixed_expense_instances ORDER BY year, month, id") abstract suspend fun getAllInstances(): List<FixedExpenseInstanceEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT) abstract suspend fun insertTemplates(items: List<FixedExpenseTemplateEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) abstract suspend fun insertInstances(items: List<FixedExpenseInstanceEntity>)
    @Query("DELETE FROM fixed_expense_instances") abstract suspend fun deleteAllInstances()
    @Query("DELETE FROM fixed_expense_templates") abstract suspend fun deleteAllTemplates()
    @Query("UPDATE fixed_expense_instances SET status=:status, updatedAt=:updatedAt WHERE id=:id")
    abstract suspend fun setStatus(id: Long, status: String, updatedAt: Long)
    @Query("UPDATE fixed_expense_instances SET status='SKIPPED', updatedAt=:updatedAt WHERE id=:id AND status='UPCOMING' AND linkedTransactionId IS NULL")
    abstract suspend fun skipIfUpcoming(id: Long, updatedAt: Long): Int
    @Query("UPDATE fixed_expense_instances SET status='PAID', linkedTransactionId=:transactionId, updatedAt=:updatedAt WHERE id=:id AND status='UPCOMING' AND linkedTransactionId IS NULL")
    abstract suspend fun linkPaidIfUpcoming(id: Long, transactionId: Long, updatedAt: Long): Int

}
