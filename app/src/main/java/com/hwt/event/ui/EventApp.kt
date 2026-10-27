package com.hwt.event.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.hwt.event.data.EventItem
import com.hwt.event.data.EventStore
import com.hwt.event.data.Occurrence
import com.hwt.event.data.UpdateClient
import com.hwt.event.data.UpdateInfo
import com.hwt.event.ui.theme.EventPalette
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.IOException
import java.io.File
import java.nio.charset.StandardCharsets
import java.text.Collator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EventApp(
    store: EventStore,
    selectedPalette: EventPalette,
    onPaletteChange: (EventPalette) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var events by remember { mutableStateOf(store.getEvents()) }
    var refreshVersion by remember { mutableIntStateOf(0) }
    var selectedEventId by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showCreateEvent by remember { mutableStateOf(false) }
    var editEvent by remember { mutableStateOf<EventItem?>(null) }
    var deleteEvent by remember { mutableStateOf<EventItem?>(null) }
    var updateCandidate by remember { mutableStateOf<UpdateInfo?>(null) }
    var downloadedUpdate by remember { mutableStateOf<File?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf("更新源尚未配置") }

    fun refresh() {
        events = store.getEvents()
        refreshVersion++
    }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun checkForUpdates() {
        if (UpdateClient.MANIFEST_URL.isBlank()) {
            updateStatus = "更新源尚未配置：需要先设置 Gitee 更新清单地址。"
            showMessage(updateStatus)
            return
        }
        scope.launch {
            isCheckingUpdate = true
            updateStatus = "正在检查更新…"
            try {
                val update = withContext(Dispatchers.IO) { UpdateClient.fetchUpdateInfo() }
                val currentVersion = UpdateClient.currentVersionCode(context)
                if (update.versionCode <= currentVersion) {
                    updateStatus = "当前已是最新版（${update.versionName}）。"
                    showMessage(updateStatus)
                } else {
                    updateCandidate = update
                    downloadedUpdate = null
                    updateStatus = "发现新版本 ${update.versionName}。"
                }
            } catch (error: Exception) {
                updateStatus = "检查更新失败：${error.message ?: "请稍后重试"}"
                showMessage(updateStatus)
            } finally {
                isCheckingUpdate = false
            }
        }
    }

    fun downloadUpdate(update: UpdateInfo) {
        scope.launch {
            isDownloadingUpdate = true
            updateStatus = "正在下载 ${update.versionName}…"
            try {
                downloadedUpdate = withContext(Dispatchers.IO) {
                    UpdateClient.downloadAndVerify(context, update)
                }
                updateStatus = "下载和校验完成，可以安装。"
                showMessage("更新包下载完成，已通过完整性校验")
            } catch (error: Exception) {
                updateStatus = "下载失败：${error.message ?: "请稍后重试"}"
                showMessage(updateStatus)
            } finally {
                isDownloadingUpdate = false
            }
        }
    }

    fun installUpdate(apkFile: File) {
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            showMessage("请在系统设置中允许 event 安装应用，然后返回这里再次点击“安装”。")
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            }.onFailure { showMessage("无法打开安装权限设置：${it.message ?: "请手动开启"}") }
            return
        }
        runCatching { context.startActivity(UpdateClient.createInstallIntent(context, apkFile)) }
            .onFailure { showMessage("无法启动系统安装器：${it.message ?: "请重试"}") }
    }

    fun recordOne(eventId: String) {
        val occurrenceId = store.recordOne(eventId)
        refresh()
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "已记录一次",
                actionLabel = "撤销",
                duration = SnackbarDuration.Short
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                store.deleteOccurrence(occurrenceId)
                refresh()
                snackbarHostState.showSnackbar("已撤销这次记录")
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter(StandardCharsets.UTF_8)?.use {
                    it.write(store.exportBackup())
                } ?: throw IOException("无法写入所选文件")
            }.onSuccess { showMessage("备份已保存") }
                .onFailure { showMessage("备份失败：${it.message ?: "请重试"}") }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader(StandardCharsets.UTF_8)?.use {
                    it.readText()
                } ?: throw IOException("无法读取所选文件")
                store.importBackup(json)
            }.onSuccess { counts ->
                refresh()
                showMessage("已合并 ${counts.first} 个事件、${counts.second} 条记录")
            }.onFailure { error ->
                val detail = if (error is JSONException) "备份文件格式有误" else (error.message ?: "请重试")
                showMessage("导入失败：$detail")
            }
        }
    }

    val collator = remember { Collator.getInstance(Locale.CHINA).apply { strength = Collator.PRIMARY } }
    val sortedEvents = remember(events, collator) {
        events.sortedWith { left, right ->
            when {
                left.isPinned != right.isPinned -> if (left.isPinned) -1 else 1
                else -> collator.compare(left.name, right.name)
            }
        }
    }
    val screen = when {
        showSettings -> "settings"
        selectedEventId != null -> "event:$selectedEventId"
        else -> "home"
    }

    BackHandler(enabled = selectedEventId != null || showSettings) {
        when {
            showSettings -> showSettings = false
            else -> selectedEventId = null
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(18.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.inversePrimary
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.navigationBars
    ) { insets ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
        ) {
            AnimatedContent(
                targetState = screen,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 18 }) togetherWith
                        (fadeOut(tween(160)) + slideOutHorizontally(tween(160)) { -it / 18 })
                },
                label = "screen-transition"
            ) { targetScreen ->
                when {
                    targetScreen == "settings" -> SettingsScreen(
                        eventCount = events.size,
                        occurrenceCount = events.sumOf { it.totalCount },
                        selectedPalette = selectedPalette,
                        onPaletteChange = onPaletteChange,
                        onBack = { showSettings = false },
                        onExport = {
                            val filename = "event-backup-${LocalDate.now()}.json"
                            exportLauncher.launch(filename)
                        },
                        onImport = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                        updateStatus = updateStatus,
                        isCheckingUpdate = isCheckingUpdate,
                        isDownloadingUpdate = isDownloadingUpdate,
                        onCheckUpdates = ::checkForUpdates
                    )

                    targetScreen.startsWith("event:") -> {
                        val targetEventId = targetScreen.substringAfter(":")
                        val detailEvent = events.firstOrNull { it.id == targetEventId }
                        if (detailEvent != null) {
                            val detailOccurrences = remember(targetEventId, refreshVersion) {
                                store.getOccurrences(targetEventId)
                            }
                            EventDetailScreen(
                                event = detailEvent,
                                occurrences = detailOccurrences,
                                refreshVersion = refreshVersion,
                                onBack = { selectedEventId = null },
                                onBackfill = { timestamp ->
                                    store.recordOne(detailEvent.id, timestamp)
                                    refresh()
                                    showMessage("补记已保存")
                                },
                                onEditOccurrence = { occurrence, timestamp ->
                                    store.updateOccurrence(occurrence.id, timestamp)
                                    refresh()
                                    showMessage("记录时间已修改")
                                },
                                onDeleteOccurrence = { occurrence ->
                                    store.deleteOccurrence(occurrence.id)
                                    refresh()
                                    showMessage("记录已删除")
                                },
                                onRename = { editEvent = detailEvent },
                                onTogglePin = {
                                    store.setPinned(detailEvent.id, !detailEvent.isPinned)
                                    refresh()
                                },
                                onDeleteEvent = { deleteEvent = detailEvent }
                            )
                        }
                    }

                    else -> HomeScreen(
                        events = sortedEvents,
                        onCreateEvent = { showCreateEvent = true },
                        onOpenEvent = { selectedEventId = it.id },
                        onRecordOne = { recordOne(it.id) },
                        onSettings = { showSettings = true }
                    )
                }
            }
        }
    }

    if (showCreateEvent) {
        EventNameDialog(
            title = "新建事件",
            confirmLabel = "创建",
            onDismiss = { showCreateEvent = false },
            onSave = { name ->
                store.createEvent(name)
                refresh()
                showCreateEvent = false
                showMessage("事件已创建")
            }
        )
    }
    editEvent?.let { event ->
        EventNameDialog(
            title = "修改事件名称",
            confirmLabel = "保存",
            initialName = event.name,
            onDismiss = { editEvent = null },
            onSave = { name ->
                store.renameEvent(event.id, name)
                refresh()
                editEvent = null
                showMessage("名称已更新")
            }
        )
    }
    deleteEvent?.let { event ->
        AlertDialog(
            onDismissRequest = { deleteEvent = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("删除“${event.name}”？") },
            text = { Text("这个事件和它的所有发生记录都会从本机删除。") },
            confirmButton = {
                TextButton(onClick = {
                    store.deleteEvent(event.id)
                    if (selectedEventId == event.id) selectedEventId = null
                    deleteEvent = null
                    refresh()
                    showMessage("事件已删除")
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteEvent = null }) { Text("取消") } }
        )
    }
    updateCandidate?.let { update ->
        AlertDialog(
            onDismissRequest = {
                if (!isDownloadingUpdate) {
                    updateCandidate = null
                    downloadedUpdate = null
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("发现 event 新版本 ${update.versionName}") },
            text = {
                Column {
                    if (update.changelog.isNotBlank()) Text(update.changelog)
                    else Text("可以下载并安装这个版本。")
                    Spacer(Modifier.height(10.dp))
                    Text(updateStatus, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    enabled = !isDownloadingUpdate,
                    onClick = {
                        val apk = downloadedUpdate
                        if (apk == null) downloadUpdate(update) else installUpdate(apk)
                    }
                ) {
                    Text(if (downloadedUpdate == null) "下载更新" else "安装")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDownloadingUpdate,
                    onClick = {
                        updateCandidate = null
                        downloadedUpdate = null
                    }
                ) { Text("稍后") }
            }
        )
    }
}

@Composable
private fun HomeScreen(
    events: List<EventItem>,
    onCreateEvent: () -> Unit,
    onOpenEvent: (EventItem) -> Unit,
    onRecordOne: (EventItem) -> Unit,
    onSettings: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "event",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = onSettings) { Text("设置") }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("事件", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.weight(1f))
                    if (events.isNotEmpty()) {
                        Text("${events.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (events.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 52.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("还没有事件", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "点击右下角 + 新建事件",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(events, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onOpen = { onOpenEvent(event) },
                        onRecordOne = { onRecordOne(event) }
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = onCreateEvent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 18.dp)
                .semantics { contentDescription = "新建事件" },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
        }
    }
}

