package com.yuliang.app.domain.model

import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId

enum class TransactionType { EXPENSE, INCOME, TRANSFER }
enum class IncomeAllocation { SPENDABLE, SAVING, RECORD_ONLY }
enum class FixedExpenseStatus { UPCOMING, PAID, SKIPPED }
enum class BudgetRiskLevel { SAFE, WARNING, RISK }

data class MonthlyPlan(
    val id: Long = 0,
    val year: Int,
    val month: Int,
    val baseIncomeCents: Long,
    val savingGoalCents: Long,
    val safetyReserveCents: Long,
    val effectiveStartDate: LocalDate,
)

data class Transaction(
    val id: Long = 0,
    val type: TransactionType,
    val amountCents: Long,
    val occurredAt: Instant,
    val incomeAllocation: IncomeAllocation? = null,
    val linkedFixedExpenseId: Long? = null,
    val categoryId: Long? = null,
    val note: String? = null,
) { fun date(zoneId: ZoneId): LocalDate = occurredAt.atZone(zoneId).toLocalDate() }

data class FixedExpense(
    val instanceId: Long,
    val templateId: Long,
    val name: String,
    val amountCents: Long,
    val dueDay: Int,
    val status: FixedExpenseStatus,
    val linkedTransactionId: Long? = null,
)

sealed interface Prediction {
    data class Available(val projectedMonthEndBalanceCents: Long, val sampleDays: Int) : Prediction
    data class InsufficientData(val sampleDays: Int) : Prediction
}

data class BudgetResult(
    val monthlySpendableCents: Long,
    val monthRemainingCents: Long,
    val todayAllowanceCents: Long,
    val todayRemainingCents: Long,
    val todayOverspendCents: Long,
    val weekAllowanceCents: Long,
    val weekRemainingCents: Long,
    val prediction: Prediction,
    val savingGoalOnTrack: Boolean,
    val budgetRiskLevel: BudgetRiskLevel,
)
