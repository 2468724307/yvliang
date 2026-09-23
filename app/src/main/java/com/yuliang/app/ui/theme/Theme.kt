package com.yuliang.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

object Spacing {
    val tiny = 4.dp
    val small = 8.dp
    val compact = 12.dp
    val medium = 16.dp
    val content = 20.dp
    val large = 24.dp
    val section = 32.dp
}

object YuliangColors {
    val Expense = Color(0xFFB6534D)
    val Income = Color(0xFF237A66)
    val Saving = Color(0xFF4D65A7)
    val BudgetSafe = Color(0xFF3D8C78)
    val BudgetWarning = Color(0xFFC18424)
    val BudgetRisk = Color(0xFFB64D4D)
}

object YuliangShapes {
    val small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    val medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    val card = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
    val hero = androidx.compose.foundation.shape.RoundedCornerShape(30.dp)
    val pill = androidx.compose.foundation.shape.RoundedCornerShape(50)
}

object MotionTokens {
    const val Fast = 90
    const val Medium = 220
    const val Slow = 340
    const val StaggerDelay = 28
}

private val Light = lightColorScheme(
    primary = Color(0xFF306B66),
    onPrimary = Color.White,
    secondary = Color(0xFF526861),
    surface = Color(0xFFF8FAF7),
    surfaceContainer = Color(0xFFEEF3EF),
    outline = Color(0xFF778781),
    error = YuliangColors.BudgetRisk,
)
private val Dark = darkColorScheme(
    primary = Color(0xFF9FD0C7),
    onPrimary = Color(0xFF003732),
    secondary = Color(0xFFB6CCC5),
    surface = Color(0xFF101513),
    surfaceContainer = Color(0xFF1B2421),
    outline = Color(0xFF899993),
    error = Color(0xFFFFB4AB),
)

private val AppShapes = Shapes(
    small = YuliangShapes.small,
    medium = YuliangShapes.medium,
    large = YuliangShapes.card,
    extraLarge = YuliangShapes.hero,
)

private val AppTypography = Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(fontSize = 48.sp, lineHeight = 54.sp, fontWeight = FontWeight.SemiBold),
    headlineLarge = androidx.compose.ui.text.TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = androidx.compose.ui.text.TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = androidx.compose.ui.text.TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun YuliangTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
