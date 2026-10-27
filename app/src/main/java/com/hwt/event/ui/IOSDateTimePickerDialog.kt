package com.hwt.event.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PickerWeekdays = listOf("一", "二", "三", "四", "五", "六", "日")
private val PickerDateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINA)
private val PickerMonthFormatter = DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)

@Composable
internal fun IOSDateTimePickerDialog(
    title: String,
    initialMillis: Long,
    onDismissRequest: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    val initialDateTime = remember(initialMillis, zone) {
        Instant.ofEpochMilli(initialMillis).atZone(zone).toLocalDateTime()
    }
    var selectedDate by remember(initialMillis) { mutableStateOf(initialDateTime.toLocalDate()) }
    var visibleMonth by remember(initialMillis) {
        mutableStateOf(initialDateTime.toLocalDate().withDayOfMonth(1))
    }
    var hourText by remember(initialMillis) {
        mutableStateOf(initialDateTime.hour.toString().padStart(2, '0'))
    }
    var minuteText by remember(initialMillis) {
        mutableStateOf(initialDateTime.minute.toString().padStart(2, '0'))
    }

    val today = LocalDate.now()
    val currentMonth = today.withDayOfMonth(1)
    val parsedHour = hourText.toIntOrNull()?.takeIf { it in 0..23 }
    val parsedMinute = minuteText.toIntOrNull()?.takeIf { it in 0..59 }
    val candidateMillis = if (parsedHour != null && parsedMinute != null) {
        selectedDate.atTime(parsedHour, parsedMinute).atZone(zone).toInstant().toEpochMilli()
    } else {
        null
    }
    val isFutureTime = candidateMillis != null && candidateMillis > System.currentTimeMillis()
    val selectedDateLabel = remember(selectedDate) { selectedDate.format(PickerDateFormatter) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .widthIn(max = 440.dp)
                .heightIn(max = 760.dp),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 16.dp)
                        .width(38.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "选择发生日期和时间",
                    modifier = Modifier.padding(top = 3.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(Modifier.padding(horizontal = 15.dp, vertical = 12.dp)) {
                        Text(
                            "发生日期",
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            selectedDateLabel,
                            modifier = Modifier.padding(top = 2.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { visibleMonth = visibleMonth.minusMonths(1) },
                        modifier = Modifier.semantics { contentDescription = "上个月" }
                    ) {
                        Text("‹", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        text = visibleMonth.format(PickerMonthFormatter),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = {
                            selectedDate = today
                            visibleMonth = currentMonth
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("今天")
                    }
                    IconButton(
                        onClick = { visibleMonth = visibleMonth.plusMonths(1) },
                        enabled = visibleMonth < currentMonth,
                        modifier = Modifier.semantics { contentDescription = "下个月" }
                    ) {
                        Text(
                            "›",
                            fontSize = 28.sp,
                            color = if (visibleMonth < currentMonth) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    PickerWeekdays.forEach { weekday ->
                        Text(
                            weekday,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                AnimatedContent(
                    targetState = visibleMonth,
                    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
                    label = "picker-month-transition"
                ) { month ->
                    PickerCalendarGrid(
                        month = month,
                        selectedDate = selectedDate,
                        today = today,
                        onSelectDate = { selectedDate = it }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("发生时间", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("24 小时制", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    TimeNumberField(
                        value = hourText,
                        placeholder = "时",
                        accessibilityLabel = "小时",
                        onValueChange = { raw ->
                            val digits = raw.filter(Char::isDigit).take(2)
                            val number = digits.toIntOrNull()
                            if (digits.isEmpty() || number == null || number in 0..23) hourText = digits
                        }
                    )
                    Text(
                        ":",
                        modifier = Modifier.padding(horizontal = 7.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium
                    )
                    TimeNumberField(
                        value = minuteText,
                        placeholder = "分",
                        accessibilityLabel = "分钟",
                        onValueChange = { raw ->
                            val digits = raw.filter(Char::isDigit).take(2)
                            val number = digits.toIntOrNull()
                            if (digits.isEmpty() || number == null || number in 0..59) minuteText = digits
                        }
                    )
                }
                if (isFutureTime) {
                    Text(
                        "不能选择未来时间",
                        modifier = Modifier.padding(top = 6.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = { candidateMillis?.let(onConfirm) },
                        enabled = candidateMillis != null && !isFutureTime,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerCalendarGrid(
    month: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
    onSelectDate: (LocalDate) -> Unit
) {
    val firstDayOffset = month.dayOfWeek.value - 1
    val dayCount = month.lengthOfMonth()
    val rowCount = (firstDayOffset + dayCount + 6) / 7

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(rowCount) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(7) { columnIndex ->
                    val day = rowIndex * 7 + columnIndex - firstDayOffset + 1
                    if (day in 1..dayCount) {
                        val date = month.withDayOfMonth(day)
                        val isSelected = date == selectedDate
                        val isToday = date == today
                        val isEnabled = !date.isAfter(today)
                        val shape = CircleShape
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .padding(1.dp)
                                .clip(shape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                                .then(
                                    if (isToday && !isSelected) {
                                        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, shape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable(enabled = isEnabled) { onSelectDate(date) }
                                .semantics { contentDescription = date.format(PickerDateFormatter) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    } else {
                        Spacer(Modifier.weight(1f).height(42.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeNumberField(
    value: String,
    placeholder: String,
    accessibilityLabel: String,
    onValueChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 58.dp, height = 54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics { contentDescription = accessibilityLabel },
        contentAlignment = Alignment.Center
    ) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                style = MaterialTheme.typography.titleMedium
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 23.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        )
    }
}
