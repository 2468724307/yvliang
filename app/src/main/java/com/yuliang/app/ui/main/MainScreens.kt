package com.yuliang.app.ui.main

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yuliang.app.BuildConfig
import com.yuliang.app.data.repository.Category
import com.yuliang.app.domain.dashboard.*
import com.yuliang.app.domain.model.*
import com.yuliang.app.ui.components.*
import com.yuliang.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(state: MainUiState, onPlan: () -> Unit, onRecord: () -> Unit, onTransaction: (Long) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.content, Spacing.content, Spacing.content, 112.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        item {
            Text("余量", style = MaterialTheme.typography.headlineLarge)
            Text("先看今天，再决定怎么花", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when (val dashboard = state.dashboard) {
            DashboardResult.NoPlan -> item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(Spacing.large), verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                        Text("先建立本月计划", style = MaterialTheme.typography.titleLarge)
                        Text("填入生活费、存钱目标和安全余额后，余量才能计算今天还能花多少。")
                        PressableButton(onPlan, Modifier.fillMaxWidth(), reduceMotion = state.reduceMotion) { Text("开始设置") }
                    }
                }
            }
            is DashboardResult.Ready -> {
                item {
                    Box(Modifier.fillMaxWidth().heightIn(min = 230.dp)) {
                        TiltBudgetHero(state.reduceMotion, Modifier.matchParentSize()) {
                            Column(Modifier.padding(Spacing.large), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                                Text("今日还能花", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = .85f))
                                RollingMoney(dashboard.budget.todayRemainingCents, reduceMotion = state.reduceMotion)
                                if (dashboard.budget.todayOverspendCents > 0) {
                                    Text("今日已超出建议 ${dashboard.budget.todayOverspendCents.money()}", color = Color.White)
                                } else {
                                    Text("今日建议 ${dashboard.budget.todayAllowanceCents.money()}", color = Color.White.copy(alpha = .85f))
                                }
                                Text(dashboard.budget.riskText(), fontWeight = FontWeight.Medium, color = Color.White)
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.compact)) {
                        MetricCard("本周剩余", dashboard.budget.weekRemainingCents.money(), Modifier.weight(1f))
                        MetricCard("预计月底", dashboard.budget.prediction.label(), Modifier.weight(1f))
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(Spacing.medium), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("本月计划", style = MaterialTheme.typography.titleMedium)
                                Text("剩余 ${dashboard.budget.monthRemainingCents.money()}")
                            }
                            val budgetColor = when (dashboard.budget.budgetRiskLevel) {
                                BudgetRiskLevel.SAFE -> MaterialTheme.colorScheme.primary
                                BudgetRiskLevel.WARNING -> AppColors.current.warning
                                BudgetRiskLevel.RISK -> AppColors.current.danger
                            }
                            LinearProgressIndicator(progress = { dashboard.planProgress }, modifier = Modifier.fillMaxWidth(), color = budgetColor)
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                                Text("●", color = AppColors.current.amber)
                                Text("储蓄目标", style = MaterialTheme.typography.labelLarge)
                            }
                            Text(
                                if (dashboard.budget.savingGoalOnTrack) "存钱目标状态正常" else "提醒：按当前进度，存钱目标可能受影响",
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            val risk = dashboard.budget.budgetRiskLevel
                            Surface(
                                color = when (risk) {
                                    BudgetRiskLevel.SAFE -> AppColors.current.positiveContainer
                                    BudgetRiskLevel.WARNING -> AppColors.current.warningContainer
                                    BudgetRiskLevel.RISK -> AppColors.current.dangerContainer
                                },
                                shape = YuliangShapes.small,
                            ) {
                                Text(dashboard.budget.riskText(), Modifier.padding(Spacing.compact), color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("最近账单", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = onRecord) { Text("＋ 记一笔") }
                    }
                }
                if (dashboard.recentTransactions.isEmpty()) item { EmptyCard("还没有账单，记下第一笔消费吧。") }
                else itemsIndexed(dashboard.recentTransactions, key = { _, item -> item.id }) { index, tx ->
                    StaggeredItem(index, !state.reduceMotion) { TransactionRow(tx, state.categories, onClick = { onTransaction(tx.id) }) }
                }
            }
        }
    }
}

