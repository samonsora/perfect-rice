package com.example.team1application

import android.content.Context
import android.graphics.Color
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import java.util.Date
import java.util.Locale

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

// --- 2. データ保存管理 ---
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
        return try {
            val jsonString = file.readText()
            json.decodeFromString<List<SleepRecord>>(jsonString)
        } catch (e: Exception) { emptyList() }
    }
}

// --- 3. 計算・変換ロジック ---
fun calculateDuration(bedtime: String, wakeUp: String, snoozeCount: Int): String {
    return try {
        val sdf = SimpleDateFormat("HH:mm", Locale.JAPAN)
        val bedDate = sdf.parse(bedtime) ?: return "0h 00m"
        val wakeDate = sdf.parse(wakeUp) ?: return "0h 00m"
        var diff = wakeDate.time - bedDate.time
        if (diff < 0) diff += 24 * 60 * 60 * 1000
        val totalMinutes = diff / (1000 * 60) //
        val netMinutes = totalMinutes - (snoozeCount * 5)
        val finalMinutes = if (netMinutes < 0) 0L else netMinutes
        val hours = finalMinutes / 60
        val mins = finalMinutes % 60
        "${hours}h ${String.format("%02d", mins)}m"
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

sealed class GraphType(val label: String, val unit: String, val dataExtractor: (SleepRecord) -> Float) {
    object SleepDuration : GraphType("総睡眠時間", "h", { parseSleepDuration(it.sleepTime) })
    object SnoozeCount : GraphType("スヌーズ回数", "回", { it.snoozeCount.toFloat() })
    companion object { fun values() = listOf(SleepDuration, SnoozeCount) }
}

// --- 4. メイン画面 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RirekiScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dataManager = remember { SleepDataManager(context) }
    var allRecords by remember { mutableStateOf(dataManager.loadRecords()) }

    // ダイアログ管理用の状態
    var showAddDialog by remember { mutableStateOf(false) }
    var showOverwriteWarning by remember { mutableStateOf(false) }
    var pendingRecord by remember { mutableStateOf<SleepRecord?>(null) }

    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var selectedGraphType by remember { mutableStateOf<GraphType>(GraphType.SleepDuration) }
    var expanded by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN) }

    val filteredRecords = remember(allRecords, startDate, endDate) {
        val list = if (startDate == null || endDate == null) allRecords
        else allRecords.filter { record ->
            try {
                val recordDateMillis = dateFormatter.parse(record.date)?.time ?: 0L
                recordDateMillis in startDate!!..(endDate!! + 86399999)
            } catch (e: Exception) { false }
        }
        list.sortedBy { it.date } // 日付順に並び替え
    }
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "追加")
            }
        }
    ) { padding ->
        Column(modifier = modifier.padding(padding).padding(horizontal = 8.dp).fillMaxSize()) {

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                TextField(
                    value = selectedGraphType.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("表示項目") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    GraphType.values().forEach { type ->
                        DropdownMenuItem(text = { Text(type.label) }, onClick = { selectedGraphType = type; expanded = false })
                    }
                }
            }

            CustomBarChart(
                barEntries = filteredRecords.mapIndexed { i, r -> BarEntry(i.toFloat(), selectedGraphType.dataExtractor(r)) },
                xAxisLabels = filteredRecords.map { it.date },
                graphType = selectedGraphType,
                modifier = Modifier.height(250.dp)
            )

            DateRangePickerButton(startDate, endDate) { s, e -> startDate = s; endDate = e }

            Text("記録一覧 (${filteredRecords.size}件)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filteredRecords, key = { it.id }) { record ->
                    RecordCard(record = record, onDelete = {
                        val newList = allRecords.filter { it.id != record.id }
                        allRecords = newList
                        dataManager.saveRecords(newList)
                    })
                }
            }
        }
    }

    // 追加ダイアログ
    if (showAddDialog) {
        AddRecordDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newRecord ->
                val exists = allRecords.any { it.date == newRecord.date }
                if (exists) {
                    pendingRecord = newRecord
                    showOverwriteWarning = true
                    showAddDialog = false
                } else {
                    val newList = (allRecords + newRecord).sortedBy { it.date }
                    allRecords = newList
                    dataManager.saveRecords(newList)
                    showAddDialog = false
                }
            }
        )
    }

    // 上書き確認ダイアログ
    if (showOverwriteWarning && pendingRecord != null) {
        AlertDialog(
            onDismissRequest = { showOverwriteWarning = false },
            title = { Text("上書きの確認") },
            text = { Text("${pendingRecord?.date} の記録は既に存在します。上書きしてもよろしいですか？") },
            confirmButton = {
                Button(onClick = {
                    val recordToSave = pendingRecord!!
                    val newList = (allRecords.filter { it.date != recordToSave.date } + recordToSave).sortedBy { it.date }
                    allRecords = newList
                    dataManager.saveRecords(newList)
                    showOverwriteWarning = false
                    pendingRecord = null
                    Toast.makeText(context, "上書きしました", Toast.LENGTH_SHORT).show()
                }) { Text("上書きする") }
            },
            dismissButton = {
                TextButton(onClick = { showOverwriteWarning = false; pendingRecord = null }) { Text("キャンセル") }
            }
        )
    }
}

