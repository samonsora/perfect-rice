package com.example.team1application

// 必要なインポートをすべて追加
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView

// MPAndroidChart関連のインポート
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.components.Legend
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.rememberDateRangePickerState
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.components.AxisBase
import androidx.core.graphics.toColorInt


// --- データ構造とヘルパー関数 ---

/**
 * 1回分の睡眠記録を保持するデータクラス
 */
data class SleepRecord(
    val date: String,
    val sleepTime: String,
    val wakeUpTime: String,
    val bedtime: String,
    val snoozeCount: Int,
    val snoozeDuration: String
)

//テスト用ダミーデータ
fun getDummyRecords(): List<SleepRecord> {
    return listOf(
        SleepRecord("2025/11/25", "7h 30m", "07:00", "23:30", 2, "10分"),
        SleepRecord("2025/11/26", "8h 00m", "07:30", "23:30", 0, "0分"),
        SleepRecord("2025/11/27", "6h 45m", "06:30", "23:45", 3, "15分"),
        SleepRecord("2025/11/28", "7h 15m", "07:15", "00:00", 1, "5分"),
        SleepRecord("2025/11/29", "7h 15m", "07:15", "00:00", 1, "5分"),
        SleepRecord("2025/11/30", "5h 15m", "05:15", "00:00", 1, "5分")
    )
}

/**
 * 睡眠時間文字列（例: "7h 30m"）を時間単位のFloatに変換する
 */
fun parseSleepDuration(sleepTime: String): Float {
    return try {
        val parts = sleepTime.split("h", "m").map { it.trim() }.filter { it.isNotEmpty() }
        val hours = parts.getOrNull(0)?.toFloat() ?: 0f
        val minutes = parts.getOrNull(1)?.toFloat() ?: 0f
        hours + minutes / 60f
    } catch (e: Exception) {
        0f
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RirekiScreen(modifier: Modifier = Modifier) {
    val allRecords = getDummyRecords()

    // 状態をRirekiScreenで定義 (ステートホイスティング)
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }

    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN) }

    // フィルタリングロジック
    val filteredRecords = remember(startDate, endDate) {
        if (startDate == null || endDate == null) {
            allRecords // 期間が未選択なら全て表示
        } else {
            allRecords.filter { record ->
                try {
                    val recordDateMillis = dateFormatter.parse(record.date)?.time ?: 0L
                    val start = startDate!!
                    // 終了日の23:59:59までを範囲に含めるための調整
                    val end = endDate!! + TimeUnit.DAYS.toMillis(1) - 1

                    recordDateMillis in start..end
                } catch (e: Exception) {
                    false // 日付パースエラー時は除外
                }
            }
        }
    }

    // Column でセクションを縦に配置
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 8.dp)) {

        // 1. グラフエリア (フィルタリングされたデータを渡す)
        SleepLineChart(
            records = filteredRecords,
            modifier = Modifier.height(300.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 期間選択ボタン
        DateRangePickerButton(
            startDate = startDate,
            endDate = endDate,
            onDatesSelected = { start, end ->
                startDate = start
                endDate = end
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 表示件数
        Text(text = "表示件数: ${filteredRecords.size}件", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        // 4. 記録リスト
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(filteredRecords) { record ->
                RecordCard(
                    record = record,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

// --- グラフコンポーネント ---

@Composable
fun SleepLineChart(
    records: List<SleepRecord>,
    modifier: Modifier = Modifier
) {
    val entries = records.mapIndexed { index, record ->
        Entry(index.toFloat(), parseSleepDuration(record.sleepTime))
    }
    val xAxisLabels = records.map { it.date }

    AndroidView(
        modifier = modifier.fillMaxWidth().height(300.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                isDragEnabled = true
                axisRight.isEnabled = false
                legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            }
        },
        update = { chart ->
            if (entries.isNotEmpty()) {
                val dataSet = LineDataSet(entries, "睡眠時間 (h)").apply {
                    color = Color.BLUE
                    setCircleColor(Color.BLUE)
                    lineWidth = 3f
                    setDrawFilled(true)
                    fillColor = "#BBDEFB".toColorInt()
                    valueTextSize = 12f
                    setDrawValues(true)
                }

                chart.data = LineData(dataSet)

                chart.xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(xAxisLabels.toTypedArray())
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    labelRotationAngle = -45f
                    setDrawGridLines(false)
                }

                chart.axisLeft.apply {
                    axisMinimum = 0f
                    axisMaximum = 10f

                    // 💡 修正点: YAxisValueFormatter ではなく ValueFormatter を継承する
                    valueFormatter = object : ValueFormatter() {
                        // 必須: getAxisLabel メソッドをオーバーライドする
                        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                            // Y軸の単位を設定 (例: 7.5 -> "7.5 h")
                            return String.format(Locale.US, "%.1f h", value)
                        }
                    }
                }
                chart.animateX(800)
                chart.invalidate()
            } else {
                chart.clear()
            }
        }
    )
}

// --- 期間選択ボタン ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerButton(
    startDate: Long?,
    endDate: Long?,
    onDatesSelected: (Long?, Long?) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val formatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN) }

    Button(
        onClick = { showDatePicker = true },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
    ) {
        val startStr = startDate?.let { formatter.format(Date(it)) } ?: "開始日"
        val endStr = endDate?.let { formatter.format(Date(it)) } ?: "終了日"

        Text(text = "期間: $startStr 〜 $endStr")
    }

    if (showDatePicker) {
        val datePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = startDate,
            initialSelectedEndDateMillis = endDate
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        onDatesSelected(
                            datePickerState.selectedStartDateMillis,
                            datePickerState.selectedEndDateMillis
                        )
                        showDatePicker = false
                    },
                    enabled = datePickerState.selectedStartDateMillis != null &&
                            datePickerState.selectedEndDateMillis != null
                ) {
                    Text("決定")
                }
            },
            dismissButton = {
                Button(onClick = { showDatePicker = false }) {
                    Text("キャンセル")
                }
            }
        ) {
            DateRangePicker(
                state = datePickerState,
                headline = { Text("期間を選択してください") }
            )
        }
    }
}

// --- 記録カード ---

@Composable
fun RecordCard(record: SleepRecord, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = " ${record.date}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(text = "睡眠時間: ${record.sleepTime}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "就寝時間: ${record.bedtime}")
            Text(text = "起床時間: ${record.wakeUpTime}")
            Text(text = "スヌーズ回数: ${record.snoozeCount}回")
            Text(text = "スヌーズ合計時間: ${record.snoozeDuration}")
        }
    }
}