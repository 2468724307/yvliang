package com.yuliang.app.data.transfer

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import com.yuliang.app.data.database.*
import com.yuliang.app.data.settings.UserSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

data class TransferResult(val bytesWritten: Long)

class DataTransferService(private val db: YuliangDatabase, private val settings: UserSettingsStore? = null) {
    companion object { const val BACKUP_VERSION = 1 }

    suspend fun createBackup(): ByteArray = withContext(Dispatchers.IO) {
        val root = JSONObject()
            .put("format", "yuliang-backup")
            .put("version", BACKUP_VERSION)
            .put("createdAt", System.currentTimeMillis())
            .put("settings", JSONObject().put("reduceMotion", settings?.reduceMotion?.first() ?: false))
            .put("monthlyPlans", JSONArray(db.monthlyPlanDao().getAll().map(::planJson)))
            .put("categories", JSONArray(db.categoryDao().getAll().map(::categoryJson)))
            .put("fixedExpenseTemplates", JSONArray(db.fixedExpenseDao().getAllTemplates().map(::templateJson)))
            .put("fixedExpenseInstances", JSONArray(db.fixedExpenseDao().getAllInstances().map(::instanceJson)))
            .put("transactions", JSONArray(db.ledgerDao().getAll().map(::transactionJson)))
        root.toString(2).toByteArray(StandardCharsets.UTF_8)
    }

    suspend fun createTransactionsCsv(): ByteArray = withContext(Dispatchers.IO) {
        val lines = buildList {
            add("id,type,amountCents,categoryId,note,occurredAt,incomeAllocation,linkedFixedExpenseId")
            db.ledgerDao().getAll().forEach { tx ->
                add(listOf(tx.id, tx.type, tx.amountCents, tx.categoryId ?: "", csv(tx.note.orEmpty()), tx.occurredAt, tx.incomeAllocation ?: "", tx.linkedFixedExpenseId ?: "").joinToString(","))
            }
        }
        ("\uFEFF" + lines.joinToString("\r\n")).toByteArray(StandardCharsets.UTF_8)
    }

    suspend fun restore(bytes: ByteArray) = withContext(Dispatchers.IO) {
        require(bytes.isNotEmpty()) { "备份文件为空" }
        val snapshot = parseAndValidate(bytes)
        db.withTransaction {
            val ledger = db.ledgerDao()
            val fixed = db.fixedExpenseDao()
            ledger.deleteAll()
            fixed.deleteAllInstances()
            fixed.deleteAllTemplates()
            db.monthlyPlanDao().deleteAll()
            db.categoryDao().deleteAll()
            db.categoryDao().insertAll(snapshot.categories)
            db.monthlyPlanDao().insertAll(snapshot.plans)
            fixed.insertTemplates(snapshot.templates)
            fixed.insertInstances(snapshot.instances)
            ledger.insertAll(snapshot.transactions)
        }
        // Room data is the critical restore boundary. A non-critical preference write must not
        // turn an already committed, valid restore into a reported failure.
        runCatching { settings?.setReduceMotion(snapshot.reduceMotion) }
    }

    suspend fun writeAndVerify(resolver: ContentResolver, uri: Uri, bytes: ByteArray): TransferResult = withContext(Dispatchers.IO) {
        require(bytes.isNotEmpty()) { "没有可写入的数据" }
        resolver.openOutputStream(uri, "wt")?.use { it.write(bytes); it.flush() }
            ?: error("无法打开目标文件")
        val size = resolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(8 * 1024)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
            }
            total
        } ?: 0
        check(size > 0) { "文件写入后无法读取" }
        TransferResult(size)
    }

    suspend fun read(resolver: ContentResolver, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("无法读取所选文件")
    }

    private fun parseAndValidate(bytes: ByteArray): Snapshot {
        val root = try { JSONObject(String(bytes, StandardCharsets.UTF_8)) } catch (_: Exception) {
            throw IllegalArgumentException("不是有效的余量备份文件")
        }
        require(root.optString("format") == "yuliang-backup") { "备份格式不匹配" }
        require(root.optInt("version", -1) == BACKUP_VERSION) { "暂不支持此备份版本" }
        val snapshot = Snapshot(
            plans = root.array("monthlyPlans").objects(::parsePlan),
            categories = root.array("categories").objects(::parseCategory),
            templates = root.array("fixedExpenseTemplates").objects(::parseTemplate),
            instances = root.array("fixedExpenseInstances").objects(::parseInstance),
            transactions = root.array("transactions").objects(::parseTransaction),
            reduceMotion = root.optJSONObject("settings")?.optBoolean("reduceMotion", false) ?: false,
        )
        validate(snapshot)
        return snapshot
    }

    private fun validate(s: Snapshot) {
        require(s.plans.all { it.month in 1..12 && it.baseIncomeCents >= 0 && it.savingGoalCents >= 0 && it.safetyReserveCents >= 0 }) { "计划数据无效" }
        require(s.plans.distinctBy { it.year to it.month }.size == s.plans.size) { "存在重复月份计划" }
        requireUniqueIds(s.categories.map { it.id }, "分类")
        requireUniqueIds(s.templates.map { it.id }, "固定支出模板")
        requireUniqueIds(s.instances.map { it.id }, "固定支出实例")
        requireUniqueIds(s.transactions.map { it.id }, "账单")
        val categoryIds = s.categories.map { it.id }.toSet()
        val templateIds = s.templates.map { it.id }.toSet()
        val instanceIds = s.instances.map { it.id }.toSet()
        val transactionIds = s.transactions.map { it.id }.toSet()
        require(s.categories.all { it.name.isNotBlank() && it.type in setOf("EXPENSE", "INCOME") }) { "分类数据无效" }
        require(s.templates.all { it.amountCents > 0 && it.dueDay in 1..31 && (it.categoryId == null || it.categoryId in categoryIds) }) { "固定支出模板无效" }
        require(s.instances.all { it.templateId in templateIds && it.month in 1..12 && it.status in setOf("UPCOMING", "PAID", "SKIPPED") }) { "固定支出实例无效" }
        require(s.transactions.all { it.amountCents > 0 && it.type in setOf("EXPENSE", "INCOME", "TRANSFER") && (it.categoryId == null || it.categoryId in categoryIds) }) { "账单数据无效" }
        require(s.transactions.all { it.linkedFixedExpenseId == null || it.linkedFixedExpenseId in instanceIds }) { "账单引用的固定支出不存在" }
        require(s.instances.all { it.linkedTransactionId == null || it.linkedTransactionId in transactionIds }) { "固定支出引用的账单不存在" }
    }

    private fun requireUniqueIds(ids: List<Long>, label: String) {
        require(ids.all { it > 0 } && ids.distinct().size == ids.size) { "$label ID 无效或重复" }
    }
}

