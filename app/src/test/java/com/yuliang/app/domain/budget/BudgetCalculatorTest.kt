package com.yuliang.app.domain.budget

import com.yuliang.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class BudgetCalculatorTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val calculator = BudgetCalculator()
    private fun plan(date: LocalDate = LocalDate.of(2028, 2, 1), income: Long = 200_000, saving: Long = 30_000, reserve: Long = 10_000) =
        MonthlyPlan(year = 2028, month = 2, baseIncomeCents = income, savingGoalCents = saving, safetyReserveCents = reserve, effectiveStartDate = date)
    private fun expense(cents: Long, date: LocalDate, linked: Long? = null) = Transaction(type = TransactionType.EXPENSE, amountCents = cents, occurredAt = date.atTime(12, 0).atZone(zone).toInstant(), linkedFixedExpenseId = linked)
    private fun income(cents: Long, allocation: IncomeAllocation) = Transaction(type = TransactionType.INCOME, amountCents = cents, occurredAt = LocalDate.of(2028, 2, 3).atStartOfDay(zone).toInstant(), incomeAllocation = allocation)
    private fun fixed(status: FixedExpenseStatus) = FixedExpense(1, 1, "宿舍网费", 40_000, 5, status, if (status == FixedExpenseStatus.PAID) 9 else null)

    @Test fun baseFormulaAndZeroSpend() {
        val r = calculator.calculate(plan(), emptyList(), listOf(fixed(FixedExpenseStatus.UPCOMING)), LocalDate.of(2028, 2, 1), zone)
        assertEquals(120_000, r.monthlySpendableCents)
        assertEquals(120_000, r.monthRemainingCents)
        assertEquals(120_000 / 29, r.todayAllowanceCents)
    }

    @Test fun paidFixedExpenseIsNotCountedTwice() {
        val paid = fixed(FixedExpenseStatus.PAID)
        val r = calculator.calculate(plan(), listOf(expense(40_000, LocalDate.of(2028, 2, 5), linked = 1)), listOf(paid), LocalDate.of(2028, 2, 5), zone)
        assertEquals(120_000, r.monthRemainingCents)
    }

    @Test fun skippedFixedExpenseIsNotFrozen() {
        val r = calculator.calculate(plan(), emptyList(), listOf(fixed(FixedExpenseStatus.SKIPPED)), LocalDate.of(2028, 2, 1), zone)
        assertEquals(160_000, r.monthlySpendableCents)
    }

    @Test fun overspendShowsZeroAndCarriesForward() {
        val date = LocalDate.of(2028, 2, 10)
        val before = expense(80_000, date.minusDays(1))
        val today = expense(10_000, date)
        val r = calculator.calculate(plan(income = 100_000, saving = 0, reserve = 0), listOf(before, today), emptyList(), date, zone)
        assertEquals(0, r.todayRemainingCents)
        assertTrue(r.todayOverspendCents > 0)
        assertEquals(10_000, r.monthRemainingCents)
    }

    @Test fun savingRaisesFutureAllowance() {
        val date = LocalDate.of(2028, 2, 10)
        val lowSpend = calculator.calculate(plan(income = 290_000, saving = 0, reserve = 0), listOf(expense(1_000, date.minusDays(1))), emptyList(), date, zone)
        val highSpend = calculator.calculate(plan(income = 290_000, saving = 0, reserve = 0), listOf(expense(20_000, date.minusDays(1))), emptyList(), date, zone)
        assertTrue(lowSpend.todayAllowanceCents > highSpend.todayAllowanceCents)
    }

    @Test fun allocationsHaveThreeDifferentBudgetEffects() {
        val base = plan(income = 100_000, saving = 0, reserve = 0)
        val date = LocalDate.of(2028, 2, 3)
        assertEquals(150_000, calculator.calculate(base, listOf(income(50_000, IncomeAllocation.SPENDABLE)), emptyList(), date, zone).monthlySpendableCents)
        assertEquals(100_000, calculator.calculate(base, listOf(income(50_000, IncomeAllocation.SAVING)), emptyList(), date, zone).monthlySpendableCents)
        assertEquals(100_000, calculator.calculate(base, listOf(income(50_000, IncomeAllocation.RECORD_ONLY)), emptyList(), date, zone).monthlySpendableCents)
    }

    @Test fun midMonthUsesOnlyRemainingLeapYearDays() {
        val date = LocalDate.of(2028, 2, 15)
        val r = calculator.calculate(plan(date, income = 150_000, saving = 0, reserve = 0), emptyList(), emptyList(), date, zone)
        assertEquals(10_000, r.todayAllowanceCents)
    }

    @Test fun reservationsOverIncomeGiveRiskWithoutCrash() {
        val r = calculator.calculate(plan(income = 20_000, saving = 30_000, reserve = 10_000), emptyList(), emptyList(), LocalDate.of(2028, 2, 1), zone)
        assertEquals(BudgetRiskLevel.RISK, r.budgetRiskLevel)
        assertEquals(0, r.todayRemainingCents)
    }

    @Test fun predictionRequiresThreeConsumptionDays() {
        val date = LocalDate.of(2028, 2, 10)
        val two = listOf(expense(1000, date.minusDays(1)), expense(1000, date.minusDays(2)))
        assertTrue(calculator.calculate(plan(), two, emptyList(), date, zone).prediction is Prediction.InsufficientData)
        val three = two + expense(1000, date.minusDays(3))
        assertTrue(calculator.calculate(plan(), three, emptyList(), date, zone).prediction is Prediction.Available)
    }

    @Test fun editingAndDeletingTransactionRecomputesAllDerivedValues() {
        val date = LocalDate.of(2028, 2, 10)
        val first = calculator.calculate(plan(), listOf(expense(10_000, date.minusDays(1))), emptyList(), date, zone)
        val edited = calculator.calculate(plan(), listOf(expense(20_000, date.minusDays(1))), emptyList(), date, zone)
        val deleted = calculator.calculate(plan(), emptyList(), emptyList(), date, zone)
        assertTrue(first.monthRemainingCents > edited.monthRemainingCents)
        assertTrue(deleted.todayAllowanceCents > first.todayAllowanceCents)
    }

    @Test fun crossMonthWeekIsClippedToFebruary() {
        val date = LocalDate.of(2028, 2, 29)
        val r = calculator.calculate(plan(income = 290_000, saving = 0, reserve = 0), emptyList(), emptyList(), date, zone)
        assertEquals(290_000, r.weekAllowanceCents)
        assertEquals(r.monthRemainingCents, r.weekRemainingCents)
    }

    @Test fun remainderCentsStayInBudgetAndGoToLaterDays() {
        val date = LocalDate.of(2028, 2, 28)
        val r = calculator.calculate(plan(income = 101, saving = 0, reserve = 0), emptyList(), emptyList(), date, zone)
        assertEquals(50, r.todayAllowanceCents)
        assertEquals(101, r.weekAllowanceCents)
    }

    @Test(expected = ArithmeticException::class)
    fun overflowFailsClearly() {
        calculator.calculate(plan(income = Long.MAX_VALUE, saving = 0, reserve = 0), listOf(income(1, IncomeAllocation.SPENDABLE)), emptyList(), LocalDate.of(2028, 2, 3), zone)
    }
}
