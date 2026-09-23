package com.yuliang.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

object Spacing {
    val tiny = 4.dp
    val small = 8.dp
    val compact = 12.dp
    val medium = 16.dp
    val content = 20.dp
    val large = 24.dp
    val section = 32.dp
}

/** Semantic colors outside Material's standard roles. Use these in screens and charts. */
data class YuliangPalette(
    val brandPressed: Color,
    val brandSoft: Color,
    val amber: Color,
    val amberSoft: Color,
    val borderStrong: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val positive: Color,
    val positiveContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val categoryColors: Map<String, Color>,
) {
    fun category(name: String): Color = categoryColors[name] ?: categoryColors.getValue("其他")
}

private val categories = mapOf(
    "餐饮" to Color(0xFFD48662), "交通" to Color(0xFF668BB5),
    "购物" to Color(0xFFAC82AE), "学习" to Color(0xFF629478),
    "娱乐" to Color(0xFFD4A451), "居住" to Color(0xFF8B7A69),
    "医疗" to Color(0xFFC97676), "通讯" to Color(0xFF6C9298),
    "其他" to Color(0xFF8B9290),
)

private val LightPalette = YuliangPalette(
    brandPressed = Color(0xFF285E58), brandSoft = Color(0xFFEFF7F5),
    amber = Color(0xFFD9A45F), amberSoft = Color(0xFFF7EAD7),
    borderStrong = Color(0xFFCBD3CF), textTertiary = Color(0xFF929B97),
    textDisabled = Color(0xFFB7BFBB), positive = Color(0xFF3F8468),
    positiveContainer = Color(0xFFE8F4ED), warning = Color(0xFFC78A35),
    warningContainer = Color(0xFFFBF1DF), danger = Color(0xFFC85E5E),
    dangerContainer = Color(0xFFFBE9E9), info = Color(0xFF557FA8),
    infoContainer = Color(0xFFEAF1F7), heroStart = Color(0xFF2F6F68),
    heroEnd = Color(0xFF285E58), categoryColors = categories,
)

private val DarkPalette = YuliangPalette(
    brandPressed = Color(0xFF57998F), brandSoft = Color(0xFF23443F),
    amber = Color(0xFFDEB46C), amberSoft = Color(0xFF463B2B),
    borderStrong = Color(0xFF45534E), textTertiary = Color(0xFF79847F),
    textDisabled = Color(0xFF65716C), positive = Color(0xFF70B68C),
    positiveContainer = Color(0xFF253F32), warning = Color(0xFFDEB46C),
    warningContainer = Color(0xFF483B28), danger = Color(0xFFDF8585),
    dangerContainer = Color(0xFF482C2D), info = Color(0xFF8FB3D4),
    infoContainer = Color(0xFF243746), heroStart = Color(0xFF214A46),
    heroEnd = Color(0xFF193C39), categoryColors = categories,
)

private val LocalYuliangPalette = staticCompositionLocalOf { LightPalette }

object AppColors {
    val current: YuliangPalette @Composable get() = LocalYuliangPalette.current
}

@Composable
fun appTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    errorContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    errorBorderColor = MaterialTheme.colorScheme.error,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
)

object YuliangShapes {
    val small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    val medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    val card = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
    val hero = androidx.compose.foundation.shape.RoundedCornerShape(30.dp)
    val pill = androidx.compose.foundation.shape.RoundedCornerShape(50)
}

object MotionTokens {
    const val Fast = 80
    const val Quick = 140
    const val Normal = 220
    const val Container = 320
    const val Slow = 420
    const val Medium = Normal
    const val StaggerDelay = 28
    const val PressedScale = .975f
    const val CardPressedScale = .992f
    val MicroOffset = 4.dp
    val ItemOffset = 8.dp
    val PageOffset = 12.dp
    val PressSpring = spring<Float>(dampingRatio = .85f, stiffness = Spring.StiffnessMedium)
    val SoftSpring = spring<Float>(dampingRatio = .9f, stiffness = Spring.StiffnessMediumLow)
}

private val Light = lightColorScheme(
    primary = Color(0xFF2F6F68),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDEEEA),
    onPrimaryContainer = Color(0xFF202725),
    secondary = Color(0xFF65706C),
    onSecondary = Color.White,
    background = Color(0xFFF5F7F5),
    onBackground = Color(0xFF202725),
    surface = Color.White,
    onSurface = Color(0xFF202725),
    onSurfaceVariant = Color(0xFF65706C),
    surfaceContainer = Color(0xFFEEF2EF),
    surfaceContainerHigh = Color(0xFFE7ECE9),
    outline = Color(0xFFE1E6E3),
    outlineVariant = Color(0xFFCBD3CF),
    error = Color(0xFFC85E5E),
    onError = Color.White,
    errorContainer = Color(0xFFFBE9E9),
    onErrorContainer = Color(0xFF202725),
)
private val Dark = darkColorScheme(
    primary = Color(0xFF77B8AE),
    onPrimary = Color(0xFF003732),
    primaryContainer = Color(0xFF23443F),
    onPrimaryContainer = Color(0xFFF0F4F2),
    secondary = Color(0xFFADB8B4),
    onSecondary = Color(0xFF111615),
    background = Color(0xFF111615),
    onBackground = Color(0xFFF0F4F2),
    surface = Color(0xFF18201E),
    onSurface = Color(0xFFF0F4F2),
    onSurfaceVariant = Color(0xFFADB8B4),
    surfaceContainer = Color(0xFF202A27),
    surfaceContainerHigh = Color(0xFF28332F),
    outline = Color(0xFF303A37),
    outlineVariant = Color(0xFF45534E),
    error = Color(0xFFDF8585),
    onError = Color(0xFF111615),
    errorContainer = Color(0xFF482C2D),
    onErrorContainer = Color(0xFFF0F4F2),
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
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalYuliangPalette provides if (dark) DarkPalette else LightPalette) {
        MaterialTheme(
            colorScheme = if (dark) Dark else Light,
            shapes = AppShapes,
            typography = AppTypography,
            content = content,
        )
    }
}
