package com.yuliang.app.domain.dashboard

import com.yuliang.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class DashboardTest {
    private val date = LocalDate.of(2026, 9, 23)
    private val zone = ZoneId.of("UTC")
    private val plan = MonthlyPlan(year = 2026, month = 9, baseIncomeCents = 100_000, savingGoalCents = 10_000, safetyReserveCents = 5_000, effectiveStartDate = LocalDate.of(2026, 9, 1))

    @Test fun noPlanProducesExplicitState() {
        assertEquals(DashboardResult.NoPlan, DashboardUseCase().execute(null, emptyList(), emptyList(), date, zone))
    }

    @Test fun dashboardConsumesBudgetEngineAndRecentBills() {
        val bills = (1..8).map { day -> Transaction(day.toLong(), TransactionType.EXPENSE, day * 100L, LocalDate.of(2026, 9, day).atStartOfDay(zone).toInstant()) }
        val result = DashboardUseCase().execute(plan, bills, emptyList(), date, zone) as DashboardResult.Ready
        assertEquals(3_600L, result.monthSpentCents)
        assertEquals(listOf(8L, 7L, 6L, 5L, 4L), result.recentTransactions.map { it.id })
        assertEquals(85_000L, result.budget.monthlySpendableCents)
    }

    @Test fun statisticsHandlesEmptyNormalAndRiskInputs() {
        val calculator = StatisticsCalculator()
        assertEquals(StatisticsResult.Empty, calculator.calculate(emptyList(), emptyMap(), null, date, zone))
        val tx = listOf(
            Transaction(1, TransactionType.EXPENSE, 2_000, LocalDate.of(2026, 9, 20).atStartOfDay(zone).toInstant(), categoryId = 4),
            Transaction(2, TransactionType.EXPENSE, 3_000, LocalDate.of(2026, 9, 21).atStartOfDay(zone).toInstant(), categoryId = 4),
        )
        val budget = com.yuliang.app.domain.budget.BudgetCalculator().calculate(plan, tx, emptyList(), date, zone)
        val result = calculator.calculate(tx, mapOf(4L to "学习"), budget, date, zone) as StatisticsResult.Content
        assertEquals(5_000L, result.totalExpenseCents)
        assertEquals("学习", result.topCategory)
        assertEquals(budget.budgetRiskLevel, result.riskLevel)
    }

    @Test fun sevenDayStatisticsUsesSamePeriodForTotalCategoriesAndTrendAcrossMonth() {
        val now = LocalDate.of(2026, 10, 2)
        val tx = listOf(
            Transaction(1, TransactionType.EXPENSE, 1_000, LocalDate.of(2026, 9, 26).atStartOfDay(zone).toInstant(), categoryId = 4),
            Transaction(2, TransactionType.EXPENSE, 2_000, LocalDate.of(2026, 9, 27).atStartOfDay(zone).toInstant(), categoryId = 4),
            Transaction(3, TransactionType.EXPENSE, 3_000, now.atStartOfDay(zone).toInstant(), categoryId = 5),
        )
        val stats = StatisticsCalculator().calculate(tx, mapOf(4L to "学习", 5L to "餐饮"), null, now, zone, StatisticsPeriod.LAST_SEVEN_DAYS) as StatisticsResult.Content
        assertEquals(6_000L, stats.totalExpenseCents)
        assertEquals(6_000L, stats.categorySlices.sumOf { it.amountCents })
        assertEquals(6_000L, stats.dailyTrend.sumOf { it.amountCents })
        assertEquals(LocalDate.of(2026, 9, 26), stats.dailyTrend.first().date)
        assertEquals(7, stats.dailyTrend.size)
        assertNull(stats.prediction)
    }

    @Test fun transactionImpactNeverShowsNegativeMainValue() {
        assertEquals(ExpenseImpact(0, 1_500), TransactionImpactCalculator.afterExpense(3_500, 5_000))
        assertEquals(ExpenseImpact(1_500, 0), TransactionImpactCalculator.afterExpense(3_500, 2_000))
    }

    @Test fun editingAndDeletingExpenseKeepDashboardAndStatisticsInAgreement() {
        val first = Transaction(1, TransactionType.EXPENSE, 1_000, date.atStartOfDay(zone).toInstant(), categoryId = 4)
        val second = Transaction(2, TransactionType.EXPENSE, 2_000, date.atStartOfDay(zone).toInstant(), categoryId = 5)
        for (ledger in listOf(listOf(first, second), listOf(first.copy(amountCents = 8_000), second), listOf(second))) {
            val dashboard = DashboardUseCase().execute(plan, ledger, emptyList(), date, zone) as DashboardResult.Ready
            val statistics = StatisticsCalculator().calculate(ledger, mapOf(4L to "学习", 5L to "餐饮"), dashboard.budget, date, zone) as StatisticsResult.Content
            assertEquals(dashboard.monthSpentCents, statistics.totalExpenseCents)
            assertEquals(statistics.totalExpenseCents, statistics.categorySlices.sumOf { it.amountCents })
            assertEquals(statistics.totalExpenseCents, statistics.dailyTrend.sumOf { it.amountCents })
        }
    }
}
