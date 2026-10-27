package com.hwt.event.ui

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwt.event.data.EventItem
import com.hwt.event.data.Occurrence
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EventDetailScreen(
    event: EventItem,
    occurrences: List<Occurrence>,
    refreshVersion: Int,
    onBack: () -> Unit,
    onBackfill: (Long) -> Unit,
    onEditOccurrence: (Occurrence, Long) -> Unit,
    onDeleteOccurrence: (Occurrence) -> Unit,
    onRename: () -> Unit,
    onTogglePin: () -> Unit,
    onDeleteEvent: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember(event.id) { mutableStateOf(false) }
    var heatmapPeriod by remember(event.id) { mutableStateOf(HeatmapPeriod.MONTH) }
    var anchorDate by remember(event.id) { mutableStateOf(LocalDate.now()) }
    var selectedDate by remember(event.id) { mutableStateOf<LocalDate?>(null) }
    var pendingDeleteOccurrence by remember { mutableStateOf<Occurrence?>(null) }
    var dateTimePickerRequest by remember { mutableStateOf<DateTimePickerRequest?>(null) }

    val periodStart = remember(anchorDate, heatmapPeriod) {
        when (heatmapPeriod) {
            HeatmapPeriod.WEEK -> mondayOf(anchorDate)
            HeatmapPeriod.MONTH -> anchorDate.withDayOfMonth(1)
            HeatmapPeriod.YEAR -> anchorDate.withDayOfYear(1)
        }
    }
    val periodEnd = remember(periodStart, heatmapPeriod) {
        when (heatmapPeriod) {
            HeatmapPeriod.WEEK -> periodStart.plusWeeks(1)
            HeatmapPeriod.MONTH -> periodStart.plusMonths(1)
            HeatmapPeriod.YEAR -> periodStart.plusYears(1)
        }
    }
    val periodOccurrences = remember(occurrences, periodStart, periodEnd, refreshVersion) {
        occurrences.filter { occurrence ->
            val date = runCatching { LocalDate.parse(occurrence.localDate) }.getOrNull()
            date != null && !date.isBefore(periodStart) && date.isBefore(periodEnd)
        }
    }
    val dayCounts = remember(periodOccurrences) {
        periodOccurrences.groupingBy { it.localDate }.eachCount()
    }
    val periodLabel = remember(periodStart, heatmapPeriod) {
        when (heatmapPeriod) {
            HeatmapPeriod.WEEK -> {
                val weekEnd = periodStart.plusDays(6)
                if (periodStart.year == weekEnd.year) {
                    "${periodStart.monthValue}月${periodStart.dayOfMonth}日–${weekEnd.monthValue}月${weekEnd.dayOfMonth}日"
                } else {
                    "${periodStart.year}年${periodStart.monthValue}月${periodStart.dayOfMonth}日–${weekEnd.year}年${weekEnd.monthValue}月${weekEnd.dayOfMonth}日"
                }
            }
            HeatmapPeriod.MONTH -> "${periodStart.year}年${periodStart.monthValue}月"
            HeatmapPeriod.YEAR -> "${periodStart.year}年"
        }
    }
    val currentPeriodStart = remember(heatmapPeriod) {
        val today = LocalDate.now()
        when (heatmapPeriod) {
            HeatmapPeriod.WEEK -> mondayOf(today)
            HeatmapPeriod.MONTH -> today.withDayOfMonth(1)
            HeatmapPeriod.YEAR -> today.withDayOfYear(1)
        }
    }
    fun movePeriod(direction: Long) {
        anchorDate = when (heatmapPeriod) {
            HeatmapPeriod.WEEK -> anchorDate.plusWeeks(direction)
            HeatmapPeriod.MONTH -> anchorDate.plusMonths(direction)
            HeatmapPeriod.YEAR -> anchorDate.plusYears(direction)
        }
        selectedDate = null
    }
    val visibleHistory = remember(occurrences, selectedDate) {
        selectedDate?.let { selected -> occurrences.filter { it.localDate == selected.toString() } } ?: occurrences
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(58.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "返回" }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            Text(
                text = event.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.semantics { contentDescription = "事件设置" }
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(if (event.isPinned) "取消置顶" else "置顶") },
                        onClick = { menuExpanded = false; onTogglePin() }
                    )
                    DropdownMenuItem(
                        text = { Text("修改名称") },
                        onClick = { menuExpanded = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text("删除事件", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDeleteEvent() }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().animateContentSize(tween(220)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(start = 12.dp, top = 14.dp, end = 12.dp, bottom = 14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            HeatmapPeriod.entries.forEach { option ->
                                val isSelected = heatmapPeriod == option
                                val chipColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                    animationSpec = tween(180),
                                    label = "period-chip-background"
                                )
                                val labelColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    animationSpec = tween(180),
                                    label = "period-chip-label"
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(chipColor)
                                        .selectable(
                                            selected = isSelected,
                                            role = Role.Tab,
                                            onClick = {
                                                if (!isSelected) {
                                                    heatmapPeriod = option
                                                    selectedDate = null
                                                }
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option.label,
                                        color = labelColor,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { movePeriod(-1) },
                                modifier = Modifier.semantics {
                                    contentDescription = when (heatmapPeriod) {
                                        HeatmapPeriod.WEEK -> "上一周"
                                        HeatmapPeriod.MONTH -> "上个月"
                                        HeatmapPeriod.YEAR -> "上一年"
                                    }
                                }
                            ) {
                                Text("‹", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(periodLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("${periodOccurrences.size} 次", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(
                                onClick = { movePeriod(1) },
                                enabled = periodStart < currentPeriodStart,
                                modifier = Modifier.semantics {
                                    contentDescription = when (heatmapPeriod) {
                                        HeatmapPeriod.WEEK -> "下一周"
                                        HeatmapPeriod.MONTH -> "下个月"
                                        HeatmapPeriod.YEAR -> "下一年"
                                    }
                                }
                            ) {
                                Text(
                                    "›",
                                    fontSize = 28.sp,
                                    color = if (periodStart < currentPeriodStart) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        ContributionHeatmap(
                            period = heatmapPeriod,
                            anchorDate = anchorDate,
                            dayCounts = dayCounts,
                            selectedDate = selectedDate,
                            onDayClick = { selectedDate = it }
                        )
                        AnimatedVisibility(
                            visible = selectedDate != null,
                            enter = fadeIn(tween(180)) + expandVertically(tween(180)),
                            exit = fadeOut(tween(140)) + shrinkVertically(tween(140))
                        ) {
                            selectedDate?.let { date ->
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.animateContentSize(tween(180))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${date.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA))} · ${dayCounts[date.toString()] ?: 0} 次",
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    TextButton(onClick = { selectedDate = null }) {
                                        Text("看全部", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            }
                            }
                        }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        dateTimePickerRequest = DateTimePickerRequest(
                            initialMillis = System.currentTimeMillis(),
                            title = "补记发生时间",
                            onConfirm = onBackfill
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("补记")
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(if (selectedDate == null) "发生记录" else "当天记录", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = if (selectedDate == null) "${occurrences.size} 条 · 按时间倒序" else "${visibleHistory.size} 条",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TextButton(
                        onClick = {
                            if (selectedDate == null) {
                                showDatePicker(context, LocalDate.now()) { selectedDate = it }
                            } else {
                                selectedDate = null
                            }
                        },
                        modifier = Modifier.semantics {
                            contentDescription = if (selectedDate == null) "按日期筛选记录" else "清除日期筛选"
                        }
                    ) {
                        Text(if (selectedDate == null) "选日期" else "清除筛选")
                    }
                }
            }
            if (visibleHistory.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = if (occurrences.isEmpty()) "还没有记录。可以在图表下方点“补记”添加发生时间。" else "这一天没有记录。",
                            modifier = Modifier.padding(18.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(visibleHistory, key = { it.id }) { occurrence ->
                    OccurrenceRow(
                        occurrence = occurrence,
                        onEdit = {
                            dateTimePickerRequest = DateTimePickerRequest(
                                initialMillis = occurrence.occurredAt,
                                title = "修改发生时间",
                                onConfirm = { timestamp -> onEditOccurrence(occurrence, timestamp) }
                            )
                        },
                        onDelete = { pendingDeleteOccurrence = occurrence }
                    )
                }
            }
        }
    }

    dateTimePickerRequest?.let { request ->
        IOSDateTimePickerDialog(
            title = request.title,
            initialMillis = request.initialMillis,
            onDismissRequest = { dateTimePickerRequest = null },
            onConfirm = { timestamp ->
                dateTimePickerRequest = null
                request.onConfirm(timestamp)
            }
        )
    }

    pendingDeleteOccurrence?.let { occurrence ->
        AlertDialog(
            onDismissRequest = { pendingDeleteOccurrence = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("删除这条记录？") },
            text = { Text("删除后不会计入事件总数和热力图。") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteOccurrence(occurrence)
                    pendingDeleteOccurrence = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteOccurrence = null }) { Text("取消") } }
        )
    }
}

private data class DateTimePickerRequest(
    val initialMillis: Long,
    val title: String,
    val onConfirm: (Long) -> Unit
)

@Composable
private fun OccurrenceRow(occurrence: Occurrence, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuExpanded by remember(occurrence.id) { mutableStateOf(false) }
    val formatted = remember(occurrence.occurredAt) {
        Instant.ofEpochMilli(occurrence.occurredAt)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy年M月d日  HH:mm:ss", Locale.CHINA))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 15.dp, top = 10.dp, end = 7.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("+1", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(formatted, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("记录一次", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.semantics { contentDescription = "记录操作" }
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("修改时间") }, onClick = { menuExpanded = false; onEdit() })
                    DropdownMenuItem(
                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}

private fun showDatePicker(
    context: Context,
    initialDate: LocalDate,
    onSelected: (LocalDate) -> Unit
) {
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth -> onSelected(LocalDate.of(year, month + 1, dayOfMonth)) },
        initialDate.year,
        initialDate.monthValue - 1,
        initialDate.dayOfMonth
    ).show()
}
