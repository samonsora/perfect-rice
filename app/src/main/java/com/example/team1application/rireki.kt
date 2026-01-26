package com.example.team1application

import android.content.Context
import android.graphics.Color
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// --- 1. データ構造 ---
@Serializable
data class SleepRecord(
    val id: Long = System.currentTimeMillis(),
    val date: String,
    val sleepTime: String,
    val wakeUpTime: String,
    val bedtime: String,
    val snoozeCount: Int,
    val snoozeDuration: String
)

// --- 2. データ管理クラス ---
class SleepDataManager(private val context: Context) {
    private val fileName = "sleep_records.json"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; coerceInputValues = true }
    fun saveRecords(records: List<SleepRecord>) {
        try {
            val jsonString = json.encodeToString(records)
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { it.write(jsonString.toByteArray()) }
        } catch (e: Exception) { e.printStackTrace() }
    }
    fun loadRecords(): List<SleepRecord> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return emptyList()
        return try { json.decodeFromString<List<SleepRecord>>(file.readText()) } catch (e: Exception) { emptyList() }
    }
}

// --- 3. ロジック関数 ---
fun calculateDuration(bedtime: String, wakeUp: String, snoozeCount: Int): String {
    return try {
        val sdf = SimpleDateFormat("HH:mm", Locale.JAPAN)
        val bedDate = sdf.parse(bedtime) ?: return "0h 00m"
        val wakeDate = sdf.parse(wakeUp) ?: return "0h 00m"
        var diff = wakeDate.time - bedDate.time
        if (diff < 0) diff += 24 * 60 * 60 * 1000
        val totalMinutes = diff / (1000 * 60)
        val netMinutes = totalMinutes - (snoozeCount * 5)
        val finalMinutes = if (netMinutes < 0) 0L else netMinutes
        "${finalMinutes / 60}h ${String.format("%02d", finalMinutes % 60)}m"
    } catch (e: Exception) { "0h 00m" }
}

fun parseSleepDuration(sleepTime: String): Float {
    return try {
        val parts = sleepTime.split("h", "m").map { it.trim() }.filter { it.isNotEmpty() }
        val hours = parts.getOrNull(0)?.toFloat() ?: 0f
        val minutes = parts.getOrNull(1)?.toFloat() ?: 0f
        hours + minutes / 60f
    } catch (e: Exception) { 0f }
}

