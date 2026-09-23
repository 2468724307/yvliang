package com.yuliang.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.yuliang.app.ui.theme.AppColors
import com.yuliang.app.ui.theme.MotionTokens
import com.yuliang.app.ui.theme.YuliangShapes
import java.math.BigDecimal

@Composable
fun YuliangBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(visible) { onDismiss() }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.fillMaxSize(),
        enter = fadeIn(tween(if (reduceMotion) 0 else MotionTokens.Medium)),
        exit = fadeOut(tween(if (reduceMotion) 0 else MotionTokens.Fast)),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .38f)).clickable { onDismiss() })
            var dragged by remember { mutableFloatStateOf(0f) }
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().wrapContentHeight()
                    .imePadding().navigationBarsPadding()
                    .graphicsLayer { translationY = dragged }
                    .pointerInput(visible) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, delta -> dragged = (dragged + delta).coerceAtLeast(0f) },
                            onDragEnd = { if (dragged > 100.dp.toPx()) onDismiss(); dragged = 0f },
                            onDragCancel = { dragged = 0f },
                        )
                    }
                    .padding(PaddingValues(horizontal = 20.dp, vertical = 12.dp)),
                content = content,
            )
        }
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
    val haptic = LocalHapticFeedback.current
    val palette = AppColors.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) MotionTokens.PressedScale else 1f,
        animationSpec = if (reduceMotion) snap() else tween(MotionTokens.Fast),
        label = "buttonPress",
    )
    Button(
        onClick = { if (!reduceMotion) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() },
        enabled = enabled,
        interactionSource = interaction,
        shape = YuliangShapes.card,
        colors = ButtonDefaults.buttonColors(containerColor = if (pressed) palette.brandPressed else MaterialTheme.colorScheme.primary),
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        content = content,
    )
}

@Composable
fun RollingMoney(cents: Long, modifier: Modifier = Modifier, reduceMotion: Boolean = false) {
    if (reduceMotion) {
        AmountText(cents, modifier, large = true, color = LocalContentColor.current)
    } else Crossfade(targetState = cents, modifier = modifier, animationSpec = tween(MotionTokens.Medium), label = "moneyChange") { value ->
        AmountText(value, large = true, color = LocalContentColor.current)
    }
}

@Composable
fun TiltBudgetHero(
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var touch by remember { mutableStateOf<Offset?>(null) }
    var cardSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    val targetX = touch?.let { ((it.y / cardSize.height.coerceAtLeast(1f)) - .5f) * -3f } ?: 0f
    val targetY = touch?.let { ((it.x / cardSize.width.coerceAtLeast(1f)) - .5f) * 3f } ?: 0f
    val x by animateFloatAsState(if (reduceMotion) 0f else targetX, spring(dampingRatio = .72f), label = "tiltX")
    val y by animateFloatAsState(if (reduceMotion) 0f else targetY, spring(dampingRatio = .72f), label = "tiltY")
    val palette = AppColors.current
    Box(
        modifier = modifier
            .onSizeChanged { cardSize = androidx.compose.ui.geometry.Size(it.width.toFloat(), it.height.toFloat()) }
            .graphicsLayer { rotationX = x.coerceIn(-1.5f, 1.5f); rotationY = y.coerceIn(-1.5f, 1.5f); cameraDistance = 18f * density }
            .clip(YuliangShapes.hero)
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
            .fillMaxWidth().heightIn(min = 200.dp),
        content = { CompositionLocalProvider(LocalContentColor provides androidx.compose.ui.graphics.Color.White) { content() } },
    )
}
