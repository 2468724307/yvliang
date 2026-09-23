package com.yuliang.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun launchAndMainNavigationWork() {
        compose.onNodeWithText("余量").assertIsDisplayed()
        compose.onNodeWithText("账单").performClick()
        compose.onNodeWithText("全部账单").assertIsDisplayed()
        compose.onNodeWithText("统计").performClick()
        compose.onNodeWithText("本月还没有可统计的消费。记录几笔后，这里会显示分类与趋势。").assertIsDisplayed()
    }

    @Test fun systemBackPopsSecondaryPage() {
        compose.onNodeWithText("我的").performClick()
        compose.onNodeWithTag("profile_list").performScrollToNode(hasText("关于余量"))
        compose.onNodeWithText("关于余量").performClick()
        compose.onNodeWithText("关于余量").assertIsDisplayed()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNodeWithText("数据管理").assertIsDisplayed()
    }

    @Test fun systemBackPopsPlanPage() {
        compose.onNodeWithText("开始设置").performClick()
        compose.onNodeWithText("本月计划").assertIsDisplayed()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNodeWithText("开始设置").assertIsDisplayed()
    }

    @Test fun quickRecordCapsuleExpandsAndCloses() {
        compose.onNodeWithTag("open_record").assertIsDisplayed().performClick()
        compose.onNodeWithTag("record_amount").assertIsDisplayed()
        compose.onNodeWithText("关闭").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("open_record").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("open_record").assertExists()
    }

    @Test fun quickRecordReturnsAfterPersistingToLedger() {
        compose.onNodeWithTag("open_record").performClick()
        compose.onNodeWithTag("record_amount").performTextInput("12.34")
        compose.onNodeWithTag("save_record").performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("open_record").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("账单").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            try {
                compose.onNodeWithTag("bills_list").performScrollToNode(hasText("−¥12.34"))
                true
            } catch (_: AssertionError) {
                false // Room emits the saved transaction after the sheet closes.
            }
        }
        compose.onNodeWithText("−¥12.34").assertIsDisplayed()
    }

    @Test fun systemBackDismissesQuickRecord() {
        compose.onNodeWithTag("open_record").performClick()
        compose.onNodeWithTag("record_amount").assertIsDisplayed()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("open_record").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("open_record").assertExists()
    }
}
