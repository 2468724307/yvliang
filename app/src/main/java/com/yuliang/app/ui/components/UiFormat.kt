package com.yuliang.app.ui.components

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun Long.money(): String = "¥" + BigDecimal.valueOf(this, 2).setScale(2).toPlainString()
fun Long.yuanInput(): String = BigDecimal.valueOf(this, 2).stripTrailingZeros().toPlainString()

fun yuanToCentsOrNull(text: String): Long? = try {
    BigDecimal(text.trim()).setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact().takeIf { it > 0 }
} catch (_: Exception) { null }

fun Instant.displayDate(zoneId: ZoneId = ZoneId.systemDefault()): String =
    atZone(zoneId).format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
