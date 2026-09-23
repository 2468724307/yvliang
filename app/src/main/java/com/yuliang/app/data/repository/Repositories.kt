package com.yuliang.app.data.repository

import androidx.room.withTransaction
import com.yuliang.app.data.database.*
import com.yuliang.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.*

interface PlanRepository {
    fun observe(year: Int, month: Int): Flow<MonthlyPlan?>
    suspend fun save(plan: MonthlyPlan)
}

interface LedgerRepository {
    fun observeMonth(year: Int, month: Int, zoneId: ZoneId): Flow<List<Transaction>>
    fun observeAll(): Flow<List<Transaction>>
    suspend fun add(transaction: Transaction): Long
    suspend fun update(transaction: Transaction)
    suspend fun delete(id: Long)
    suspend fun get(id: Long): Transaction?
}

data class Category(val id: Long, val name: String, val icon: String, val type: TransactionType, val isEnabled: Boolean)

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
    suspend fun ensureDefaults()
    suspend fun add(name: String, icon: String, type: TransactionType)
    suspend fun archive(id: Long)
}

interface FixedExpenseRepository {
    fun observeMonth(year: Int, month: Int): Flow<List<FixedExpense>>
    suspend fun addTemplateAndMonth(name: String, amountCents: Long, dueDay: Int, year: Int, month: Int)
    suspend fun ensureMonthInstances(year: Int, month: Int)
    suspend fun setSkipped(instanceId: Long)
    suspend fun pay(instanceId: Long, paidAt: Instant): Long
}

class RoomPlanRepository(private val dao: MonthlyPlanDao) : PlanRepository {
    override fun observe(year: Int, month: Int) = dao.observe(year, month).map { it?.toDomain() }
    override suspend fun save(plan: MonthlyPlan) {
        require(plan.baseIncomeCents >= 0 && plan.savingGoalCents >= 0 && plan.safetyReserveCents >= 0)
        require(plan.month in 1..12 && plan.effectiveStartDate.year == plan.year && plan.effectiveStartDate.monthValue == plan.month)
        val now = System.currentTimeMillis()
        val existing = dao.get(plan.year, plan.month)
        val stablePlan = if (existing == null) plan else plan.copy(id = existing.id, effectiveStartDate = LocalDate.ofEpochDay(existing.effectiveStartEpochDay))
        dao.upsert(stablePlan.toEntity(existing?.id ?: plan.id, existing?.createdAt ?: now, now))
    }
}

class RoomLedgerRepository(private val dao: LedgerDao) : LedgerRepository {
    override fun observeMonth(year: Int, month: Int, zoneId: ZoneId): Flow<List<Transaction>> {
        val start = LocalDate.of(year, month, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = LocalDate.of(year, month, 1).plusMonths(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
        return dao.observeTransactions(start, end).map { list -> list.map { it.toDomain() } }
    }
    override fun observeAll(): Flow<List<Transaction>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }
    override suspend fun add(transaction: Transaction): Long {
        require(transaction.amountCents > 0)
        val now = System.currentTimeMillis()
        return dao.insertTransaction(transaction.toEntity(createdAt = now, updatedAt = now))
    }
    override suspend fun update(transaction: Transaction) {
        require(transaction.id > 0 && transaction.amountCents > 0)
        val current = requireNotNull(dao.getTransaction(transaction.id))
        require(current.linkedFixedExpenseId == null) { "固定支出关联账单请在固定支出中管理" }
        dao.updateTransaction(transaction.toEntity(createdAt = current.createdAt, updatedAt = System.currentTimeMillis()))
    }
    override suspend fun delete(id: Long) {
        val current = requireNotNull(dao.getTransaction(id))
        require(current.linkedFixedExpenseId == null) { "固定支出关联账单不能直接删除" }
        check(dao.deleteById(id) == 1)
    }
    override suspend fun get(id: Long): Transaction? = dao.getTransaction(id)?.toDomain()
}

class RoomCategoryRepository(private val dao: CategoryDao) : CategoryRepository {
    override fun observeAll(): Flow<List<Category>> = dao.observeAll().map { rows ->
        rows.map { Category(it.id, it.name, it.icon, TransactionType.valueOf(it.type), it.isEnabled) }
    }

    override suspend fun ensureDefaults() {
        listOf(
            "餐饮" to "🍜", "购物" to "🛍", "交通" to "🚌",
            "学习" to "📚", "娱乐" to "🎮", "其他" to "…",
        ).forEachIndexed { index, (name, icon) ->
            dao.insert(CategoryEntity(name = name, icon = icon, type = TransactionType.EXPENSE.name, sortOrder = index, isSystem = true))
        }
        dao.insert(CategoryEntity(name = "收入", icon = "＋", type = TransactionType.INCOME.name, sortOrder = 100, isSystem = true))
    }

