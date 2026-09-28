package com.yuliang.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
}