@Composable private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(Spacing.medium), verticalArrangement = Arrangement.spacedBy(Spacing.tiny)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge)
    } }
}

@Composable private fun EmptyCard(text: String) = Card(Modifier.fillMaxWidth()) { Text(text, Modifier.padding(Spacing.large)) }

@Composable
fun BillsScreen(state: MainUiState, onTransaction: (Long) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf<TransactionType?>(null) }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var dateFilter by rememberSaveable { mutableStateOf(BillDateFilter.ALL) }
    val today = LocalDate.now()
    val filtered = state.transactions.filter { tx ->
        val categoryName = state.categories.firstOrNull { it.id == tx.categoryId }?.name.orEmpty()
        val date = tx.date(ZoneId.systemDefault())
        (query.isBlank() || tx.note.orEmpty().contains(query, true) || categoryName.contains(query, true)) &&
            (type == null || tx.type == type) &&
            (categoryId == null || tx.categoryId == categoryId) &&
            when (dateFilter) {
                BillDateFilter.ALL -> true
                BillDateFilter.THIS_MONTH -> date.year == today.year && date.monthValue == today.monthValue
                BillDateFilter.LAST_7_DAYS -> date in today.minusDays(6)..today
            }
    }
    val grouped = filtered.groupBy { it.date(ZoneId.systemDefault()) }.toSortedMap(reverseOrder())
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("bills_list"),
        contentPadding = PaddingValues(Spacing.content, Spacing.content, Spacing.content, 104.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.compact),
    ) {
        item { Text("全部账单", style = MaterialTheme.typography.headlineLarge) }
        item { OutlinedTextField(query, { query = it }, label = { Text("搜索备注或分类") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = appTextFieldColors()) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                item { FilterChip(type == null, { type = null }, { Text("全部") }) }
                item { FilterChip(type == TransactionType.EXPENSE, { type = TransactionType.EXPENSE }, { Text("支出") }) }
                item { FilterChip(type == TransactionType.INCOME, { type = TransactionType.INCOME }, { Text("收入") }) }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                item { FilterChip(dateFilter == BillDateFilter.ALL, { dateFilter = BillDateFilter.ALL }, { Text("不限日期") }) }
                item { FilterChip(dateFilter == BillDateFilter.THIS_MONTH, { dateFilter = BillDateFilter.THIS_MONTH }, { Text("本月") }) }
                item { FilterChip(dateFilter == BillDateFilter.LAST_7_DAYS, { dateFilter = BillDateFilter.LAST_7_DAYS }, { Text("近 7 天") }) }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                item { FilterChip(categoryId == null, { categoryId = null }, { Text("全部分类") }) }
                items(state.categories, key = { it.id }) { category -> FilterChip(categoryId == category.id, { categoryId = category.id }, { Text(category.name) }) }
            }
        }
        if (filtered.isEmpty()) item { EmptyCard("没有符合条件的账单。") }
        grouped.forEach { (date, rows) ->
            item(key = "header-$date") { Text(date.format(DateTimeFormatter.ofPattern("M月d日 EEEE")), style = MaterialTheme.typography.titleMedium) }
            itemsIndexed(rows, key = { _, item -> item.id }) { index, tx ->
                StaggeredItem(index, !state.reduceMotion) { TransactionRow(tx, state.categories) { onTransaction(tx.id) } }
            }
        }
    }
}

private enum class BillDateFilter { ALL, THIS_MONTH, LAST_7_DAYS }

