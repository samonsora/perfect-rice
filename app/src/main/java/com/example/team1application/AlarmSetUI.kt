package com.example.team1application

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSetUI(
    onDismiss: () -> Unit,
    onSave: (AlarmSetting) -> Unit,
    onDelete: () -> Unit,
    initialTime: String,
    isNewAlarm: Boolean,
    existingAlarm: AlarmSetting? = null
) {
    val context = LocalContext.current

    // --- 状態管理 ---
    var alarmTime by remember { mutableStateOf(initialTime) }
    var alarmName by remember { mutableStateOf(existingAlarm?.name ?: "") }
    var alarmVolume by remember { mutableFloatStateOf(existingAlarm?.volume ?: 0.5f) }
    var isFadeInEnabled by remember { mutableStateOf(existingAlarm?.fadeIn ?: false) }
    var alarmType by remember { mutableStateOf(existingAlarm?.type ?: AlarmType.WAKE_UP) }
    var alarmDays by remember { mutableStateOf(existingAlarm?.days ?: "") }

    val soundOptions = remember {
        mutableStateListOf("alarmsound1", "+アラーム音を追加", "-アラーム音を削除")
    }
    var alarmSound by remember { mutableStateOf(existingAlarm?.soundName ?: "alarmsound1") }

    // ファイル選択ランチャー
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileNameWithExt = getFileName(context, it)
            if (fileNameWithExt?.endsWith(".mp3", ignoreCase = true) == true) {
                val fileName = fileNameWithExt.substringBeforeLast(".")
                if (saveFileToInternalStorage(context, it, "$fileName.mp3")) {
                    val insertIndex = soundOptions.indexOf("+アラーム音を追加")
                    if (insertIndex != -1 && !soundOptions.contains(fileName)) {
                        soundOptions.add(insertIndex, fileName)
                    }
                    alarmSound = fileName
                }
            }
        }
    }

    val snoozeIntervalOptions = listOf("なし", "5分", "10分", "15分", "30分")
    val snoozeCountOptions = listOf("無制限", "1回", "2回", "3回", "4回", "5回")

    var snoozeInterval by remember { mutableStateOf(existingAlarm?.snoozeInterval ?: snoozeIntervalOptions[0]) }
    var snoozeCount by remember { mutableStateOf(existingAlarm?.snoozeCount ?: snoozeCountOptions[0]) }

    var showTimeDialog by remember { mutableStateOf(false) }
    var showVolumeDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var showRepeatDialog by remember { mutableStateOf(false) }
    var showSoundDeleteDialog by remember { mutableStateOf(false) }

    val customSoundList = soundOptions.filter {
        it != "alarmsound1" && it != "+アラーム音を追加" && it != "-アラーム音を削除"
    }

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る", tint = onSurfaceColor)
                }
                Text("アラームの設定", style = MaterialTheme.typography.titleLarge, color = onSurfaceColor)
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp, 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                if (!isNewAlarm) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) { Text("削除") }
                    Spacer(Modifier.width(8.dp))
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = onSurfaceColor
                    )
                ) { Text("キャンセル") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        onSave(AlarmSetting(
                            id = existingAlarm?.id ?: -1,
                            time = alarmTime,
                            days = alarmDays,
                            isActive = existingAlarm?.isActive ?: true,
                            name = alarmName,
                            snoozeInterval = snoozeInterval,
                            snoozeCount = snoozeCount,
                            volume = alarmVolume,
                            fadeIn = isFadeInEnabled,
                            type = alarmType,
                            soundName = alarmSound
                        ))
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) { Text("完了") }
            }
        }
    ) { paddingValues ->
        AlarmSettingListContent(
            modifier = Modifier.padding(paddingValues),
            currentTime = alarmTime,
            onTimeClick = { showTimeDialog = true },
            alarmName = alarmName,
            onNameClick = { showNameDialog = true },
            alarmDays = alarmDays,
            onRepeatClick = { showRepeatDialog = true },
            snoozeInterval = snoozeInterval,
            onSnoozeIntervalChange = { newInterval ->
                snoozeInterval = newInterval
                if (newInterval == "なし") snoozeCount = snoozeCountOptions[0]
            },
            snoozeCount = snoozeCount,
            onSnoozeCountChange = { snoozeCount = it },
            snoozeIntervalOptions = snoozeIntervalOptions,
            snoozeCountOptions = snoozeCountOptions,
            alarmVolume = alarmVolume,
            onVolumeClick = { showVolumeDialog = true },
            isFadeInEnabled = isFadeInEnabled,
            onFadeInChange = { isFadeInEnabled = it },
            alarmType = alarmType,
            onTypeChange = { alarmType = it },
            selectedSound = alarmSound,
            soundOptions = soundOptions,
            onSoundChange = { selected ->
                when (selected) {
                    "+アラーム音を追加" -> launcher.launch("audio/mpeg")
                    "-アラーム音を削除" -> {
                        showSoundDeleteDialog = true // 削除ダイアログを表示
                    }
                    else -> alarmSound = selected
                }
            },
        )
    }

    // --- 各種ダイアログ ---
    if (showTimeDialog) {
        AlarmSetupDialog(
            onDismiss = { showTimeDialog = false },
            onSave = { alarmTime = it; showTimeDialog = false }
        )
    }
    if (showNameDialog) {
        AlarmNameEditDialog(
            initialName = alarmName,
            onDismiss = { showNameDialog = false },
            onSave = { alarmName = it; showNameDialog = false }
        )
    }
    if (showRepeatDialog) {
        AlarmRepeatDialog(
            initialDays = alarmDays,
            onDismiss = { showRepeatDialog = false },
            onSave = { alarmDays = it; showRepeatDialog = false }
        )
    }
    if (showVolumeDialog) {
        AlarmVolumeDialog(
            initialVolume = alarmVolume,
            soundName = alarmSound,
            onDismiss = { showVolumeDialog = false },
            onSave = { alarmVolume = it; showVolumeDialog = false }
        )
    }
    // --- サウンド削除ダイアログを表示するロジック ---
    if (showSoundDeleteDialog) {
        SoundDeleteDialog(
            customSounds = customSoundList,
            onDismiss = { showSoundDeleteDialog = false },
            onDelete = { soundToDelete ->
                // 1. 内部ストレージからファイルを物理削除
                val file = File(context.filesDir, "$soundToDelete.mp3")
                if (file.exists()) {
                    file.delete()
                }
                // 2. 表示用リスト(soundOptions)から削除
                soundOptions.remove(soundToDelete)
                // 3. 現在選択中の音が削除された場合、デフォルト音に戻す
                if (alarmSound == soundToDelete) {
                    alarmSound = "alarmsound1"
                }
                // すべて削除した場合はダイアログを閉じる
                if (soundOptions.none { it != "alarmsound1" && !it.startsWith("+") && !it.startsWith("-") }) {
                    showSoundDeleteDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSettingListContent(
    modifier: Modifier = Modifier,
    currentTime: String,
    onTimeClick: () -> Unit,
    alarmName: String,
    onNameClick: () -> Unit,
    alarmDays: String,
    onRepeatClick: () -> Unit,
    snoozeInterval: String,
    onSnoozeIntervalChange: (String) -> Unit,
    snoozeCount: String,
    onSnoozeCountChange: (String) -> Unit,
    snoozeIntervalOptions: List<String>,
    snoozeCountOptions: List<String>,
    alarmVolume: Float,
    onVolumeClick: () -> Unit,
    isFadeInEnabled: Boolean,
    onFadeInChange: (Boolean) -> Unit,
    alarmType: AlarmType,
    onTypeChange: (AlarmType) -> Unit,
    selectedSound: String,
    soundOptions: List<String>,
    onSoundChange: (String) -> Unit
) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        fun header(text: String) = item {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
            )
        }

        header("アラームの種類")
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = alarmType == AlarmType.WAKE_UP,
                        onClick = { onTypeChange(AlarmType.WAKE_UP) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.height(52.dp).weight(1f)
                    ) {
                        Text("起床", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    SegmentedButton(
                        selected = alarmType == AlarmType.BEDTIME,
                        onClick = { onTypeChange(AlarmType.BEDTIME) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.height(52.dp).weight(1f)
                    ) {
                        Text("就寝", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        header("基本設定")
        item { AlarmTimeSettingItem(time = currentTime, onClick = onTimeClick) }
        item { AlarmSimpleSettingItem(title = "繰り返し", subtitle = alarmDays.ifBlank { "毎日" }, onClick = onRepeatClick) }

        // 起床時のみスヌーズ設定を表示
        if (alarmType == AlarmType.WAKE_UP) {
            item { AlarmDropdownSettingItem("スヌーズの間隔", snoozeInterval, snoozeIntervalOptions, onSnoozeIntervalChange) }
            if (snoozeInterval != "なし") {
                item { AlarmDropdownSettingItem("スヌーズの回数", snoozeCount, snoozeCountOptions, onSnoozeCountChange) }
            }
        }

        header("アラーム名")
        item { AlarmSimpleSettingItem("アラーム名", alarmName.ifBlank { "指定なし" }, onClick = onNameClick) }

        header("アラーム音の設定")
        item { AlarmDropdownSettingItem("アラーム音", selectedSound, soundOptions, onSoundChange) }
        item { AlarmVolumeSettingItem(volume = alarmVolume, onClick = onVolumeClick) }
        item { AlarmSwitchSettingItem(title = "フェードイン", subtitle = "音量を徐々に大きくする", checked = isFadeInEnabled, onCheckedChange = onFadeInChange) }
    }
}

/**
 * 音量設定＆試聴ダイアログ
 */

@Composable
fun AlarmVolumeDialog(initialVolume: Float, soundName: String, onDismiss: () -> Unit, onSave: (Float) -> Unit) {
    val context = LocalContext.current
    var currentVolume by remember { mutableFloatStateOf(initialVolume) }
    var isPlaying by remember { mutableStateOf(false) }

    val mediaPlayer = remember(soundName) {
        val mp = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = true
        }
        try {
            val resId = context.resources.getIdentifier(soundName, "raw", context.packageName)
            if (resId != 0) {
                mp.setDataSource(context, "android.resource://${context.packageName}/$resId".toUri())
            } else {
                val file = File(context.filesDir, "$soundName.mp3")
                if (file.exists()) mp.setDataSource(file.absolutePath) else return@remember null
            }
            mp.prepare()
            mp.setVolume(currentVolume, currentVolume)
            mp
        } catch (_: Exception) { null }
    }

    LaunchedEffect(currentVolume) { mediaPlayer?.setVolume(currentVolume, currentVolume) }
    DisposableEffect(Unit) { onDispose { mediaPlayer?.release() } }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("最大音量を設定", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Text("${(currentVolume * 100).roundToInt()}%", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                Slider(value = currentVolume, onValueChange = { currentVolume = it })
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    if (isPlaying) mediaPlayer?.pause() else mediaPlayer?.start()
                    isPlaying = !isPlaying
                }, modifier = Modifier.fillMaxWidth()) { Text(if (isPlaying) "■ 停止" else "▶ 試聴する") }
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("キャンセル") }
                    Button(onClick = { onSave(currentVolume) }) { Text("完了") }
                }
            }
        }
    }
}