@Composable
private fun EventCard(
    event: EventItem,
    onOpen: () -> Unit,
    onRecordOne: () -> Unit
) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = event.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            FilledTonalIconButton(
                onClick = onRecordOne,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "${event.name} 记录一次" },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    eventCount: Int,
    occurrenceCount: Int,
    selectedPalette: EventPalette,
    onPaletteChange: (EventPalette) -> Unit,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    updateStatus: String,
    isCheckingUpdate: Boolean,
    isDownloadingUpdate: Boolean,
    onCheckUpdates: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "返回" }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            Text("设置与迁移", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("主题配色", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "选择应用的主色调，切换后立即生效。",
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                EventPalette.entries.forEach { palette ->
                    val isSelected = selectedPalette == palette
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                            .clickable { onPaletteChange(palette) }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(20.dp),
                            shape = CircleShape,
                            color = palette.previewColor
                        ) {}
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = palette.label,
                            modifier = Modifier.weight(1f),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        )
                        RadioButton(selected = isSelected, onClick = { onPaletteChange(palette) })
                    }
                    if (palette != EventPalette.entries.last()) Spacer(Modifier.height(8.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("本机数据", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("$eventCount 个事件 · $occurrenceCount 条记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("记录保存在手机本地。迁移时导出备份文件，再在新设备导入。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                Button(onClick = onExport, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Text("导出备份文件") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Text("导入并合并备份") }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("应用更新", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("${updateStatus}\n更新包会先校验完整性，再交给 Android 系统安装。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCheckUpdates,
                    enabled = !isCheckingUpdate && !isDownloadingUpdate,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(if (isCheckingUpdate) "正在检查…" else if (isDownloadingUpdate) "正在下载…" else "检查更新")
                }
            }
        }
    }
}

@Composable
private fun EventNameDialog(
    title: String,
    confirmLabel: String,
    initialName: String = "",
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember(title, initialName) { mutableStateOf(initialName) }
    val cleanName = name.trim()
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("事件名称") },
                    placeholder = { Text("例如：喝水、运动") },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    supportingText = {
                        Text(
                            "${cleanName.length}/60",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (cleanName.isNotEmpty()) onSave(cleanName) })
                )
                Spacer(Modifier.height(4.dp))
                Text("一次点击只记录一次发生。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(cleanName) },
                enabled = cleanName.isNotEmpty(),
                shape = RoundedCornerShape(16.dp)
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun lastActivityLabel(timestamp: Long?): String {
    if (timestamp == null) return "还没有记录"
    val today = LocalDate.now()
    val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    return when (date) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else -> date.format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA))
    }
}

