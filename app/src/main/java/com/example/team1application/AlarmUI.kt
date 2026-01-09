package com.example.team1application

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // itemsを使うために必要
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings // これが必要です
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // アラームデータのロード
    val allAlarms = remember {
        AlarmDataStore.loadAlarms(context).toMutableStateList()
    }

    // --- 各種状態 ---
    var showOnlyActive by remember { mutableStateOf(false) }
    var showWakeUp by remember { mutableStateOf(true) }
    var showBedtime by remember { mutableStateOf(true) }
    var menuExpanded by remember { mutableStateOf(false) }

    var showSetupDialog by remember { mutableStateOf(false) }
    var timeForNewAlarm by remember { mutableStateOf<String?>(null) }
    var editingAlarmId by remember { mutableStateOf<Int?>(null) }

    // 歯車ボタンの状態
    var showExtraSettings by remember { mutableStateOf(false) }

    val editingAlarm = allAlarms.find { it.id == editingAlarmId }

    // --- A. 詳細設定画面への遷移 ---
    if (timeForNewAlarm != null || editingAlarm != null) {
        val isNew = editingAlarm == null
        val initialTime = timeForNewAlarm ?: editingAlarm?.time ?: "07:00"

        AlarmSetUI(
            initialTime = initialTime,
            isNewAlarm = isNew,
            existingAlarm = editingAlarm,
            onDismiss = {
                timeForNewAlarm = null
                editingAlarmId = null
            },
            onSave = { updatedSetting ->
                alarmSave(context, allAlarms, updatedSetting)
                timeForNewAlarm = null
                editingAlarmId = null
            },
            onDelete = {
                editingAlarm?.let { deleteAlarmAndSave(context, allAlarms, it.id) }
                editingAlarmId = null
            }
        )
        return
    }

    // --- B. 時刻選択ダイアログ ---
    if (showSetupDialog) {
        AlarmSetupDialog(
            onDismiss = { showSetupDialog = false },
            onSave = { selectedTime ->
                showSetupDialog = false
                timeForNewAlarm = selectedTime
            }
        )
    }

    // --- C. メインリスト画面 ---
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("アラーム一覧") },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "メニュー")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("アクティブのみ表示") },
                                onClick = { showOnlyActive = !showOnlyActive },
                                leadingIcon = { if (showOnlyActive) Icon(Icons.Default.Check, null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("起床アラームを表示") },
                                onClick = { showWakeUp = !showWakeUp },
                                leadingIcon = { if (showWakeUp) Icon(Icons.Default.Check, null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("就寝アラームを表示") },
                                onClick = { showBedtime = !showBedtime },
                                leadingIcon = { if (showBedtime) Icon(Icons.Default.Check, null) }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = modifier.padding(innerPadding).fillMaxSize().padding(16.dp)) {

            // フィルタリング処理
            val filteredAlarms = allAlarms.filter { alarm ->
                val activeFilter = if (showOnlyActive) alarm.isActive else true
                val typeFilter = when (alarm.type) {
                    AlarmType.WAKE_UP -> showWakeUp
                    AlarmType.BEDTIME -> showBedtime
                }
                activeFilter && typeFilter
            }

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items = filteredAlarms, key = { it.id }) { alarm ->
                    AlarmSettingCard(
                        alarm = alarm,
                        onToggleActive = { id, active ->
                            val index = allAlarms.indexOfFirst { it.id == id }
                            if (index != -1) {
                                allAlarms[index] = allAlarms[index].copy(isActive = active)
                                AlarmDataStore.saveAlarms(context, allAlarms)
                            }
                        },
                        onDeleteAlarm = { id -> deleteAlarmAndSave(context, allAlarms, id) },
                        onEditAlarm = { id -> editingAlarmId = id }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 下部操作エリア ---
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                if (showExtraSettings) {
                    Button(
                        onClick = { /* 監視設定へ */ },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text("📱 監視対象アプリの設定")
                    }
                    Button(
                        onClick = { /* 通知設定へ */ },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text("🔔 就寝前通知の設定")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showSetupDialog = true },
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("➕ 新しいアラームを設定")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledIconButton(
                        onClick = { showExtraSettings = !showExtraSettings },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "設定"
                        )
                    }
                }
            }
        }
    }
}

//  AlarmSettingCard 関数を AlarmScreen の外に記述 ---

@Composable
fun AlarmSettingCard(
    alarm: AlarmSetting,
    onToggleActive: (Int, Boolean) -> Unit,
    onDeleteAlarm: (Int) -> Unit,
    onEditAlarm: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 💡 アラームの種類に応じた色とラベルを定義
    val (cardColors, typeLabel) = when (alarm.type) {
        AlarmType.WAKE_UP -> Pair(
            CardDefaults.cardColors(
                // 暖色系（薄いオレンジ/イエロー）
                containerColor = Color(0xFFFFF1CC),
                contentColor = Color(0xFF452B00)
            ),
            "☀️ 起床アラーム"
        )
        AlarmType.BEDTIME -> Pair(
            CardDefaults.cardColors(
                // 寒色系（薄いパープル/ブルー）
                containerColor = Color(0xFFEADDFF),
                contentColor = Color(0xFF21005D)
            ),
            "🌙 就寝アラーム"
        )
    }

    Card(
        modifier = modifier,
        colors = cardColors
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEditAlarm(alarm.id) }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {

                // 💡 改善点1: アラーム名がある場合のみ、一番上に表示する
                if (alarm.name.isNotBlank()) {
                    Text(
                        text = alarm.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cardColors.contentColor
                    )
                }

                // 💡 改善点2: 「起床/就寝」ラベルをアラーム名とは別の行に表示する
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = cardColors.contentColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // 時刻
                Text(
                    text = alarm.time,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )

                // 曜日
                if (alarm.days.isNotBlank()) {
                    Text(
                        text = alarm.days,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cardColors.contentColor.copy(alpha = 0.7f)
                    )
                }
            }

            // 右側の操作（削除・スイッチ）
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onDeleteAlarm(alarm.id) }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "削除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = alarm.isActive,
                    onCheckedChange = { onToggleActive(alarm.id, it) }
                )
            }
        }
    }
}