/**
 * 繰り返し設定ダイアログ
 */
@Composable
fun AlarmRepeatDialog(initialDays: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val dayOptions = listOf("月", "火", "水", "木", "金", "土", "日")
    val initialSelected = if (initialDays == "毎日" || initialDays.isBlank()) emptySet() else initialDays.split(",").toSet()
    var selectedDays by remember { mutableStateOf(initialSelected) }
    var repeatMode by remember { mutableStateOf(if (initialDays == "毎日" || initialDays.isBlank()) "毎日" else "曜日指定") }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("繰り返し", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Column(Modifier.selectableGroup()) {
                    listOf("毎日", "曜日指定").forEach { text ->
                        Row(
                            Modifier.fillMaxWidth().height(48.dp).selectable(
                                selected = (text == repeatMode),
                                onClick = { repeatMode = text },
                                role = Role.RadioButton
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (text == repeatMode), onClick = null)
                            Text(text = text, modifier = Modifier.padding(start = 16.dp))
                        }
                    }
                }
                if (repeatMode == "曜日指定") {
                    dayOptions.forEach { day ->
                        Row(
                            modifier = Modifier.fillMaxWidth().height(40.dp).clickable {
                                selectedDays = if (selectedDays.contains(day)) selectedDays - day else selectedDays + day
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = selectedDays.contains(day), onCheckedChange = null)
                            Text(text = "${day}曜日", modifier = Modifier.padding(start = 16.dp))
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("キャンセル") }
                    Button(onClick = {
                        val result = if (repeatMode == "毎日") "" else dayOptions.filter { selectedDays.contains(it) }.joinToString(",")
                        onSave(result)
                    }) { Text("OK") }
                }
            }
        }
    }
}

