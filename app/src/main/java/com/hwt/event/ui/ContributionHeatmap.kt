package com.hwt.event.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal enum class HeatmapPeriod(val label: String) {
    WEEK("周"),
    MONTH("月"),
    YEAR("年")
}

private val LightHeatmapLevels = listOf(
    Color(0xFFEBEDF0),
    Color(0xFFACE4BC),
    Color(0xFF65C987),
    Color(0xFF31A354),
    Color(0xFF176D3A)
)

private val DarkHeatmapLevels = listOf(
    Color(0xFF26322B),
    Color(0xFF9BD6A9),
    Color(0xFF5CBF79),
    Color(0xFF36A05B),
    Color(0xFF1D7040)
)

private val WeekdayLabels = listOf("Mon", "", "Wed", "", "Fri", "", "")
private val WeekdayNamesChinese = listOf("一", "二", "三", "四", "五", "六", "日")
private val WeekdayNamesForWeekView = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
private val CellSize = 5.dp
private val CellGap = 1.dp
private val WeekdayLabelWidth = 24.dp
private val SmallCellShape = RoundedCornerShape(1.5.dp)

@Composable
internal fun ContributionHeatmap(
    period: HeatmapPeriod,
    anchorDate: LocalDate,
    dayCounts: Map<String, Int>,
    selectedDate: LocalDate?,
    onDayClick: (LocalDate) -> Unit
) {
    val palette = if (isSystemInDarkTheme()) DarkHeatmapLevels else LightHeatmapLevels
    when (period) {
        HeatmapPeriod.YEAR -> YearHeatmap(
            year = anchorDate.year,
            dayCounts = dayCounts,
            selectedDate = selectedDate,
            palette = palette,
            onDayClick = onDayClick
        )

        HeatmapPeriod.MONTH -> MonthHeatmap(
            monthStart = anchorDate.withDayOfMonth(1),
            dayCounts = dayCounts,
            selectedDate = selectedDate,
            palette = palette,
            onDayClick = onDayClick
        )

        HeatmapPeriod.WEEK -> WeekHeatmap(
            weekStart = mondayOf(anchorDate),
            dayCounts = dayCounts,
            selectedDate = selectedDate,
            palette = palette,
            onDayClick = onDayClick
        )
    }
}

