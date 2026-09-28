package com.yuliang.app

import android.content.ContentValues
import android.provider.MediaStore
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class DeviceSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun startupQuickRecordAndSystemBackWorkOnAndroid() {
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText("余量").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("open_record").performClick()
        compose.onNodeWithTag("record_amount").assertIsDisplayed()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("open_record").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("账单").performClick()
        compose.onNodeWithText("全部账单").assertIsDisplayed()
    }

    @Test fun exportIsVerifiedThroughAndroidContentProvider() {
        val context = compose.activity
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "yuliang-test-${System.currentTimeMillis()}.json")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        assertNotNull("Android Downloads provider must create a document", uri)
        try {
            val bytes = "{\"format\":\"yuliang-test\"}".toByteArray()
            val service = (context.application as YuliangApplication).container.dataTransfer
            val result = runBlocking { service.writeAndVerify(resolver, uri!!, bytes) }
            assertEquals(bytes.size.toLong(), result.bytesWritten)
            assertArrayEquals(bytes, resolver.openInputStream(uri!!)?.use { it.readBytes() })
        } finally {
            if (uri != null) resolver.delete(uri, null, null)
        }
    }
}