/**
 * 名前編集ダイアログ
 */
@Composable
fun AlarmNameEditDialog(initialName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initialName) }
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("アラーム名を入力", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("キャンセル") }
                    Button(onClick = { onSave(text) }) { Text("OK") }
                }
            }
        }
    }
}

// --- 補助パーツ ---

@Composable
fun AlarmSwitchSettingItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).padding(16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp)
}

@Composable
fun AlarmSimpleSettingItem(title: String, subtitle: String, onClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).clickable(onClick = onClick).padding(16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp)
}

@Composable
fun AlarmDropdownSettingItem(title: String, selectedValue: String, options: List<String>, onValueChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).clickable { expanded = true }.padding(16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(selectedValue, color = MaterialTheme.colorScheme.primary)
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onValueChange(option); expanded = false }) }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp)
}

@Composable
fun AlarmTimeSettingItem(time: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 72.dp).clickable(onClick = onClick).padding(16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("時刻", style = MaterialTheme.typography.bodyLarge)
            Text(time, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp)
}

@Composable
fun AlarmVolumeSettingItem(volume: Float, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).clickable(onClick = onClick).padding(16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("最大音量", style = MaterialTheme.typography.bodyLarge)
        Text("${(volume * 100).roundToInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp)
}

/**
 * URIからファイル名を取得する（例: sample.mp3）
 */
private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) result = it.getString(index)
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) result = result?.substring(cut + 1)
    }
    return result
}

/**
 * 選択されたファイルをアプリの内部ストレージにコピー保存する
 */
private fun saveFileToInternalStorage(context: Context, uri: Uri, fileName: String): Boolean {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val file = File(context.filesDir, fileName)
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

/**
 * アラーム音削除選択ダイアログ
 */
@Composable
fun SoundDeleteDialog(
    customSounds: List<String>,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("削除する音を選択", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                if (customSounds.isEmpty()) {
                    Text("追加されたアラーム音はありません。", modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(customSounds.size) { index ->
                            val sound = customSounds[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDelete(sound) }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = sound, style = MaterialTheme.typography.bodyLarge)
                                Icon(
                                    imageVector = Icons.Default.Delete, // ※Icons.Default.Deleteを要インポート
                                    contentDescription = "削除",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("閉じる") }
                }
            }
        }
    }
}