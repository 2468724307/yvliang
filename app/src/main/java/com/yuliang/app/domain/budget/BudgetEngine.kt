package com.yuliang.app.domain.budget

import com.yuliang.app.domain.model.*
import java.time.*
import java.time.temporal.TemporalAdjusters
import kotlin.math.max

object FixedExpenseManager {
    fun frozenTotal(expenses: List<FixedExpense>): Long = expenses
        .filter { it.status != FixedExpenseStatus.SKIPPED }
        .sumCents { it.amountCents }

    fun isCountedVariableExpense(transaction: Transaction): Boolean =
        transaction.type == TransactionType.EXPENSE && transaction.linkedFixedExpenseId == null
}

object SavingGoalCalculator {
    fun isOnTrack(projectedDisposableCents: Long): Boolean = projectedDisposableCents >= 0
}

class BalancePredictor {
    fun predict(
        countedExpenses: List<Transaction>,
        currentDate: LocalDate,
        monthEnd: LocalDate,
        monthRemainingCents: Long,
        zoneId: ZoneId,
    ): Prediction {
        val byDay = countedExpenses
            .filter { !it.date(zoneId).isAfter(currentDate) }
            .groupBy { it.date(zoneId) }
            .mapValues { (_, values) -> values.sumCents { it.amountCents } }
            .toSortedMap()
        if (byDay.size < 3) return Prediction.InsufficientData(byDay.size)
        val sample = byDay.entries.toList().takeLast(7)
        val average = sample.sumCents { it.value } / sample.size
        val futureDays = max(0, (monthEnd.toEpochDay() - currentDate.toEpochDay()).toInt())
        return Prediction.Available(safeSubtract(monthRemainingCents, safeMultiply(average, futureDays.toLong())), sample.size)
    }
}

class BudgetCalculator(private val predictor: BalancePredictor = BalancePredictor()) {
    fun calculate(
        plan: MonthlyPlan,
        transactions: List<Transaction>,
        fixedExpenses: List<FixedExpense>,
        currentDate: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): BudgetResult {
        validate(plan, transactions, fixedExpenses, currentDate)
        val monthStart = LocalDate.of(plan.year, plan.month, 1)
        val monthEnd = monthStart.with(TemporalAdjusters.lastDayOfMonth())
        val monthTransactions = transactions.filter { it.date(zoneId) in monthStart..monthEnd }
        val spendableIncome = monthTransactions.filter {
            it.type == TransactionType.INCOME && it.incomeAllocation == IncomeAllocation.SPENDABLE
        }.sumCents { it.amountCents }
        val frozen = FixedExpenseManager.frozenTotal(fixedExpenses)
        val monthlySpendable = safeSubtract(
            safeAdd(plan.baseIncomeCents, spendableIncome),
            safeAdd(safeAdd(plan.savingGoalCents, plan.safetyReserveCents), frozen),
        )
        val countedExpenses = monthTransactions.filter(FixedExpenseManager::isCountedVariableExpense)
        val spent = countedExpenses.sumCents { it.amountCents }
        val monthRemaining = safeSubtract(monthlySpendable, spent)

        val expensesBeforeToday = countedExpenses.filter { it.date(zoneId).isBefore(currentDate) }.sumCents { it.amountCents }
        val todaySpent = countedExpenses.filter { it.date(zoneId) == currentDate }.sumCents { it.amountCents }
        val allocationStart = maxOf(plan.effectiveStartDate, currentDate)
        val remainingDays = if (allocationStart > monthEnd) 0 else daysInclusive(allocationStart, monthEnd)
        val beforeTodayRemaining = max(0, safeSubtract(monthlySpendable, expensesBeforeToday))
        val todayAllowance = if (currentDate < plan.effectiveStartDate || remainingDays == 0L) 0 else
            allocatedBetween(beforeTodayRemaining, allocationStart, monthEnd, currentDate, currentDate)
        val todayDelta = safeSubtract(todayAllowance, todaySpent)

        val weekStart = currentDate.with(DayOfWeek.MONDAY)
        val weekEnd = currentDate.with(DayOfWeek.SUNDAY)
        val budgetWeekStart = maxOf(weekStart, monthStart, plan.effectiveStartDate)
        val budgetWeekEnd = minOf(weekEnd, monthEnd)
        val spentBeforeWeek = countedExpenses.filter { it.date(zoneId).isBefore(budgetWeekStart) }.sumCents { it.amountCents }
        val daysAtWeekStart = if (budgetWeekStart > monthEnd) 0 else daysInclusive(budgetWeekStart, monthEnd)
        val weekDays = if (budgetWeekStart > budgetWeekEnd) 0 else daysInclusive(budgetWeekStart, budgetWeekEnd)
        val weekAllowance = if (daysAtWeekStart == 0L || weekDays == 0L) 0 else allocatedBetween(
            max(0, safeSubtract(monthlySpendable, spentBeforeWeek)), budgetWeekStart, monthEnd, budgetWeekStart, budgetWeekEnd
        )
        val weekSpent = countedExpenses.filter { it.date(zoneId) in budgetWeekStart..budgetWeekEnd }.sumCents { it.amountCents }
        val prediction = predictor.predict(countedExpenses, currentDate, monthEnd, monthRemaining, zoneId)
        val projected = (prediction as? Prediction.Available)?.projectedMonthEndBalanceCents
        val onTrack = projected?.let(SavingGoalCalculator::isOnTrack) ?: (monthRemaining >= 0)
        val risk = when {
            monthlySpendable < 0 || monthRemaining < 0 || projected?.let { it < 0 } == true -> BudgetRiskLevel.RISK
            projected != null && projected <= max(1, max(0, monthlySpendable) / 10) -> BudgetRiskLevel.WARNING
            else -> BudgetRiskLevel.SAFE
        }
        return BudgetResult(
            monthlySpendableCents = monthlySpendable,
            monthRemainingCents = monthRemaining,
            todayAllowanceCents = max(0, todayAllowance),
            todayRemainingCents = max(0, todayDelta),
            todayOverspendCents = max(0, -todayDelta),
            weekAllowanceCents = weekAllowance,
            weekRemainingCents = safeSubtract(weekAllowance, weekSpent),
            prediction = prediction,
            savingGoalOnTrack = onTrack,
            budgetRiskLevel = risk,
        )
    }

