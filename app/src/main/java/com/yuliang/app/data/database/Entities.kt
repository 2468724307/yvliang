package com.yuliang.app.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_plans", indices = [Index(value = ["year", "month"], unique = true)])
data class MonthlyPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val year: Int,
    val month: Int,
    val baseIncomeCents: Long,
    val savingGoalCents: Long,
    val safetyReserveCents: Long = 0,
    val effectiveStartEpochDay: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "categories", indices = [Index(value = ["name", "type"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val type: String,
    val sortOrder: Int,
    val isSystem: Boolean,
    val isEnabled: Boolean = true,
)

@Entity(
    tableName = "fixed_expense_templates",
    foreignKeys = [ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("categoryId")],
)
data class FixedExpenseTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountCents: Long,
    val dueDay: Int,
    val categoryId: Long? = null,
    val repeatMonthly: Boolean = true,
    val isEnabled: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "fixed_expense_instances",
    foreignKeys = [ForeignKey(entity = FixedExpenseTemplateEntity::class, parentColumns = ["id"], childColumns = ["templateId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["templateId", "year", "month"], unique = true), Index("linkedTransactionId")],
)
data class FixedExpenseInstanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val year: Int,
    val month: Int,
    val status: String = "UPCOMING",
    val linkedTransactionId: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("occurredAt"), Index("categoryId"), Index("linkedFixedExpenseId", unique = true)],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amountCents: Long,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val note: String? = null,
    val occurredAt: Long,
    val incomeAllocation: String? = null,
    val linkedFixedExpenseId: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
