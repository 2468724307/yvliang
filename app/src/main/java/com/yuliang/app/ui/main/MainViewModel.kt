package com.yuliang.app.ui.main

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yuliang.app.AppContainer
import com.yuliang.app.data.repository.Category
import com.yuliang.app.domain.dashboard.*
import com.yuliang.app.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*

data class ExportPayload(val name: String, val mimeType: String, val bytes: ByteArray)

data class MainUiState(
    val dashboard: DashboardResult = DashboardResult.NoPlan,
    val statistics: StatisticsResult = StatisticsResult.Empty,
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val reduceMotion: Boolean = false,
    val busy: Boolean = false,
    val export: ExportPayload? = null,
)

class MainViewModel(private val container: AppContainer) : ViewModel() {
    private val zone = ZoneId.systemDefault()
    private val today: LocalDate get() = LocalDate.now(zone)
    private val busy = MutableStateFlow(false)
    private val export = MutableStateFlow<ExportPayload?>(null)
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()
    private var recording = false

    private data class Sources(
        val plan: MonthlyPlan?,
        val fixed: List<FixedExpense>,
        val transactions: List<Transaction>,
        val categories: List<Category>,
        val reduceMotion: Boolean,
    )

    private val sources = combine(
        container.planRepository.observe(today.year, today.monthValue),
        container.fixedExpenseRepository.observeMonth(today.year, today.monthValue),
        container.ledgerRepository.observeAll(),
        container.categoryRepository.observeAll(),
        container.settings.reduceMotion,
    ) { plan, fixed, transactions, categories, reduceMotion ->
        Sources(plan, fixed, transactions, categories, reduceMotion)
    }

    val state: StateFlow<MainUiState> = combine(sources, busy, export) { source, working, pendingExport ->
        val currentMonth = source.transactions.filter { tx ->
            val date = tx.date(zone)
            date.year == today.year && date.monthValue == today.monthValue
        }
        val dashboard = DashboardUseCase().execute(source.plan, currentMonth, source.fixed, today, zone)
        val budget = (dashboard as? DashboardResult.Ready)?.budget
        MainUiState(
            dashboard = dashboard,
            statistics = StatisticsCalculator().calculate(currentMonth, source.categories.associate { it.id to it.name }, budget, today, zone),
            transactions = source.transactions,
            categories = source.categories,
            reduceMotion = source.reduceMotion,
            busy = working,
            export = pendingExport,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            container.categoryRepository.ensureDefaults()
            container.fixedExpenseRepository.ensureMonthInstances(today.year, today.monthValue)
        }
    }

    fun addTransaction(type: TransactionType, amountCents: Long, categoryId: Long?, note: String?, occurredAt: Instant, allocation: IncomeAllocation?, onResult: (Boolean) -> Unit = {}) {
        if (recording) return
        recording = true
        viewModelScope.launch {
            busy.value = true
            val saved = try {
                container.ledgerRepository.add(Transaction(type = type, amountCents = amountCents, occurredAt = occurredAt, incomeAllocation = allocation, categoryId = categoryId, note = note))
                _messages.emit("已记账")
                true
            } catch (error: Exception) {
                _messages.emit(error.message ?: "保存失败，请重试")
                false
            } finally {
                recording = false
                busy.value = false
            }
            onResult(saved)
        }
    }

    fun updateTransaction(id: Long, type: TransactionType, amountCents: Long, categoryId: Long?, note: String?, occurredAt: Instant, allocation: IncomeAllocation?) = action("账单已更新") {
        container.ledgerRepository.update(Transaction(id = id, type = type, amountCents = amountCents, occurredAt = occurredAt, incomeAllocation = allocation, categoryId = categoryId, note = note))
    }

    fun deleteTransaction(id: Long) = action("账单已删除") { container.ledgerRepository.delete(id) }
    fun addCategory(name: String, icon: String, type: TransactionType) = action("分类已添加") { container.categoryRepository.add(name, icon, type) }
    fun archiveCategory(id: Long) = action("分类已归档，历史账单仍会保留") { container.categoryRepository.archive(id) }
    fun setReduceMotion(value: Boolean) = action(null) { container.settings.setReduceMotion(value) }

    fun prepareBackup() = action(null) {
        export.value = ExportPayload("余量备份-${today}.json", "application/json", container.dataTransfer.createBackup())
    }

    fun prepareCsv() = action(null) {
        export.value = ExportPayload("余量账单-${today}.csv", "text/csv", container.dataTransfer.createTransactionsCsv())
    }

    fun exportHandled() { export.value = null }

    fun writeExport(resolver: ContentResolver, uri: Uri, payload: ExportPayload) = action(null) {
        val result = container.dataTransfer.writeAndVerify(resolver, uri, payload.bytes)
        export.value = null
        _messages.emit("文件已保存（${result.bytesWritten} 字节）")
    }

    suspend fun writeExportAwait(resolver: ContentResolver, uri: Uri, payload: ExportPayload): Result<Long> = runCatching {
        val written = container.dataTransfer.writeAndVerify(resolver, uri, payload.bytes).bytesWritten
        export.value = null
        _messages.emit("文件已保存（$written 字节）")
        written
    }.onFailure { _messages.emit(it.message ?: "文件保存失败") }

    fun restore(resolver: ContentResolver, uri: Uri) = action(null) {
        val bytes = container.dataTransfer.read(resolver, uri)
        container.dataTransfer.restore(bytes)
        _messages.emit("备份恢复完成")
    }

    fun showMessage(message: String) { _messages.tryEmit(message) }

    private fun action(success: String?, block: suspend () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            try {
                block()
                if (success != null) _messages.emit(success)
            } catch (error: Exception) {
                _messages.emit(error.message ?: "操作失败，请稍后重试")
            } finally {
                busy.value = false
            }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(container) as T
    }
}