    private fun validate(plan: MonthlyPlan, transactions: List<Transaction>, fixed: List<FixedExpense>, date: LocalDate) {
        require(plan.month in 1..12)
        require(plan.baseIncomeCents >= 0 && plan.savingGoalCents >= 0 && plan.safetyReserveCents >= 0)
        require(plan.effectiveStartDate.year == plan.year && plan.effectiveStartDate.monthValue == plan.month)
        require(date.year == plan.year && date.monthValue == plan.month) { "currentDate must be inside the plan month" }
        require(transactions.all { it.amountCents > 0 }) { "Transaction amounts must be positive" }
        require(fixed.all { it.amountCents > 0 }) { "Fixed expense amounts must be positive" }
    }
}

private fun daysInclusive(start: LocalDate, end: LocalDate): Long = end.toEpochDay() - start.toEpochDay() + 1
private fun allocatedBetween(total: Long, start: LocalDate, end: LocalDate, rangeStart: LocalDate, rangeEnd: LocalDate): Long {
    if (total <= 0 || start > end || rangeStart > rangeEnd) return 0
    val days = daysInclusive(start, end)
    val base = total / days
    val remainder = total % days
    val count = daysInclusive(maxOf(start, rangeStart), minOf(end, rangeEnd)).coerceAtLeast(0)
    if (count == 0L) return 0
    val remainderStart = end.minusDays(remainder - 1)
    val extraStart = maxOf(start, rangeStart, remainderStart)
    val extraEnd = minOf(end, rangeEnd)
    val extras = if (remainder == 0L || extraStart > extraEnd) 0 else daysInclusive(extraStart, extraEnd)
    return safeAdd(safeMultiply(base, count), extras)
}
private inline fun <T> Iterable<T>.sumCents(value: (T) -> Long): Long = fold(0L) { sum, item -> safeAdd(sum, value(item)) }
private fun safeAdd(a: Long, b: Long): Long = Math.addExact(a, b)
private fun safeSubtract(a: Long, b: Long): Long = Math.subtractExact(a, b)
private fun safeMultiply(a: Long, b: Long): Long = Math.multiplyExact(a, b)
