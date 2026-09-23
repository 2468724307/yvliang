package com.yuliang.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.TextUnit
import com.yuliang.app.ui.theme.Spacing
import com.yuliang.app.ui.theme.YuliangElevation
import com.yuliang.app.ui.theme.YuliangShapes
import com.yuliang.app.ui.theme.YuliangSizes
import com.yuliang.app.ui.theme.YuliangTypography
import com.yuliang.app.ui.theme.appTextFieldColors

@Composable
fun PrimaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    reduceMotion: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) = PressableButton(onClick, modifier.heightIn(min = YuliangSizes.primaryAction), enabled, reduceMotion, content)

@Composable
fun SecondaryActionButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) =
    OutlinedButton(onClick, modifier.heightIn(min = YuliangSizes.touchTarget), enabled = enabled, shape = YuliangShapes.medium, content = content)

@Composable
fun DestructiveActionButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) =
    Button(onClick, modifier.heightIn(min = YuliangSizes.touchTarget), enabled = enabled, shape = YuliangShapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError), content = content)

@Composable
fun QuietActionButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) =
    TextButton(onClick, modifier.heightIn(min = YuliangSizes.touchTarget), content = content)

@Composable
fun DataCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier, shape = YuliangShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(Spacing.tiny / 4, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = YuliangElevation.flat)) {
        Column(Modifier.padding(Spacing.medium), verticalArrangement = Arrangement.spacedBy(Spacing.small), content = content)
    }
}

@Composable
fun AmountText(cents: Long, modifier: Modifier = Modifier, large: Boolean = false, color: Color = Color.Unspecified) {
    val style = if (large) YuliangTypography.displayAmount else YuliangTypography.amountMedium
    val text = cents.money()
    Text(buildAnnotatedString {
        withStyle(SpanStyle(fontSize = style.fontSize * .65f)) { append("¥") }
        append(text.removePrefix("¥"))
    }, modifier = modifier, style = style, color = color, maxLines = 1, softWrap = false)
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = YuliangTypography.titleMedium)
        if (action != null && onAction != null) QuietActionButton(onAction) { Text(action) }
    }
}

@Composable
fun AppTextInput(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, enabled: Boolean = true, singleLine: Boolean = true) =
    OutlinedTextField(value, onValueChange, modifier = modifier, label = { Text(label) }, enabled = enabled, singleLine = singleLine,
        shape = YuliangShapes.medium, colors = appTextFieldColors())

@Composable
fun AmountInput(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, prominent: Boolean = false, enabled: Boolean = true) =
    OutlinedTextField(value, { onValueChange(it.filter { c -> c.isDigit() || c == '.' }) }, modifier = modifier,
        label = { Text(label) }, prefix = { Text("¥", style = if (prominent) YuliangTypography.amountMedium else YuliangTypography.bodyLarge) },
        textStyle = if (prominent) YuliangTypography.amountLarge else YuliangTypography.bodyLarge,
        singleLine = true, enabled = enabled, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = YuliangShapes.medium, colors = appTextFieldColors())