@Composable
private fun YearHeatmap(
    year: Int,
    dayCounts: Map<String, Int>,
    selectedDate: LocalDate?,
    palette: List<Color>,
    onDayClick: (LocalDate) -> Unit
) {
    val firstDay = remember(year) { LocalDate.of(year, 1, 1) }
    val dayOffset = firstDay.dayOfWeek.value - 1
    val dayCount = firstDay.lengthOfYear()
    val weekCount = (dayOffset + dayCount + 6) / 7
    val monthLabels = remember(year, dayOffset) {
        (1..12).associate { month ->
            val monthStart = LocalDate.of(year, month, 1)
            ((dayOffset + monthStart.dayOfYear - 1) / 7) to "${month}月"
        }
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Spacer(Modifier.width(WeekdayLabelWidth))
            Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                repeat(weekCount) { week ->
                    Box(
                        modifier = Modifier.width(CellSize).height(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        monthLabels[week]?.let { month ->
                            Text(
                                text = month,
                                modifier = Modifier.requiredWidth(25.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 8.sp,
                                lineHeight = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.width(WeekdayLabelWidth), verticalArrangement = Arrangement.spacedBy(CellGap)) {
                repeat(7) { weekday ->
                    Box(
                        Modifier.size(width = WeekdayLabelWidth, height = CellSize),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        WeekdayLabels[weekday].takeIf(String::isNotEmpty)?.let { label ->
                            Text(
                                text = label,
                                modifier = Modifier.requiredWidth(22.dp).requiredHeight(9.dp).offset(y = (-2).dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 7.sp,
                                lineHeight = 9.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                repeat(weekCount) { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
                        repeat(7) { weekday ->
                            val dayIndex = week * 7 + weekday - dayOffset
                            if (dayIndex in 0 until dayCount) {
                                val date = firstDay.plusDays(dayIndex.toLong())
                                HeatmapDayCell(
                                    count = dayCounts[date.toString()] ?: 0,
                                    label = null,
                                    description = "${date.format(dateFormatter)}，${dayCounts[date.toString()] ?: 0} 次",
                                    isSelected = date == selectedDate,
                                    palette = palette,
                                    onClick = { onDayClick(date) },
                                    modifier = Modifier.size(CellSize),
                                    smallCell = true
                                )
                            } else {
                                Spacer(Modifier.size(CellSize))
                            }
                        }
                    }
                }
            }
        }
        HeatmapLegend(palette)
    }
}

@Composable
private fun MonthHeatmap(
    monthStart: LocalDate,
    dayCounts: Map<String, Int>,
    selectedDate: LocalDate?,
    palette: List<Color>,
    onDayClick: (LocalDate) -> Unit
) {
    val dayCount = monthStart.lengthOfMonth()
    val leadingDays = monthStart.dayOfWeek.value - 1
    val weekCount = (leadingDays + dayCount + 6) / 7
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA) }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            WeekdayNamesChinese.forEach { weekday ->
                Text(
                    text = weekday,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        repeat(weekCount) { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { weekday ->
                    val day = week * 7 + weekday - leadingDays + 1
                    if (day in 1..dayCount) {
                        val date = monthStart.withDayOfMonth(day)
                        val count = dayCounts[date.toString()] ?: 0
                        HeatmapDayCell(
                            count = count,
                            label = day.toString(),
                            countLabel = if (count > 0) "${count}次" else null,
                            description = "${date.format(dateFormatter)}，$count 次",
                            isSelected = date == selectedDate,
                            palette = palette,
                            onClick = { onDayClick(date) },
                            modifier = Modifier.weight(1f).aspectRatio(1f),
                            smallCell = false
                        )
                    } else {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
        HeatmapLegend(palette)
    }
}

@Composable
private fun WeekHeatmap(
    weekStart: LocalDate,
    dayCounts: Map<String, Int>,
    selectedDate: LocalDate?,
    palette: List<Color>,
    onDayClick: (LocalDate) -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        (0..6).forEach { index ->
            val date = weekStart.plusDays(index.toLong())
            val count = dayCounts[date.toString()] ?: 0
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    WeekdayNamesForWeekView[index],
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    maxLines = 1
                )
                HeatmapDayCell(
                    count = count,
                    label = date.dayOfMonth.toString(),
                    description = "${date.format(dateFormatter)}，$count 次",
                    isSelected = date == selectedDate,
                    palette = palette,
                    onClick = { onDayClick(date) },
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    smallCell = false
                )
                Text(
                    "$count 次",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    HeatmapLegend(palette)
}

@Composable
private fun HeatmapDayCell(
    count: Int,
    label: String?,
    countLabel: String? = null,
    description: String,
    isSelected: Boolean,
    palette: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier,
    smallCell: Boolean
) {
    val level = count.coerceIn(0, palette.lastIndex)
    val targetColor = palette[level]
    val backgroundColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 200),
        label = "heatmap-cell-color"
    )
    val textColor = if (backgroundColor.luminance() > 0.46f) {
        Color(0xFF173C29)
    } else {
        Color.White
    }
    val shape = if (smallCell) SmallCellShape else RoundedCornerShape(8.dp)
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        count == 0 && isSystemInDarkTheme() -> Color(0xFF3D4A42)
        count == 0 -> Color(0xFFD8DEE4)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(if (isSelected) 2.dp else if (smallCell) 0.5.dp else 0.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        if (label != null) {
            val labelColor = if (count == 0) MaterialTheme.colorScheme.onSurfaceVariant else textColor
            if (countLabel == null) {
                Text(
                    text = label,
                    color = labelColor,
                    fontSize = if (smallCell) 7.sp else 11.sp,
                    lineHeight = if (smallCell) 8.sp else 14.sp,
                    fontWeight = if (count > 0) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = label,
                        color = labelColor,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = countLabel,
                        color = labelColor,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapLegend(palette: List<Color>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text("少", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        Spacer(Modifier.width(5.dp))
        palette.forEachIndexed { index, color ->
            Spacer(
                Modifier
                    .padding(end = 3.dp)
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
                    .then(
                        if (index == 0) Modifier.border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))
                        else Modifier
                    )
            )
        }
        Text("多", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
    }
}

internal fun mondayOf(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - 1).toLong())