// --- 4. メイン画面 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RirekiScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dataManager = remember { SleepDataManager(context) }
    var allRecords by remember { mutableStateOf(dataManager.loadRecords()) }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<SleepRecord?>(null) }
    var showOverwriteWarning by remember { mutableStateOf(false) }
    var pendingRecord by remember { mutableStateOf<SleepRecord?>(null) }

    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN) }

    // 初期期間の設定（今週の月曜〜日曜）
    val calendar = Calendar.getInstance()
    var startDate by remember {
        mutableStateOf<Long?>(calendar.apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis)
    }
    var endDate by remember {
        mutableStateOf<Long?>(calendar.apply { add(Calendar.DATE, 6); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59) }.timeInMillis)
    }

    // 🌟 歯抜け防止ロジック: 選択期間内の全日付リストを作成
    val weekDays = remember(startDate, endDate) {
        val days = mutableListOf<String>()
        if (startDate != null && endDate != null) {
            val cal = Calendar.getInstance().apply { timeInMillis = startDate!! }
            val endCal = Calendar.getInstance().apply { timeInMillis = endDate!! }
            while (!cal.after(endCal)) {
                days.add(dateFormatter.format(cal.time))
                cal.add(Calendar.DATE, 1)
            }
        }
        days
    }

    // 🌟 歯抜け防止ロジック: 全日付にデータをマッピング（無い日は0）
    val barEntries = remember(allRecords, weekDays) {
        val recordMap = allRecords.associateBy { it.date }
        weekDays.mapIndexed { index, date ->
            val duration = recordMap[date]?.let { parseSleepDuration(it.sleepTime) } ?: 0f
            BarEntry(index.toFloat(), duration)
        }
    }

    val weeklyAvgSleep = remember(barEntries) {
        val activeEntries = barEntries.filter { it.y > 0f }
        if (activeEntries.isNotEmpty()) activeEntries.map { it.y }.average() else 0.0
    }

    // リスト表示用（選択期間内の記録があるものだけ）
    val filteredRecordsForList = remember(allRecords, startDate, endDate) {
        allRecords.filter { record ->
            val time = try { dateFormatter.parse(record.date)?.time ?: 0L } catch (e: Exception) { 0L }
            time in (startDate ?: 0L)..(endDate ?: Long.MAX_VALUE)
        }.sortedByDescending { it.date }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Default.Add, contentDescription = "追加")
            }
        }
    ) { padding ->
        Column(modifier = modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize()) {

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("睡眠推移", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NightsStay, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                        Text(String.format(" 平均 %.1fh", weeklyAvgSleep), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 📊 グラフエリア
            Card(
                modifier = Modifier.fillMaxWidth().height(260.dp).padding(vertical = 8.dp).shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
            ) {
                CustomBarChart(
                    barEntries = barEntries,
                    xAxisLabels = weekDays,
                    modifier = Modifier.padding(8.dp)
                )
            }

            DateRangePickerButton(startDate, endDate) { s, e -> startDate = s; endDate = e }

            Text("記録一覧 (${filteredRecordsForList.size}件)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredRecordsForList, key = { it.id }) { record ->
                    EnhancedRecordCard(
                        record = record,
                        onDelete = {
                            val newList = allRecords.filter { it.id != record.id }
                            allRecords = newList
                            dataManager.saveRecords(newList)
                        },
                        onEdit = { editingRecord = record }
                    )
                }
            }
        }
    }

    // --- ダイアログ類 ---
    if (showAddDialog || editingRecord != null) {
        AddOrEditRecordDialog(
            existingRecord = editingRecord,
            onDismiss = { showAddDialog = false; editingRecord = null },
            onConfirm = { newRecord ->
                val dateConflict = allRecords.any { it.date == newRecord.date && it.id != newRecord.id }
                if (dateConflict) { pendingRecord = newRecord; showOverwriteWarning = true }
                else {
                    val newList = if (allRecords.any { it.id == newRecord.id }) {
                        allRecords.map { if (it.id == newRecord.id) newRecord else it }
                    } else { allRecords + newRecord }
                    allRecords = newList.sortedBy { it.date }; dataManager.saveRecords(allRecords)
                }
                showAddDialog = false; editingRecord = null
            }
        )
    }

    if (showOverwriteWarning && pendingRecord != null) {
        AlertDialog(
            onDismissRequest = { showOverwriteWarning = false },
            title = { Text("上書きの確認") },
            text = { Text("${pendingRecord?.date} の記録を上書きしますか？") },
            confirmButton = { Button(onClick = {
                val recordToSave = pendingRecord!!
                allRecords = (allRecords.filter { it.date != recordToSave.date } + recordToSave).sortedBy { it.date }
                dataManager.saveRecords(allRecords); showOverwriteWarning = false; pendingRecord = null
            }) { Text("上書き") } },
            dismissButton = { TextButton(onClick = { showOverwriteWarning = false; pendingRecord = null }) { Text("キャンセル") } }
        )
    }
}

// --- 5. グラフコンポーネント ---
@Composable
fun CustomBarChart(barEntries: List<BarEntry>, xAxisLabels: List<String>, modifier: Modifier) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false; axisRight.isEnabled = false
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    labelRotationAngle = -45f
                    axisMinimum = -0.5f
                }
                axisLeft.apply {
                    axisMinimum = 0f
                    axisMaximum = 12f
                    setDrawGridLines(true)
                }
                setTouchEnabled(true); setScaleEnabled(false)
            }
        },
        update = { chart ->
            // 🌟 色分け: 0時間は薄いグレー
            val colors = barEntries.map {
                if (it.y > 0f) Color.rgb(100, 149, 237) else Color.LTGRAY
            }

            if (barEntries.isNotEmpty()) {
                val dataSet = BarDataSet(barEntries, "睡眠時間 (h)").apply {
                    this.colors = colors
                    valueTextSize = 10f
                    valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                        override fun getFormattedValue(value: Float): String =
                            if (value > 0f) String.format("%.1f", value) else ""
                    }
                }
                chart.data = BarData(dataSet).apply { barWidth = 0.5f }
                chart.xAxis.axisMaximum = barEntries.size.toFloat() - 0.5f
                chart.xAxis.valueFormatter = IndexAxisValueFormatter(xAxisLabels.map { if (it.length >= 10) it.substring(5) else it })
                chart.notifyDataSetChanged()
                chart.invalidate()
            } else { chart.clear() }
        }
    )
}