    override suspend fun add(name: String, icon: String, type: TransactionType) {
        require(name.isNotBlank() && name.trim().length <= 12) { "分类名称需为 1 至 12 个字符" }
        require(type != TransactionType.TRANSFER) { "转账不使用消费分类" }
        val nextSort = dao.getAll().maxOfOrNull { it.sortOrder }?.plus(1) ?: 0
        val id = dao.insert(CategoryEntity(name = name.trim(), icon = icon.ifBlank { "·" }, type = type.name, sortOrder = nextSort, isSystem = false))
        require(id != -1L) { "已存在同名分类" }
    }

    override suspend fun archive(id: Long) { dao.archive(id) }
}

class RoomFixedExpenseRepository(private val db: YuliangDatabase) : FixedExpenseRepository {
    private val dao = db.fixedExpenseDao()
    override fun observeMonth(year: Int, month: Int): Flow<List<FixedExpense>> =
        combine(dao.observeTemplates(), dao.observeInstances(year, month)) { templates, instances ->
            val byId = templates.associateBy { it.id }
            instances.mapNotNull { instance -> byId[instance.templateId]?.let { instance.toDomain(it) } }
        }

    override suspend fun addTemplateAndMonth(name: String, amountCents: Long, dueDay: Int, year: Int, month: Int) {
        require(name.isNotBlank() && amountCents > 0 && dueDay in 1..31 && month in 1..12)
        db.withTransaction {
            val now = System.currentTimeMillis()
            val templateId = dao.insertTemplate(FixedExpenseTemplateEntity(name = name.trim(), amountCents = amountCents, dueDay = dueDay, createdAt = now, updatedAt = now))
            dao.insertInstance(FixedExpenseInstanceEntity(templateId = templateId, year = year, month = month, createdAt = now, updatedAt = now))
        }
    }

    override suspend fun ensureMonthInstances(year: Int, month: Int) {
        require(month in 1..12)
        db.withTransaction {
            val now = System.currentTimeMillis()
            dao.getRepeatingTemplates().forEach { template ->
                dao.insertInstance(FixedExpenseInstanceEntity(templateId = template.id, year = year, month = month, createdAt = now, updatedAt = now))
            }
        }
    }

    override suspend fun setSkipped(instanceId: Long) {
        require(dao.skipIfUpcoming(instanceId, System.currentTimeMillis()) == 1) { "Only an upcoming fixed expense can be skipped" }
    }

    override suspend fun pay(instanceId: Long, paidAt: Instant): Long = db.withTransaction {
        val instance = requireNotNull(dao.getInstance(instanceId)) { "Fixed expense instance does not exist" }
        require(instance.status == FixedExpenseStatus.UPCOMING.name && instance.linkedTransactionId == null) { "Fixed expense is already resolved" }
        val template = requireNotNull(dao.getTemplate(instance.templateId)) { "Fixed expense template does not exist" }
        val now = System.currentTimeMillis()
        val txId = db.ledgerDao().insertTransaction(TransactionEntity(type = TransactionType.EXPENSE.name, amountCents = template.amountCents, categoryId = template.categoryId, occurredAt = paidAt.toEpochMilli(), linkedFixedExpenseId = instanceId, createdAt = now, updatedAt = now))
        check(dao.linkPaidIfUpcoming(instanceId, txId, now) == 1) { "Fixed expense state changed" }
        txId
    }
}

private fun MonthlyPlanEntity.toDomain() = MonthlyPlan(id, year, month, baseIncomeCents, savingGoalCents, safetyReserveCents, LocalDate.ofEpochDay(effectiveStartEpochDay))
private fun MonthlyPlan.toEntity(entityId: Long, created: Long, updated: Long) = MonthlyPlanEntity(entityId, year, month, baseIncomeCents, savingGoalCents, safetyReserveCents, effectiveStartDate.toEpochDay(), created, updated)
internal fun TransactionEntity.toDomain() = Transaction(id, TransactionType.valueOf(type), amountCents, Instant.ofEpochMilli(occurredAt), incomeAllocation?.let(IncomeAllocation::valueOf), linkedFixedExpenseId, categoryId, note)
internal fun Transaction.toEntity(createdAt: Long, updatedAt: Long) = TransactionEntity(
    id = id,
    type = type.name,
    amountCents = amountCents,
    categoryId = categoryId,
    note = note?.trim()?.takeIf(String::isNotEmpty),
    occurredAt = occurredAt.toEpochMilli(),
    incomeAllocation = incomeAllocation?.name,
    linkedFixedExpenseId = linkedFixedExpenseId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
private fun FixedExpenseInstanceEntity.toDomain(t: FixedExpenseTemplateEntity) = FixedExpense(id, templateId, t.name, t.amountCents, t.dueDay, FixedExpenseStatus.valueOf(status), linkedTransactionId)
