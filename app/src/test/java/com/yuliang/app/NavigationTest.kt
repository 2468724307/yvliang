package com.yuliang.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import com.yuliang.app.domain.model.Transaction
import com.yuliang.app.domain.model.TransactionType
import kotlinx.coroutines.runBlocking
import java.time.Instant
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
        compose.onNodeWithText("所选时间内还没有可统计的消费。记录几笔后，这里会显示分类与趋势。").assertIsDisplayed()
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
        compose.waitForIdle()
        println("PLAN_NAV_TREE: " + compose.onRoot().printToString())
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("本月计划").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("本月计划").assertIsDisplayed()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNodeWithText("开始设置").assertIsDisplayed()
    }

    @Test fun quickRecordCapsuleExpandsAndCloses() {
        compose.onNodeWithTag("open_record").assertIsDisplayed().performClick()
        compose.onNodeWithTag("record_amount").assertIsDisplayed()
        compose.onNodeWithText("关闭").performClick()
        compose.waitForIdle()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("open_record").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("open_record").assertExists()
    }

    @Test fun quickRecordReturnsAfterPersistingToLedger() {
        compose.onNodeWithTag("open_record").performClick()
        compose.onNodeWithTag("record_amount").performTextInput("12.34")
        compose.onNodeWithTag("save_record").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("open_record").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("账单").performClick()
        compose.onNodeWithTag("bills_list").performScrollToNode(hasText("−¥12.34"))
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

    @Test fun returningFromDetailKeepsBillSearchAndScroll() {
        val ledger = (compose.activity.application as YuliangApplication).container.ledgerRepository
        runBlocking {
            repeat(16) { index ->
                ledger.add(Transaction(type = TransactionType.EXPENSE, amountCents = 100L + index,
                    occurredAt = Instant.now().minusSeconds(index * 60L), note = "条目 $index"))
            }
        }
        compose.onNodeWithText("账单").performClick()
        compose.onNodeWithText("搜索备注或分类").performTextInput("条目")
        compose.onNodeWithTag("bills_list").performScrollToNode(hasText("条目 12"))
        compose.onNodeWithText("条目 12").performClick()
        compose.onNodeWithText("账单详情").assertIsDisplayed()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNodeWithTag("bills_list").assertExists()
        compose.onNodeWithText("条目 12").assertIsDisplayed()
    }

    @Test fun quickRecordDraftSurvivesActivityRecreation() {
        compose.onNodeWithTag("open_record").performClick()
        compose.onNodeWithTag("record_amount").performTextInput("12.34")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("record_amount").assertTextContains("12.34")
    }
}