// --- 6. 強化版カード ---
@Composable
fun EnhancedRecordCard(record: SleepRecord, onDelete: () -> Unit, onEdit: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (!isDark) androidx.compose.ui.graphics.Color.White
            else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.date, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(record.sleepTime, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Text("${record.bedtime} 〜 ${record.wakeUpTime}", style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color.Gray)
                if (record.snoozeCount > 0) {
                    Text("スヌーズ: ${record.snoozeCount}回", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "編集", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "削除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

// --- 7. 期間選択 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerButton(startDate: Long?, endDate: Long?, onSelected: (Long?, Long?) -> Unit) {
    var show by remember { mutableStateOf(false) }
    val fmt = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { if (startDate != null && endDate != null) onSelected(startDate - 7L*24*60*60*1000, endDate - 7L*24*60*60*1000) }) { Icon(Icons.Default.ChevronLeft, null) }
        Surface(
            modifier = Modifier.weight(1f).height(40.dp).shadow(2.dp, RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(20.dp),
            onClick = { show = true }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("${startDate?.let { fmt.format(it) } ?: "..." } 〜 ${endDate?.let { fmt.format(it) } ?: "..."}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
        IconButton(onClick = { if (startDate != null && endDate != null) onSelected(startDate + 7L*24*60*60*1000, endDate + 7L*24*60*60*1000) }) { Icon(Icons.Default.ChevronRight, null) }
    }
    if (show) {
        val state = rememberDateRangePickerState(initialSelectedStartDateMillis = startDate, initialSelectedEndDateMillis = endDate)
        DatePickerDialog(onDismissRequest = { show = false }, confirmButton = {
            Button(onClick = { onSelected(state.selectedStartDateMillis, state.selectedEndDateMillis); show = false }) { Text("OK") }
        }) { DateRangePicker(state = state, modifier = Modifier.height(400.dp)) }
    }
}

// --- 8. 入力ダイアログ ---
@Composable
fun AddOrEditRecordDialog(existingRecord: SleepRecord? = null, onDismiss: () -> Unit, onConfirm: (SleepRecord) -> Unit) {
    var date by remember { mutableStateOf(existingRecord?.date ?: SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(Date())) }
    var bedtime by remember { mutableStateOf(existingRecord?.bedtime ?: "23:00") }
    var wakeUpTime by remember { mutableStateOf(existingRecord?.wakeUpTime ?: "07:00") }
    var snoozeCount by remember { mutableStateOf(existingRecord?.snoozeCount?.toString() ?: "0") }
    val safeSnoozeCount = snoozeCount.toIntOrNull() ?: 0
    val autoSleepTime by remember(bedtime, wakeUpTime, safeSnoozeCount) { derivedStateOf { calculateDuration(bedtime, wakeUpTime, safeSnoozeCount) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingRecord == null) "睡眠記録の追加" else "睡眠記録の編集") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("日付") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bedtime, onValueChange = { bedtime = it }, label = { Text("就寝時刻") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = wakeUpTime, onValueChange = { wakeUpTime = it }, label = { Text("起床時刻") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = snoozeCount, onValueChange = { if (it.all { c -> c.isDigit() }) snoozeCount = it }, label = { Text("スヌーズ回数") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text("自動計算: $autoSleepTime", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(SleepRecord(id = existingRecord?.id ?: System.currentTimeMillis(), date = date, sleepTime = autoSleepTime, wakeUpTime = wakeUpTime, bedtime = bedtime, snoozeCount = safeSnoozeCount, snoozeDuration = "${safeSnoozeCount * 5}分")) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
    )
}