@Composable
fun StatisticsScreen(state: MainUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.content, Spacing.content, Spacing.content, 104.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        item { Text("统计", style = MaterialTheme.typography.headlineLarge) }
        when (val stats = state.statistics) {
            StatisticsResult.Empty -> item { EmptyCard("本月还没有可统计的消费。记录几笔后，这里会显示分类与趋势。") }
            is StatisticsResult.Content -> {
                item {
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(Spacing.large)) {
                        Text("本月支出", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        RollingMoney(stats.totalExpenseCents, reduceMotion = state.reduceMotion)
                        Text("最高消费分类：${stats.topCategory}；近期有效消费日日均 ${stats.recentDailyAverageCents.money()}。")
                    } }
                }
                item { Text("钱花到哪里", style = MaterialTheme.typography.titleLarge) }
                items(stats.categorySlices.take(6), key = { it.categoryId ?: -1L }) { slice ->
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.tiny)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(slice.label); Text(slice.amountCents.money()) }
                        LinearProgressIndicator(progress = { slice.fraction }, modifier = Modifier.fillMaxWidth(), color = AppColors.current.category(slice.label))
                    }
                }
                item {
                    Text("每日趋势", style = MaterialTheme.typography.titleLarge)
                    TrendChart(stats.dailyTrend)
                    Text(stats.trendSummary())
                }
                item {
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(Spacing.medium), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                        Text("预算判断", style = MaterialTheme.typography.titleMedium)
                        Text(stats.prediction.label())
                        val risk = stats.riskLevel
                        if (risk == null) Text("先建立本月计划以获得风险判断") else Surface(
                            color = when (risk) {
                                BudgetRiskLevel.SAFE -> AppColors.current.positiveContainer
                                BudgetRiskLevel.WARNING -> AppColors.current.warningContainer
                                BudgetRiskLevel.RISK -> AppColors.current.dangerContainer
                            },
                            shape = YuliangShapes.small,
                        ) {
                            Text("预算状态：${risk.riskText()}", Modifier.padding(Spacing.compact), color = MaterialTheme.colorScheme.onSurface)
                        }
                    } }
                }
            }
        }
    }
}

@Composable private fun TrendChart(points: List<DailySpend>) {
    val max = points.maxOfOrNull { it.amountCents }?.coerceAtLeast(1) ?: 1
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var chartWidth by remember { mutableFloatStateOf(1f) }
    fun selectAt(x: Float) {
        if (points.isNotEmpty()) selectedIndex = ((x / chartWidth) * (points.size - 1)).toInt().coerceIn(0, points.lastIndex)
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        selectedIndex?.let { index -> Text("${points[index].date.format(DateTimeFormatter.ofPattern("M月d日"))} · ${points[index].amountCents.money()}") }
        Canvas(
            Modifier.fillMaxWidth().height(150.dp)
                .onSizeChanged { chartWidth = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(points) {
                    detectHorizontalDragGestures(
                        onDragStart = { selectAt(it.x) },
                        onHorizontalDrag = { change, _ -> selectAt(change.position.x) },
                    )
                }
                .semantics { contentDescription = "本月每日消费趋势，共 ${points.count { it.amountCents > 0 }} 个消费日；可横向拖动查看日期金额" }
        ) {
            if (points.size < 2) return@Canvas
            for (fraction in listOf(.25f, .5f, .75f)) {
                val y = size.height * fraction
                drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            val path = Path()
            points.forEachIndexed { index, point ->
                val x = size.width * index / (points.size - 1)
                val y = size.height - size.height * (point.amountCents.toFloat() / max.toFloat())
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, lineColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
            selectedIndex?.let { index ->
                val x = size.width * index / (points.size - 1)
                val y = size.height - size.height * (points[index].amountCents.toFloat() / max.toFloat())
                drawCircle(lineColor.copy(alpha = .18f), radius = 20f, center = androidx.compose.ui.geometry.Offset(x, y))
                drawCircle(lineColor, radius = 8f, center = androidx.compose.ui.geometry.Offset(x, y))
            }
        }
    }
}

@Composable
fun ProfileScreen(
    state: MainUiState,
    onPlan: () -> Unit,
    onFixed: () -> Unit,
    onCategories: () -> Unit,
    onData: () -> Unit,
    onAbout: () -> Unit,
    onReduceMotion: (Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("profile_list"),
        contentPadding = PaddingValues(Spacing.content, Spacing.content, Spacing.content, 104.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.compact),
    ) {
        item { Text("我的", style = MaterialTheme.typography.headlineLarge) }
        item { SettingRow("本月计划", "生活费、存钱目标与安全余额", onPlan) }
        item { SettingRow("固定支出", "按月冻结并跟踪支付状态", onFixed) }
        item { SettingRow("分类管理", "新增或归档收入与支出分类", onCategories) }
        item { SettingRow("数据管理", "备份、恢复、CSV 导出与分享", onData) }
        item {
            Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(Spacing.medium), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("减少动画", style = MaterialTheme.typography.titleMedium); Text("关闭 3D 微倾、弹簧与交错入场") }
                Switch(state.reduceMotion, onReduceMotion)
            } }
        }
        item { SettingRow("关于余量", "版本、隐私与本地存储说明", onAbout) }
    }
}

