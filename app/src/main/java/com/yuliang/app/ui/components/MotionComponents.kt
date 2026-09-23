package com.yuliang.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.yuliang.app.ui.theme.AppColors
import com.yuliang.app.ui.theme.MotionTokens
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YuliangBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (visible) ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier.navigationBarsPadding().padding(PaddingValues(horizontal = 20.dp, vertical = 12.dp)),
            content = content,
        )
    }
}

@Composable
fun PressableButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    reduceMotion: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) 0.96f else 1f,
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "buttonPress",
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(),
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        content = content,
    )
}

@Composable
fun RollingMoney(cents: Long, modifier: Modifier = Modifier, reduceMotion: Boolean = false) {
    val animated by animateFloatAsState(
        targetValue = cents.toFloat(),
        animationSpec = if (reduceMotion) snap() else tween(MotionTokens.Medium, easing = FastOutSlowInEasing),
        label = "rollingMoney",
    )
    androidx.compose.material3.Text(
        text = "¥" + BigDecimal.valueOf(animated.toLong(), 2).setScale(2).toPlainString(),
        style = MaterialTheme.typography.displayLarge,
        modifier = modifier,
    )
}

@Composable
fun TiltBudgetHero(
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var touch by remember { mutableStateOf<Offset?>(null) }
    var cardSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    val targetX = touch?.let { ((it.y / cardSize.height.coerceAtLeast(1f)) - .5f) * -6f } ?: 0f
    val targetY = touch?.let { ((it.x / cardSize.width.coerceAtLeast(1f)) - .5f) * 6f } ?: 0f
    val x by animateFloatAsState(if (reduceMotion) 0f else targetX, spring(dampingRatio = .72f), label = "tiltX")
    val y by animateFloatAsState(if (reduceMotion) 0f else targetY, spring(dampingRatio = .72f), label = "tiltY")
    val palette = AppColors.current
    Box(
        modifier = modifier
            .onSizeChanged { cardSize = androidx.compose.ui.geometry.Size(it.width.toFloat(), it.height.toFloat()) }
            .graphicsLayer { rotationX = x.coerceIn(-3f, 3f); rotationY = y.coerceIn(-3f, 3f); cameraDistance = 18f * density }
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.verticalGradient(listOf(palette.heroStart, palette.heroEnd)))
            .pointerInput(reduceMotion) {
                if (!reduceMotion) {
                    detectDragGestures(
                        onDragStart = { touch = it },
                        onDragEnd = { touch = null },
                        onDragCancel = { touch = null },
                    ) { change, _ -> touch = change.position }
                }
            }
            .fillMaxSize(),
        content = { CompositionLocalProvider(LocalContentColor provides androidx.compose.ui.graphics.Color.White) { content() } },
    )
}