private data class Snapshot(
    val plans: List<MonthlyPlanEntity>,
    val categories: List<CategoryEntity>,
    val templates: List<FixedExpenseTemplateEntity>,
    val instances: List<FixedExpenseInstanceEntity>,
    val transactions: List<TransactionEntity>,
    val reduceMotion: Boolean,
)

private fun JSONObject.array(name: String): JSONArray = optJSONArray(name) ?: throw IllegalArgumentException("备份缺少 $name")
private fun <T> JSONArray.objects(mapper: (JSONObject) -> T): List<T> = (0 until length()).map { mapper(getJSONObject(it)) }
private fun JSONObject.nullableLong(name: String): Long? = if (isNull(name)) null else getLong(name)
private fun JSONObject.nullableString(name: String): String? = if (isNull(name)) null else getString(name)
private fun csv(value: String) = "\"${value.replace("\"", "\"\"")}\""

private fun planJson(v: MonthlyPlanEntity) = JSONObject().put("id", v.id).put("year", v.year).put("month", v.month).put("baseIncomeCents", v.baseIncomeCents).put("savingGoalCents", v.savingGoalCents).put("safetyReserveCents", v.safetyReserveCents).put("effectiveStartEpochDay", v.effectiveStartEpochDay).put("createdAt", v.createdAt).put("updatedAt", v.updatedAt)
private fun categoryJson(v: CategoryEntity) = JSONObject().put("id", v.id).put("name", v.name).put("icon", v.icon).put("type", v.type).put("sortOrder", v.sortOrder).put("isSystem", v.isSystem).put("isEnabled", v.isEnabled)
private fun templateJson(v: FixedExpenseTemplateEntity) = JSONObject().put("id", v.id).put("name", v.name).put("amountCents", v.amountCents).put("dueDay", v.dueDay).put("categoryId", v.categoryId ?: JSONObject.NULL).put("repeatMonthly", v.repeatMonthly).put("isEnabled", v.isEnabled).put("createdAt", v.createdAt).put("updatedAt", v.updatedAt)
private fun instanceJson(v: FixedExpenseInstanceEntity) = JSONObject().put("id", v.id).put("templateId", v.templateId).put("year", v.year).put("month", v.month).put("status", v.status).put("linkedTransactionId", v.linkedTransactionId ?: JSONObject.NULL).put("createdAt", v.createdAt).put("updatedAt", v.updatedAt)
private fun transactionJson(v: TransactionEntity) = JSONObject().put("id", v.id).put("type", v.type).put("amountCents", v.amountCents).put("categoryId", v.categoryId ?: JSONObject.NULL).put("accountId", v.accountId ?: JSONObject.NULL).put("note", v.note ?: JSONObject.NULL).put("occurredAt", v.occurredAt).put("incomeAllocation", v.incomeAllocation ?: JSONObject.NULL).put("linkedFixedExpenseId", v.linkedFixedExpenseId ?: JSONObject.NULL).put("createdAt", v.createdAt).put("updatedAt", v.updatedAt)

private fun parsePlan(o: JSONObject) = MonthlyPlanEntity(o.getLong("id"), o.getInt("year"), o.getInt("month"), o.getLong("baseIncomeCents"), o.getLong("savingGoalCents"), o.optLong("safetyReserveCents", 0), o.getLong("effectiveStartEpochDay"), o.getLong("createdAt"), o.getLong("updatedAt"))
private fun parseCategory(o: JSONObject) = CategoryEntity(o.getLong("id"), o.getString("name"), o.getString("icon"), o.getString("type"), o.getInt("sortOrder"), o.getBoolean("isSystem"), o.optBoolean("isEnabled", true))
private fun parseTemplate(o: JSONObject) = FixedExpenseTemplateEntity(o.getLong("id"), o.getString("name"), o.getLong("amountCents"), o.getInt("dueDay"), o.nullableLong("categoryId"), o.optBoolean("repeatMonthly", true), o.optBoolean("isEnabled", true), o.getLong("createdAt"), o.getLong("updatedAt"))
private fun parseInstance(o: JSONObject) = FixedExpenseInstanceEntity(o.getLong("id"), o.getLong("templateId"), o.getInt("year"), o.getInt("month"), o.getString("status"), o.nullableLong("linkedTransactionId"), o.getLong("createdAt"), o.getLong("updatedAt"))
private fun parseTransaction(o: JSONObject) = TransactionEntity(o.getLong("id"), o.getString("type"), o.getLong("amountCents"), o.nullableLong("categoryId"), o.nullableLong("accountId"), o.nullableString("note"), o.getLong("occurredAt"), o.nullableString("incomeAllocation"), o.nullableLong("linkedFixedExpenseId"), o.getLong("createdAt"), o.getLong("updatedAt"))
