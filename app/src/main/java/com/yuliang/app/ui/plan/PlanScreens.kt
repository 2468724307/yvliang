package com.yuliang.app.ui.plan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yuliang.app.domain.model.FixedExpenseStatus
import com.yuliang.app.ui.theme.appTextFieldColors
import com.yuliang.app.ui.theme.Spacing
import com.yuliang.app.ui.theme.YuliangTypography
import com.yuliang.app.ui.components.AmountInput
import com.yuliang.app.ui.components.AppTextInput
import com.yuliang.app.ui.components.AmountText
import com.yuliang.app.ui.components.DataCard
import com.yuliang.app.ui.components.PrimaryActionButton
import com.yuliang.app.ui.components.SecondaryActionButton
import com.yuliang.app.ui.components.money
import com.yuliang.app.ui.components.yuanInput
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun MonthlyPlanScreen(vm: PlanViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }
    var base by remember(state.plan) { mutableStateOf(state.plan?.baseIncomeCents?.toYuan() ?: "") }
    var saving by remember(state.plan) { mutableStateOf(state.plan?.savingGoalCents?.toYuan() ?: "") }
    var reserve by remember(state.plan) { mutableStateOf(state.plan?.safetyReserveCents?.toYuan() ?: "0") }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { SimpleTopBar("本月计划", onBack) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.padding(padding).padding(Spacing.content), verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            Text("存钱目标和安全余额会在月初立即预留。", style = MaterialTheme.typography.bodyMedium)
            MoneyField("本月生活费", base) { base = it }
            MoneyField("存钱目标", saving) { saving = it }
            MoneyField("安全余额", reserve) { reserve = it }
            state.budget?.let { DataCard(Modifier.fillMaxWidth()) {
                Text("当前可消费", style = YuliangTypography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AmountText(it.monthlySpendableCents, headline = true)
            } }
            PrimaryActionButton(enabled = !state.saving, onClick = {
                val values = listOf(base, saving, reserve).map(::yuanToCentsOrNull)
                if (values.all { it != null }) vm.savePlan(values[0]!!, values[1]!!, values[2]!!)
                else vm.showInputError("请填写有效金额，最多保留两位小数")
            }, modifier = Modifier.fillMaxWidth()) { Text(if (state.saving) "保存中" else "保存计划") }
        }
    }
}

@Composable
fun FixedExpenseScreen(vm: PlanViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueDay by remember { mutableStateOf("1") }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { SimpleTopBar("固定支出", onBack) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(horizontal = Spacing.content), verticalArrangement = Arrangement.spacedBy(Spacing.compact)) {
            item { Text("固定支出会先冻结预算，支付时不会再次扣减。", modifier = Modifier.padding(top = Spacing.medium)) }
            item { AppTextInput(name, { name = it }, "名称", modifier = Modifier.fillMaxWidth()) }
            item { MoneyField("金额", amount) { amount = it } }
            item { AppTextInput(dueDay, { dueDay = it.filter(Char::isDigit) }, "每月支付日", modifier = Modifier.fillMaxWidth()) }
            item { PrimaryActionButton(onClick = {
                val cents = yuanToCentsOrNull(amount)
                val day = dueDay.toIntOrNull()
                if (name.isBlank() || cents == null || cents <= 0 || day !in 1..31) vm.showInputError("请填写名称、正数金额和 1 至 31 日")
                else vm.addFixed(name, cents, day!!)
            }, modifier = Modifier.fillMaxWidth()) { Text("添加固定支出") } }
            items(state.fixedExpenses, key = { it.instanceId }) { item ->
                DataCard(Modifier.fillMaxWidth()) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        Text("${item.amountCents.money()} · 每月 ${item.dueDay} 日 · ${item.status.label()}", style = YuliangTypography.bodyMedium)
                        if (item.status == FixedExpenseStatus.UPCOMING) Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                            SecondaryActionButton(onClick = { vm.payFixed(item.instanceId) }) { Text("标记已支付") }
                            TextButton(onClick = { vm.skipFixed(item.instanceId) }) { Text("本月跳过") }
                        }
                }
            }
            item { Spacer(Modifier.height(Spacing.large)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SimpleTopBar(title: String, onBack: () -> Unit) = TopAppBar(title = { Text(title) }, navigationIcon = { TextButton(onClick = onBack) { Text("返回") } })
@Composable private fun MoneyField(label: String, value: String, onChange: (String) -> Unit) = AmountInput(value, onChange, "$label（元）", Modifier.fillMaxWidth())
private fun yuanToCentsOrNull(text: String): Long? = try { BigDecimal(text).setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact().takeIf { it >= 0 } } catch (_: Exception) { null }
private fun Long.toYuan() = yuanInput()
private fun FixedExpenseStatus.label() = when (this) { FixedExpenseStatus.UPCOMING -> "待支付"; FixedExpenseStatus.PAID -> "已支付"; FixedExpenseStatus.SKIPPED -> "已跳过" }
