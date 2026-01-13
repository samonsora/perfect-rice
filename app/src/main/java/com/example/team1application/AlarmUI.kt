package com.example.team1application

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

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

    // アプリ選択ダイアログの表示状態
    var showAppSelector by remember { mutableStateOf(false) }

    val editingAlarm = allAlarms.find { it.id == editingAlarmId }

    var showNotificationIntervalSelector by remember { mutableStateOf(false) }

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

    // --- D. 監視アプリ選択ダイアログ ---
    if (showAppSelector) {
        AppSelectorDialog(
            onDismiss = { showAppSelector = false },
            onSave = { selectedPackages ->
                TargetAppDataStore.saveTargetApps(context, selectedPackages)
                showAppSelector = false
            }
        )
    }

    if (showNotificationIntervalSelector) {
        NotificationTimeSelectorDialog(
            onDismiss = { showNotificationIntervalSelector = false },
            onSave = { selectedMinutes ->
                UserPreferencesStore.saveCheckMinutes(context, selectedMinutes)
                showNotificationIntervalSelector = false
                // 必要であればここでWorkManagerを再スケジュールする処理を呼ぶ
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
                        onClick = { showAppSelector = true }, // 修正：ダイアログを表示
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text("📱 監視対象アプリの設定")
                    }
                    Button(
                        onClick = { showNotificationIntervalSelector = true },
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

/**
 * 監視アプリ選択ダイアログ
 */
@Composable
fun AppSelectorDialog(onDismiss: () -> Unit, onSave: (Set<String>) -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager

    // 取得ロジックの改善
    val installedApps = remember {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }

        // 除外リスト（これら以外は YouTube や Chrome を含めすべて表示されます）
        val excludePackages = setOf(
            "com.android.settings",    // 設定
            "com.android.vending",     // Playストア
            "com.android.stk",         // SIMツールキット
            "com.android.contacts",    // 連絡先
            "com.android.deskclock",   // 時計
            "com.android.calculator",  // 電卓
            context.packageName        // このアプリ自身
        )

        // ランチャーアイコンを持つアプリを取得し、除外リストにないものを抽出
        val apps = pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { app -> !excludePackages.contains(app.packageName) }
            .sortedBy { it.loadLabel(pm).toString() }

        // --- ログ出力（YouTube や Chrome が含まれているか確認用） ---
        Log.d("FILTERED_APP_LIST", "--- 表示対象のアプリ一覧 (${apps.size}件) ---")
        apps.forEach { app ->
            val label = app.loadLabel(pm).toString()
            Log.d("FILTERED_APP_LIST", "名前: $label / パッケージ: ${app.packageName}")
        }

        apps // リストを remember に返す
    }

    val initialSelected = remember { TargetAppDataStore.loadTargetApps(context) }
    val selectedApps = remember { mutableStateListOf<String>().apply { addAll(initialSelected) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("監視するアプリを選択") },
        text = {
            if (installedApps.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("対象となるアプリが見つかりません。")
                }
            } else {
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    items(installedApps) { app ->
                        val packageName = app.packageName
                        val label = app.loadLabel(pm).toString()

                        val iconDrawable = app.loadIcon(pm)
                        val iconBitmap = iconDrawable.toBitmap().asImageBitmap()

                        val isChecked = selectedApps.contains(packageName)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selectedApps.remove(packageName)
                                    else selectedApps.add(packageName)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isChecked, onCheckedChange = null)

                            // --- アイコンの表示 ---
                            Image(
                                bitmap = iconBitmap,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(horizontal = 8.dp)
                                    .clip(CircleShape) // 丸く切り抜き（お好みで）
                            )

                            Text(
                                text = label,
                                modifier = Modifier.padding(start = 4.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selectedApps.toSet()) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}

@Composable
fun AlarmSettingCard(
    alarm: AlarmSetting,
    onToggleActive: (Int, Boolean) -> Unit,
    onDeleteAlarm: (Int) -> Unit,
    onEditAlarm: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val (cardColors, typeLabel) = when (alarm.type) {
        AlarmType.WAKE_UP -> Pair(
            CardDefaults.cardColors(
                containerColor = Color(0xFFFFF1CC),
                contentColor = Color(0xFF452B00)
            ),
            "☀️ 起床アラーム"
        )
        AlarmType.BEDTIME -> Pair(
            CardDefaults.cardColors(
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
                if (alarm.name.isNotBlank()) {
                    Text(
                        text = alarm.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cardColors.contentColor
                    )
                }

                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = cardColors.contentColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = alarm.time,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )

                if (alarm.days.isNotBlank()) {
                    Text(
                        text = alarm.days,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cardColors.contentColor.copy(alpha = 0.7f)
                    )
                }
            }

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

@Composable
fun NotificationTimeSelectorDialog(onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    val context = LocalContext.current
    val options = listOf(5, 10, 15, 20, 30, 60)
    val currentSelection = remember { UserPreferencesStore.loadCheckMinutes(context) }
    var selectedValue by remember { mutableStateOf(currentSelection) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("就寝前通知のタイミング") },
        text = {
            Column {
                Text("就寝アラームの何分前から監視を開始しますか？", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                options.forEach { minutes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedValue = minutes }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = (selectedValue == minutes),
                            onClick = { selectedValue = minutes }
                        )
                        Text(text = "${minutes}分前", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selectedValue) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}