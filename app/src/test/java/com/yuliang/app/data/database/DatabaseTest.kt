package com.yuliang.app.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yuliang.app.data.repository.RoomFixedExpenseRepository
import com.yuliang.app.data.repository.RoomPlanRepository
import com.yuliang.app.domain.budget.BudgetCalculator
import com.yuliang.app.domain.model.MonthlyPlan
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseTest {
    private lateinit var db: YuliangDatabase

    @Before fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), YuliangDatabase::class.java
        ).allowMainThreadQueries().build()
    }
    @After fun closeDb() = db.close()

    @Test fun planInsertQueryUpdateDelete() = runTest {
        val dao = db.monthlyPlanDao()
        val first = MonthlyPlanEntity(year = 2026, month = 9, baseIncomeCents = 200_000, savingGoalCents = 30_000, effectiveStartEpochDay = 1, createdAt = 1, updatedAt = 1)
        val id = dao.upsert(first)
        assertEquals(200_000L, dao.get(2026, 9)?.baseIncomeCents)
        dao.upsert(first.copy(id = id, baseIncomeCents = 210_000, updatedAt = 2))
        assertEquals(210_000L, dao.get(2026, 9)?.baseIncomeCents)
        dao.delete(requireNotNull(dao.get(2026, 9)))
        assertNull(dao.get(2026, 9))
    }

    @Test fun categoryArchivePreservesTransactionMeaning() = runTest {
        val categoryId = db.categoryDao().insert(CategoryEntity(name = "餐饮", icon = "meal", type = "EXPENSE", sortOrder = 0, isSystem = true))
        val now = System.currentTimeMillis()
        val txId = db.ledgerDao().insertTransaction(TransactionEntity(type = "EXPENSE", amountCents = 100, categoryId = categoryId, occurredAt = now, createdAt = now, updatedAt = now))
        db.categoryDao().archive(categoryId)
        assertEquals(categoryId, db.ledgerDao().getTransaction(txId)?.categoryId)
        assertTrue(db.categoryDao().observeEnabled().first().isEmpty())
    }

    @Test fun paymentIsAtomicAndCannotDuplicate() = runTest {
        val repo = RoomFixedExpenseRepository(db)
        repo.addTemplateAndMonth("网费", 3_000, 5, 2026, 9)
        val instance = repo.observeMonth(2026, 9).first().single()
        repo.pay(instance.instanceId, Instant.parse("2026-09-05T00:00:00Z"))
        assertEquals(1, db.ledgerDao().countTransactions())
        assertEquals("PAID", db.fixedExpenseDao().getInstance(instance.instanceId)?.status)
        expectIllegalArgument { repo.pay(instance.instanceId, Instant.parse("2026-09-05T00:00:00Z")) }
        assertEquals(1, db.ledgerDao().countTransactions())
    }

    @Test fun rejectedPaymentDoesNotLeaveHalfTransaction() = runTest {
        val repo = RoomFixedExpenseRepository(db)
        repo.addTemplateAndMonth("会员", 2_000, 10, 2026, 9)
        val instance = repo.observeMonth(2026, 9).first().single()
        repo.setSkipped(instance.instanceId)
        expectIllegalArgument { repo.pay(instance.instanceId, Instant.now()) }
        assertEquals(0, db.ledgerDao().countTransactions())
    }

    @Test fun monthlyInstancesKeepIndependentStatus() = runTest {
        val dao = db.fixedExpenseDao()
        val now = System.currentTimeMillis()
        val templateId = dao.insertTemplate(FixedExpenseTemplateEntity(name = "云盘", amountCents = 1000, dueDay = 1, createdAt = now, updatedAt = now))
        val september = dao.insertInstance(FixedExpenseInstanceEntity(templateId = templateId, year = 2026, month = 9, createdAt = now, updatedAt = now))
        val october = dao.insertInstance(FixedExpenseInstanceEntity(templateId = templateId, year = 2026, month = 10, createdAt = now, updatedAt = now))
        dao.skipIfUpcoming(september, now + 1)
        assertEquals("SKIPPED", dao.getInstance(september)?.status)
        assertEquals("UPCOMING", dao.getInstance(october)?.status)
    }

    @Test fun updatingPlanPreservesOriginalEffectiveStart() = runTest {
        val repo = RoomPlanRepository(db.monthlyPlanDao())
        repo.save(MonthlyPlan(year = 2026, month = 9, baseIncomeCents = 100_000, savingGoalCents = 0, safetyReserveCents = 0, effectiveStartDate = LocalDate.of(2026, 9, 10)))
        repo.save(MonthlyPlan(year = 2026, month = 9, baseIncomeCents = 120_000, savingGoalCents = 0, safetyReserveCents = 0, effectiveStartDate = LocalDate.of(2026, 9, 20)))
        val stored = repo.observe(2026, 9).first()!!
        assertEquals(LocalDate.of(2026, 9, 10), stored.effectiveStartDate)
        assertEquals(120_000L, stored.baseIncomeCents)
    }

    @Test fun repeatingExpenseCreatesOneInstanceAndRecalculatesBudget() = runTest {
        val repo = RoomFixedExpenseRepository(db)
        repo.addTemplateAndMonth("网费", 4_000, 5, 2026, 9)
        repo.ensureMonthInstances(2026, 10)
        repo.ensureMonthInstances(2026, 10)
        val october = repo.observeMonth(2026, 10).first()
        assertEquals(1, october.size)
        val plan = MonthlyPlan(year = 2026, month = 10, baseIncomeCents = 100_000, savingGoalCents = 10_000, safetyReserveCents = 0, effectiveStartDate = LocalDate.of(2026, 10, 1))
        val result = BudgetCalculator().calculate(plan, emptyList(), october, LocalDate.of(2026, 10, 1))
        assertEquals(86_000L, result.monthlySpendableCents)
    }

    private suspend fun expectIllegalArgument(block: suspend () -> Unit) {
        try { block(); fail("Expected IllegalArgumentException") } catch (_: IllegalArgumentException) { }
    }
}
