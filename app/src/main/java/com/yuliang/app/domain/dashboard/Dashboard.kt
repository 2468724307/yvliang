package com.yuliang.app.domain.dashboard

import com.yuliang.app.domain.budget.BudgetCalculator
import com.yuliang.app.domain.budget.FixedExpenseManager
import com.yuliang.app.domain.model.*
import java.time.LocalDate
import java.time.ZoneId

sealed interface DashboardResult {
    data object NoPlan : DashboardResult
    data class Ready(
        val budget: BudgetResult,
        val recentTransactions: List<Transaction>,
        val monthSpentCents: Long,
        val planProgress: Float,
    ) : DashboardResult
}

class DashboardUseCase(private val calculator: BudgetCalculator = BudgetCalculator()) {
    fun execute(
        plan: MonthlyPlan?,
        transactions: List<Transaction>,
        fixedExpenses: List<FixedExpense>,
        today: LocalDate,
        zoneId: ZoneId,
    ): DashboardResult {
        if (plan == null) return DashboardResult.NoPlan
        val budget = calculator.calculate(plan, transactions, fixedExpenses, today, zoneId)
        val spent = transactions.filter(FixedExpenseManager::isCountedVariableExpense)
            .fold(0L) { total, item -> Math.addExact(total, item.amountCents) }
        val denominator = budget.monthlySpendableCents.coerceAtLeast(1)
        return DashboardResult.Ready(
            budget = budget,
            recentTransactions = transactions.sortedByDescending(Transaction::occurredAt).take(5),
            monthSpentCents = spent,
            planProgress = (spent.toDouble() / denominator.toDouble()).toFloat().coerceIn(0f, 1f),
        )
    }
}

data class ExpenseImpact(val remainingCents: Long, val overspendCents: Long)

object TransactionImpactCalculator {
    fun afterExpense(todayRemainingCents: Long, amountCents: Long): ExpenseImpact {
        require(todayRemainingCents >= 0 && amountCents > 0)
        val delta = Math.subtractExact(todayRemainingCents, amountCents)
        return ExpenseImpact(delta.coerceAtLeast(0), (-delta).coerceAtLeast(0))
    }
}

data class CategorySlice(val categoryId: Long?, val label: String, val amountCents: Long, val fraction: Float)
data class DailySpend(val date: LocalDate, val amountCents: Long)

sealed interface StatisticsResult {
    data object Empty : StatisticsResult
    data class Content(
        val totalExpenseCents: Long,
        val categorySlices: List<CategorySlice>,
        val dailyTrend: List<DailySpend>,
        val topCategory: String,
        val recentDailyAverageCents: Long,
        val prediction: Prediction?,
        val savingGoalOnTrack: Boolean?,
        val riskLevel: BudgetRiskLevel?,
    ) : StatisticsResult
}

class StatisticsCalculator {
    fun calculate(
        transactions: List<Transaction>,
        categoryNames: Map<Long, String>,
        budget: BudgetResult?,
        today: LocalDate,
        zoneId: ZoneId,
    ): StatisticsResult {
        val expenses = transactions.filter(FixedExpenseManager::isCountedVariableExpense)
        if (expenses.isEmpty()) return StatisticsResult.Empty
        val total = expenses.fold(0L) { sum, item -> Math.addExact(sum, item.amountCents) }
        val byCategory = expenses.groupBy(Transaction::categoryId).mapValues { (_, items) -> items.fold(0L) { sum, item -> Math.addExact(sum, item.amountCents) } }
        val slices = byCategory.entries.sortedByDescending { it.value }.map { (categoryId, amount) ->
            CategorySlice(categoryId, categoryId?.let(categoryNames::get) ?: "未分类", amount, (amount.toDouble() / total.toDouble()).toFloat())
        }
        val byDay = expenses.groupBy { it.date(zoneId) }.mapValues { (_, items) -> items.sumOf(Transaction::amountCents) }
        val monthStart = today.withDayOfMonth(1)
        val dailyTrend = (0 until today.dayOfMonth).map { offset ->
            val date = monthStart.plusDays(offset.toLong())
            DailySpend(date, byDay[date] ?: 0)
        }
        val activeRecentDays = byDay.entries.filter { !it.key.isAfter(today) }.sortedBy { it.key }.takeLast(7)
        val average = if (activeRecentDays.isEmpty()) 0 else activeRecentDays.sumOf { it.value } / activeRecentDays.size
        return StatisticsResult.Content(total, slices, dailyTrend, slices.first().label, average, budget?.prediction, budget?.savingGoalOnTrack, budget?.budgetRiskLevel)
    }
}