@Composable
fun CategoryManagementScreen(state: MainUiState, vm: MainViewModel, onBack: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var icon by rememberSaveable { mutableStateOf("·") }
    var type by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    SimplePage("分类管理", onBack) {
        Text("归档分类不会删除历史账单中的分类引用。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(name, { name = it.take(12) }, label = { Text("分类名称") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = appTextFieldColors())
        OutlinedTextField(icon, { icon = it.take(2) }, label = { Text("图标或符号") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = appTextFieldColors())
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            FilterChip(type == TransactionType.EXPENSE, { type = TransactionType.EXPENSE }, { Text("支出") })
            FilterChip(type == TransactionType.INCOME, { type = TransactionType.INCOME }, { Text("收入") })
        }
        Button(onClick = {
            if (name.isBlank()) vm.showMessage("请填写分类名称") else { vm.addCategory(name, icon, type); name = "" }
        }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("添加分类") }
        HorizontalDivider()
        state.categories.forEach { category ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(Spacing.medium), verticalAlignment = Alignment.CenterVertically) {
                    Text(category.icon, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(Spacing.compact))
                    Column(Modifier.weight(1f)) { Text(category.name); Text(category.type.label(), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (category.isEnabled) TextButton(onClick = { vm.archiveCategory(category.id) }) { Text("归档") }
                    else Text("已归档", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable private fun SettingRow(title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.medium)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(state: MainUiState, id: Long, onBack: () -> Unit, vm: MainViewModel) {
    val transaction = state.transactions.firstOrNull { it.id == id }
    if (transaction == null) { SimplePage("账单不存在", onBack) { Text("这笔账单可能已经被删除。") }; return }
    var editing by rememberSaveable { mutableStateOf(false) }
    var amount by rememberSaveable(transaction.id) { mutableStateOf(transaction.amountCents.yuanInput()) }
    var note by rememberSaveable(transaction.id) { mutableStateOf(transaction.note.orEmpty()) }
    var editCategoryId by rememberSaveable(transaction.id) { mutableStateOf(transaction.categoryId) }
    var editDate by remember(transaction.id) { mutableStateOf(transaction.date(ZoneId.systemDefault())) }
    var showEditDatePicker by remember { mutableStateOf(false) }
    val editDatePickerState = rememberDatePickerState(initialSelectedDateMillis = editDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    var confirmDelete by remember { mutableStateOf(false) }
    SimplePage("账单详情", onBack) {
        Text(transaction.type.label(), style = MaterialTheme.typography.titleMedium)
        if (editing) {
            OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("金额（元）") }, modifier = Modifier.fillMaxWidth(), colors = appTextFieldColors())
            OutlinedTextField(note, { note = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), colors = appTextFieldColors())
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                items(state.categories.filter { (it.isEnabled || it.id == transaction.categoryId) && it.type == transaction.type }, key = { it.id }) { category ->
                    FilterChip(editCategoryId == category.id, { editCategoryId = category.id }, { Text(category.name) })
                }
            }
            OutlinedButton(onClick = { showEditDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("日期：${editDate.format(DateTimeFormatter.ofPattern("yyyy年M月d日"))}")
            }
            PressableButton(onClick = {
                val cents = yuanToCentsOrNull(amount)
                if (cents == null) vm.showMessage("请输入有效正数金额，最多两位小数")
                else {
                    val oldTime = transaction.occurredAt.atZone(ZoneId.systemDefault()).toLocalTime()
                    val occurredAt = editDate.atTime(oldTime).atZone(ZoneId.systemDefault()).toInstant()
                    vm.updateTransaction(transaction.id, transaction.type, cents, editCategoryId, note, occurredAt, transaction.incomeAllocation)
                    editing = false
                }
            }, modifier = Modifier.fillMaxWidth(), reduceMotion = state.reduceMotion) { Text("保存修改") }
        } else {
            Text(transaction.amountCents.money(), style = MaterialTheme.typography.displayLarge)
            Text(state.categories.firstOrNull { it.id == transaction.categoryId }?.name ?: "未分类")
            Text(transaction.occurredAt.displayDate())
            if (!transaction.note.isNullOrBlank()) Text(transaction.note)
            if (transaction.linkedFixedExpenseId == null) {
                Button(onClick = { editing = true }, modifier = Modifier.fillMaxWidth()) { Text("编辑") }
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("删除账单", color = MaterialTheme.colorScheme.onSurface) }
            } else Text("此账单由固定支出生成，请在固定支出中管理。")
        }
    }
    if (confirmDelete) AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = { confirmDelete = false },
        title = { Text("删除这笔账单？") },
        text = { Text("删除后预算与统计会立即重新计算，此操作无法撤销。") },
        confirmButton = { TextButton(onClick = { vm.deleteTransaction(id); confirmDelete = false; onBack() }) { Text("删除") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
    )
    if (showEditDatePicker) DatePickerDialog(
        onDismissRequest = { showEditDatePicker = false },
        confirmButton = { TextButton(onClick = {
            editDatePickerState.selectedDateMillis?.let { editDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
            showEditDatePicker = false
        }) { Text("确定") } },
        dismissButton = { TextButton(onClick = { showEditDatePicker = false }) { Text("取消") } },
    ) { DatePicker(editDatePickerState) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickRecordPanel(state: MainUiState, vm: MainViewModel, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var categoryId by remember { mutableStateOf<Long?>(state.categories.firstOrNull { it.isEnabled && it.type == TransactionType.EXPENSE }?.id) }
    var showMore by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var allocation by remember { mutableStateOf(IncomeAllocation.SPENDABLE) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    LaunchedEffect(state.categories, type) {
        if (state.categories.none { it.id == categoryId && it.isEnabled && it.type == type }) {
            categoryId = state.categories.firstOrNull { it.isEnabled && it.type == type }?.id
        }
    }
    val cents = yuanToCentsOrNull(amount)
    val ready = state.dashboard as? DashboardResult.Ready
    val impact = if (type == TransactionType.EXPENSE && cents != null && ready != null) TransactionImpactCalculator.afterExpense(ready.budget.todayRemainingCents, cents) else null
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        shape = YuliangShapes.hero,
        modifier = Modifier.fillMaxWidth().padding(Spacing.compact).animateContentSize(tween(if (state.reduceMotion) 0 else MotionTokens.Slow)),
    ) {
        Column(Modifier.padding(Spacing.content), verticalArrangement = Arrangement.spacedBy(Spacing.compact)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("记一笔", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
            OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("金额（元）") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("record_amount"), colors = appTextFieldColors())
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                items(state.categories.filter { it.isEnabled && it.type == type }, key = { it.id }) { category ->
                    FilterChip(categoryId == category.id, { categoryId = category.id }, { Text("${category.icon} ${category.name}") })
                }
            }
            impact?.let { Text(if (it.overspendCents > 0) "记账后今日将超出建议 ${it.overspendCents.money()}" else "记账后今日剩余 ${it.remainingCents.money()}") }
            AnimatedVisibility(showMore) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.compact)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                        FilterChip(type == TransactionType.EXPENSE, { type = TransactionType.EXPENSE; categoryId = state.categories.firstOrNull { it.isEnabled && it.type == TransactionType.EXPENSE }?.id }, { Text("支出") })
                        FilterChip(type == TransactionType.INCOME, { type = TransactionType.INCOME; categoryId = state.categories.firstOrNull { it.isEnabled && it.type == TransactionType.INCOME }?.id }, { Text("收入") })
                    }
                    if (type == TransactionType.INCOME) {
                        Text("收入归属", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                            items(IncomeAllocation.entries) { item -> FilterChip(allocation == item, { allocation = item }, { Text(item.label()) }) }
                        }
                    }
                    OutlinedTextField(note, { note = it }, label = { Text("备注（可选）") }, modifier = Modifier.fillMaxWidth(), colors = appTextFieldColors())
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("日期：${selectedDate.format(DateTimeFormatter.ofPattern("yyyy年M月d日"))}")
                    }
                }
            }
            TextButton(onClick = { showMore = !showMore }) { Text(if (showMore) "收起更多" else "更多") }
            PressableButton(onClick = {
                if (saving) return@PressableButton
                if (cents == null || categoryId == null) vm.showMessage("请输入有效金额并选择分类")
                else {
                    saving = true
                    val occurredAt = selectedDate.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant()
                    vm.addTransaction(type, cents, categoryId, note, occurredAt, if (type == TransactionType.INCOME) allocation else null) { saved ->
                        saving = false
                        if (saved) onDismiss()
                    }
                }
            }, modifier = Modifier.fillMaxWidth().testTag("save_record"), enabled = !state.busy && !saving, reduceMotion = state.reduceMotion) { Text(if (saving || state.busy) "保存中" else "保存") }
        }
    }
    if (showDatePicker) DatePickerDialog(
        onDismissRequest = { showDatePicker = false },
        confirmButton = { TextButton(onClick = {
            datePickerState.selectedDateMillis?.let { selectedDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
            showDatePicker = false
        }) { Text("确定") } },
        dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
    ) { DatePicker(datePickerState) }
}

@Composable
fun DataManagementScreen(state: MainUiState, vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingShare by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(state.export?.mimeType ?: "application/octet-stream")) { uri ->
        val payload = state.export
        if (uri == null || payload == null) { vm.exportHandled(); return@rememberLauncherForActivityResult }
        scope.launch {
            val result = vm.writeExportAwait(context.contentResolver, uri, payload)
            if (pendingShare && result.isSuccess) {
                val intent = Intent(Intent.ACTION_SEND).setType(payload.mimeType).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(Intent.createChooser(intent, "分享余量文件"))
            }
            pendingShare = false
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) restoreUri = uri }
    LaunchedEffect(state.export) { state.export?.let { create.launch(it.name) } }
    SimplePage("数据管理", onBack) {
        Text("完整备份包含计划、分类、固定支出和账单。恢复会覆盖当前数据。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = { pendingShare = false; vm.prepareBackup() }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("保存完整备份（JSON）") }
        OutlinedButton(onClick = { pendingShare = false; vm.prepareCsv() }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("导出账单（CSV）") }
        OutlinedButton(onClick = { pendingShare = true; vm.prepareBackup() }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("分享完整备份") }
        HorizontalDivider()
        Text("恢复前会完整校验文件版本与引用关系；校验或写入失败时，现有数据保持不变。")
        Button(onClick = { open.launch(arrayOf("application/json", "text/plain")) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("从备份恢复") }
    }
    restoreUri?.let { uri -> AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = { restoreUri = null },
        title = { Text("确认覆盖当前数据？") },
        text = { Text("恢复将用备份中的计划、分类、固定支出和账单替换当前数据。文件无效或导入失败时不会更改现有数据。") },
        confirmButton = { TextButton(onClick = { vm.restore(context.contentResolver, uri); restoreUri = null }) { Text("确认恢复") } },
        dismissButton = { TextButton(onClick = { restoreUri = null }) { Text("取消") } },
    ) }
}

@Composable
fun AboutScreen(onBack: () -> Unit) = SimplePage("关于余量", onBack) {
    Text("余量", style = MaterialTheme.typography.headlineLarge)
    Text("版本 ${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）")
    Text("余量是一款面向生活费规划的本地优先记账 App。数据默认只保存在设备本地，不要求注册账号。")
    Text("预算结果来自统一 Budget Engine；预测属于估计，记录不足时不会强行给出结果。")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimplePage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { TextButton(onClick = onBack) { Text("返回") } }) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(Spacing.content), verticalArrangement = Arrangement.spacedBy(Spacing.medium), content = content)
    }
}

@Composable
private fun TransactionRow(tx: Transaction, categories: List<Category>, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(Spacing.medium), verticalAlignment = Alignment.CenterVertically) {
            val category = categories.firstOrNull { it.id == tx.categoryId }
            Text(category?.icon ?: "·", style = MaterialTheme.typography.titleLarge, color = AppColors.current.category(category?.name ?: "其他"))
            Spacer(Modifier.width(Spacing.compact))
            Column(Modifier.weight(1f)) {
                Text(category?.name ?: "未分类", style = MaterialTheme.typography.titleMedium)
                Text(tx.note?.takeIf(String::isNotBlank) ?: tx.occurredAt.displayDate(), maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text((if (tx.type == TransactionType.EXPENSE) "−" else "+") + tx.amountCents.money(), color = if (tx.type == TransactionType.EXPENSE) MaterialTheme.colorScheme.onSurface else AppColors.current.positive)
        }
    }
}

@Composable
private fun StaggeredItem(index: Int, enabled: Boolean, content: @Composable () -> Unit) {
    val animate = enabled && index < 4
    var visible by rememberSaveable { mutableStateOf(!animate) }
    LaunchedEffect(enabled) {
        if (!animate) visible = true
        else if (!visible) {
            delay((index * MotionTokens.StaggerDelay).toLong())
            visible = true
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(MotionTokens.Medium)) + slideInVertically(tween(MotionTokens.Medium)) { 18 },
    ) { content() }
}

private fun TransactionType.label() = when (this) { TransactionType.EXPENSE -> "支出"; TransactionType.INCOME -> "收入"; TransactionType.TRANSFER -> "转账" }
private fun IncomeAllocation.label() = when (this) { IncomeAllocation.SPENDABLE -> "加入可消费"; IncomeAllocation.SAVING -> "加入储蓄"; IncomeAllocation.RECORD_ONLY -> "仅记录" }
private fun BudgetRiskLevel.riskText() = when (this) { BudgetRiskLevel.SAFE -> "状态正常"; BudgetRiskLevel.WARNING -> "注意：接近预算线"; BudgetRiskLevel.RISK -> "可能超支，请检查计划" }
private fun BudgetResult.riskText() = budgetRiskLevel.riskText()
private fun Prediction?.label() = when (this) { is Prediction.Available -> "预计月底剩余 ${projectedMonthEndBalanceCents.money()}"; is Prediction.InsufficientData -> "记录几天后即可生成月底预测（当前 $sampleDays 天）"; null -> "暂无预测" }
private fun StatisticsResult.Content.trendSummary(): String {
    val active = dailyTrend.filter { it.amountCents > 0 }
    if (active.size < 2) return "当前消费日较少，趋势仅供查看。"
    val split = active.size / 2
    val first = active.take(split).map { it.amountCents }.average()
    val second = active.drop(split).map { it.amountCents }.average()
    return if (second > first) "近期日消费速度有所上升。" else "近期日消费速度较前段平稳或下降。"
}
