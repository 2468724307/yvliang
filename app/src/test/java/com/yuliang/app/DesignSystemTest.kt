package com.yuliang.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import com.yuliang.app.ui.components.AmountText
import com.yuliang.app.ui.theme.YuliangTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h568dp")
class DesignSystemTest {
    @get:Rule val compose = createComposeRule()

    @Test fun longAmountFitsAtSupportedFontScalesOnCompactScreen() {
        val fontScale = mutableFloatStateOf(1f)
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale.floatValue)) {
                YuliangTheme { Box(Modifier.fillMaxWidth()) { AmountText(999_999_999L, large = true) } }
            }
        }
        for (scale in listOf(1f, 1.3f, 1.5f, 2f)) {
            compose.runOnIdle { fontScale.floatValue = scale }
            val node = compose.onNodeWithText("¥9999999.99").assertIsDisplayed()
            assertTrue("Amount must stay within 320dp at $scale font scale", node.getUnclippedBoundsInRoot().right.value <= 320f)
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "night")
class DesignSystemDarkTest {
    @get:Rule val compose = createComposeRule()

    @Test fun systemNightModeUsesV3Surfaces() {
        var background = Color.Unspecified
        compose.setContent { YuliangTheme { background = MaterialTheme.colorScheme.background; AmountText(900L) } }
        compose.onNodeWithText("¥9.00").assertIsDisplayed()
        compose.runOnIdle { assertEquals(Color(0xFF111615), background) }
    }
}
