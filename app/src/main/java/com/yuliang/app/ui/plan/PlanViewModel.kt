package com.yuliang.app.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yuliang.app.AppContainer
import com.yuliang.app.domain.budget.BudgetCalculator
import com.yuliang.app.domain.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*

data class PlanUiState(
    val plan: MonthlyPlan? = null,
    val fixedExpenses: List<FixedExpense> = emptyList(),
    val budget: BudgetResult? = null,
    val saving: Boolean = false,
    val message: String? = null,
)

class PlanViewModel(private val container: AppContainer) : ViewModel() {
    private val zone = ZoneId.systemDefault()
    private val today: LocalDate get() = LocalDate.now(zone)
    private val calendarDay = MutableStateFlow(today)
    private val status = MutableStateFlow<Pair<Boolean, String?>>(false to null)
    private data class Sources(val date: LocalDate, val plan: MonthlyPlan?, val fixed: List<FixedExpense>, val transactions: List<Transaction>)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val sources = calendarDay.flatMapLatest { day -> combine(
        container.planRepository.observe(day.year, day.monthValue),
        container.fixedExpenseRepository.observeMonth(day.year, day.monthValue),
        container.ledgerRepository.observeMonth(day.year, day.monthValue, zone),
    ) { p, f, t -> Sources(day, p, f, t) } }

    val state: StateFlow<PlanUiState> = combine(sources, status) { source, s ->
        PlanUiState(source.plan, source.fixed, source.plan?.let { BudgetCalculator().calculate(it, source.transactions, source.fixed, source.date, zone) }, s.first, s.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState())

    init {
        viewModelScope.launch { container.fixedExpenseRepository.ensureMonthInstances(today.year, today.monthValue) }
        viewModelScope.launch {
            while (true) {
                val nextMidnight = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                delay((nextMidnight - System.currentTimeMillis()).coerceAtLeast(1_000L))
                refreshDate()
            }
        }
    }

    fun refreshDate() {
        val current = today
        if (calendarDay.value != current) {
            calendarDay.value = current
            viewModelScope.launch { container.fixedExpenseRepository.ensureMonthInstances(current.year, current.monthValue) }
        }
    }

    fun savePlan(baseCents: Long, savingCents: Long, reserveCents: Long) = launchAction("本月计划已保存") {
        container.planRepository.save(MonthlyPlan(year = today.year, month = today.monthValue, baseIncomeCents = baseCents, savingGoalCents = savingCents, safetyReserveCents = reserveCents, effectiveStartDate = today))
    }

    fun addFixed(name: String, amountCents: Long, dueDay: Int) = launchAction("固定支出已加入本月") {
        container.fixedExpenseRepository.addTemplateAndMonth(name, amountCents, dueDay, today.year, today.monthValue)
    }

    fun skipFixed(id: Long) = launchAction("本月已跳过") { container.fixedExpenseRepository.setSkipped(id) }
    fun payFixed(id: Long) = launchAction("固定支出已记为支付") { container.fixedExpenseRepository.pay(id, Instant.now()) }
    fun clearMessage() { status.value = false to null }
    fun showInputError(message: String) { status.value = false to message }

    private fun launchAction(success: String, block: suspend () -> Unit) {
        if (status.value.first) return
        status.value = true to null
        viewModelScope.launch {
            status.value = try { block(); false to success } catch (e: IllegalArgumentException) { false to (e.message ?: "输入有误") }
            catch (_: Exception) { false to "保存失败，请稍后重试" }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PlanViewModel(container) as T
    }
}