// --- 5. 追加ダイアログ ---
@Composable
fun AddRecordDialog(onDismiss: () -> Unit, onConfirm: (SleepRecord) -> Unit) {
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(Date())) }
    var bedtime by remember { mutableStateOf("23:00") }
    var wakeUpTime by remember { mutableStateOf("07:00") }
    var snoozeCount by remember { mutableStateOf("0") }
    val autoSleepTime by remember { derivedStateOf { calculateDuration(bedtime, wakeUpTime, snoozeCount.toIntOrNull() ?: 0) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("睡眠記録の追加") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("日付 (yyyy/MM/dd)") })
                OutlinedTextField(value = bedtime, onValueChange = { bedtime = it }, label = { Text("就寝時刻 (HH:mm)") })
                OutlinedTextField(value = wakeUpTime, onValueChange = { wakeUpTime = it }, label = { Text("起床時刻 (HH:mm)") })
                OutlinedTextField(
                    value = snoozeCount,
                    onValueChange = { if (it.all { char -> char.isDigit() }) snoozeCount = it },
                    label = { Text("スヌーズ回数 (1回5分)") }
                )
                Text("自動計算された睡眠時間: $autoSleepTime", color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(SleepRecord(
                    date = date, sleepTime = autoSleepTime, wakeUpTime = wakeUpTime,
                    bedtime = bedtime, snoozeCount = snoozeCount.toIntOrNull() ?: 0,
                    snoozeDuration = "${(snoozeCount.toIntOrNull() ?: 0) * 5}分"
                ))
            }) { Text("追加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
    )
}

// --- 6. グラフ (横スクロール対応) ---
@Composable
fun CustomBarChart(barEntries: List<BarEntry>, xAxisLabels: List<String>, graphType: GraphType, modifier: Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                axisRight.isEnabled = false
                legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    isGranularityEnabled = true
                    labelRotationAngle = -45f
                }
                axisLeft.axisMinimum = 0f
                setBackgroundColor(Color.WHITE)
                // スクロール設定
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(false)
                setPinchZoom(false)
            }
        },
        update = { chart ->
            if (barEntries.isNotEmpty()) {
                val dataSet = BarDataSet(barEntries, "${graphType.label} (${graphType.unit})").apply {
                    color = Color.rgb(176, 224, 230)
                    valueTextSize = 10f
                }
                chart.data = BarData(dataSet).apply { barWidth = 0.6f }
                chart.xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(xAxisLabels.toTypedArray())
                    axisMinimum = -0.5f
                    axisMaximum = barEntries.size.toFloat() - 0.5f
                }
                // 重要：データの更新後に表示範囲を制限
                chart.setVisibleXRangeMaximum(7f)
                chart.moveViewToX(barEntries.size.toFloat())
                chart.invalidate()
            } else { chart.clear() }
        }
    )
}

@Composable
fun RecordCard(record: SleepRecord, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.date, style = MaterialTheme.typography.titleMedium)
                Text("睡眠時間: ${record.sleepTime} (${record.bedtime} 〜 ${record.wakeUpTime})", style = MaterialTheme.typography.bodySmall)
                Text("スヌーズ: ${record.snoozeCount}回", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerButton(startDate: Long?, endDate: Long?, onSelected: (Long?, Long?) -> Unit) {
    var show by remember { mutableStateOf(false) }
    val fmt = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
    Button(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) {
        Text("期間: ${startDate?.let { fmt.format(it) } ?: "未設定"} 〜 ${endDate?.let { fmt.format(it) } ?: "未設定"}")
    }
    if (show) {
        val state = rememberDateRangePickerState(initialSelectedStartDateMillis = startDate, initialSelectedEndDateMillis = endDate)
        DatePickerDialog(onDismissRequest = { show = false }, confirmButton = {
            Button(onClick = { onSelected(state.selectedStartDateMillis, state.selectedEndDateMillis); show = false }) { Text("決定") }
        }) { DateRangePicker(state = state) }
    }
}