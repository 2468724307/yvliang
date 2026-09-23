package com.yuliang.app.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yuliang.app.AppContainer
import com.yuliang.app.domain.budget.BudgetCalculator
import com.yuliang.app.domain.model.*
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
    private val today = LocalDate.now(zone)
    private val year = today.year
    private val month = today.monthValue
    private val status = MutableStateFlow<Pair<Boolean, String?>>(false to null)
    private val plan = container.planRepository.observe(year, month)
    private val fixed = container.fixedExpenseRepository.observeMonth(year, month)
    private val transactions = container.ledgerRepository.observeMonth(year, month, zone)

    val state: StateFlow<PlanUiState> = combine(plan, fixed, transactions, status) { p, f, t, s ->
        PlanUiState(p, f, p?.let { BudgetCalculator().calculate(it, t, f, today, zone) }, s.first, s.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState())

    init { viewModelScope.launch { container.fixedExpenseRepository.ensureMonthInstances(year, month) } }

    fun savePlan(baseCents: Long, savingCents: Long, reserveCents: Long) = launchAction("本月计划已保存") {
        container.planRepository.save(MonthlyPlan(year = year, month = month, baseIncomeCents = baseCents, savingGoalCents = savingCents, safetyReserveCents = reserveCents, effectiveStartDate = today))
    }

    fun addFixed(name: String, amountCents: Long, dueDay: Int) = launchAction("固定支出已加入本月") {
        container.fixedExpenseRepository.addTemplateAndMonth(name, amountCents, dueDay, year, month)
    }

    fun skipFixed(id: Long) = launchAction("本月已跳过") { container.fixedExpenseRepository.setSkipped(id) }
    fun payFixed(id: Long) = launchAction("固定支出已记为支付") { container.fixedExpenseRepository.pay(id, Instant.now()) }
    fun clearMessage() { status.value = false to null }
    fun showInputError(message: String) { status.value = false to message }

    private fun launchAction(success: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            status.value = true to null
            status.value = try { block(); false to success } catch (e: IllegalArgumentException) { false to (e.message ?: "输入有误") }
            catch (_: Exception) { false to "保存失败，请稍后重试" }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PlanViewModel(container) as T
    }
}
