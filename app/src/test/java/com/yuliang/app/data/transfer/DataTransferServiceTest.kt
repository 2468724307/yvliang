package com.yuliang.app.data.transfer

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yuliang.app.data.database.*
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataTransferServiceTest {
    private lateinit var db: YuliangDatabase
    private lateinit var service: DataTransferService
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, YuliangDatabase::class.java).allowMainThreadQueries().build()
        service = DataTransferService(db)
    }
    @After fun close() = db.close()

    @Test fun backupRoundTripAndCsvContainRealData() = runTest {
        seed()
        val backup = service.createBackup()
        val csv = service.createTransactionsCsv()
        assertTrue(backup.isNotEmpty())
        assertTrue(String(backup).contains("yuliang-backup"))
        assertTrue(String(csv).contains("1200"))
        db.ledgerDao().deleteAll()
        assertEquals(0, db.ledgerDao().countTransactions())
        service.restore(backup)
        assertEquals(1, db.ledgerDao().countTransactions())
    }

    @Test fun invalidImportDoesNotDestroyExistingData() = runTest {
        seed()
        val root = JSONObject(service.createBackup().toString(Charsets.UTF_8))
        root.getJSONArray("transactions").getJSONObject(0).put("categoryId", 999)
        val invalid = root.toString().toByteArray()
        try { service.restore(invalid); fail("Expected validation failure") } catch (_: IllegalArgumentException) { }
        assertEquals(1, db.ledgerDao().countTransactions())
        assertEquals(1, db.categoryDao().getAll().size)
    }

    @Test fun databaseConstraintFailureRollsBackReplacement() = runTest {
        seed()
        val root = JSONObject(service.createBackup().toString(Charsets.UTF_8))
        val duplicate = JSONObject(root.getJSONArray("categories").getJSONObject(0).toString()).put("id", 99)
        root.getJSONArray("categories").put(duplicate)
        try { service.restore(root.toString().toByteArray()); fail("Expected unique constraint failure") } catch (_: Exception) { }
        assertEquals(1, db.ledgerDao().countTransactions())
        assertEquals("餐饮", db.categoryDao().getAll().single().name)
    }

    @Test fun writeAndVerifyCreatesReadableNonEmptyFile() = runTest {
        val target = File(context.cacheDir, "real-export.json")
        val result = service.writeAndVerify(context.contentResolver, Uri.fromFile(target), "{\"ok\":true}".toByteArray())
        assertTrue(target.exists())
        assertEquals(target.length(), result.bytesWritten)
        assertTrue(result.bytesWritten > 0)
    }

    private suspend fun seed() {
        val category = db.categoryDao().insert(CategoryEntity(name = "餐饮", icon = "meal", type = "EXPENSE", sortOrder = 0, isSystem = true))
        val now = System.currentTimeMillis()
        db.monthlyPlanDao().upsert(MonthlyPlanEntity(year = 2026, month = 9, baseIncomeCents = 100_000, savingGoalCents = 10_000, effectiveStartEpochDay = LocalDate.of(2026, 9, 1).toEpochDay(), createdAt = now, updatedAt = now))
        db.ledgerDao().insertTransaction(TransactionEntity(type = "EXPENSE", amountCents = 1_200, categoryId = category, note = "午餐", occurredAt = now, createdAt = now, updatedAt = now))
    }
}
