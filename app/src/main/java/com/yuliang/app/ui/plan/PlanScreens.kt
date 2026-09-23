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
        Column(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("存钱目标和安全余额会在月初立即预留。", style = MaterialTheme.typography.bodyMedium)
            MoneyField("本月生活费", base) { base = it }
            MoneyField("存钱目标", saving) { saving = it }
            MoneyField("安全余额", reserve) { reserve = it }
            state.budget?.let { Text("当前可消费：${it.monthlySpendableCents.formatMoney()}") }
            Button(enabled = !state.saving, onClick = {
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
        LazyColumn(Modifier.padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("固定支出会先冻结预算，支付时不会再次扣减。", modifier = Modifier.padding(top = 16.dp)) }
            item { OutlinedTextField(name, { name = it }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = appTextFieldColors()) }
            item { MoneyField("金额", amount) { amount = it } }
            item { OutlinedTextField(dueDay, { dueDay = it.filter(Char::isDigit) }, label = { Text("每月支付日") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = appTextFieldColors()) }
            item { Button(onClick = {
                val cents = yuanToCentsOrNull(amount)
                val day = dueDay.toIntOrNull()
                if (name.isBlank() || cents == null || cents <= 0 || day !in 1..31) vm.showInputError("请填写名称、正数金额和 1 至 31 日")
                else vm.addFixed(name, cents, day!!)
            }, modifier = Modifier.fillMaxWidth()) { Text("添加固定支出") } }
            items(state.fixedExpenses, key = { it.instanceId }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        Text("${item.amountCents.formatMoney()} · 每月 ${item.dueDay} 日 · ${item.status.label()}")
                        if (item.status == FixedExpenseStatus.UPCOMING) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.payFixed(item.instanceId) }) { Text("标记已支付") }
                            TextButton(onClick = { vm.skipFixed(item.instanceId) }) { Text("本月跳过") }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SimpleTopBar(title: String, onBack: () -> Unit) = TopAppBar(title = { Text(title) }, navigationIcon = { TextButton(onClick = onBack) { Text("返回") } })
@Composable private fun MoneyField(label: String, value: String, onChange: (String) -> Unit) = OutlinedTextField(value, { onChange(it.filter { c -> c.isDigit() || c == '.' }) }, label = { Text("$label（元）") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = appTextFieldColors())
private fun yuanToCentsOrNull(text: String): Long? = try { BigDecimal(text).setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact().takeIf { it >= 0 } } catch (_: Exception) { null }
private fun Long.toYuan() = BigDecimal.valueOf(this, 2).stripTrailingZeros().toPlainString()
private fun Long.formatMoney() = "¥" + BigDecimal.valueOf(this, 2).setScale(2).toPlainString()
private fun FixedExpenseStatus.label() = when (this) { FixedExpenseStatus.UPCOMING -> "待支付"; FixedExpenseStatus.PAID -> "已支付"; FixedExpenseStatus.SKIPPED -> "已跳过" }
