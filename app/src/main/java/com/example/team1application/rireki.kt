package com.example.team1application

// 必要なインポートをすべて追加
import android.R.attr.fillColor
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.* // ExposedDropdownMenuBoxなどを含む
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.components.AxisBase


// --- データ構造とヘルパー関数 ---


/**
 * 1回分の睡眠記録を保持するデータクラス
 */
data class SleepRecord(
    val date: String,
    val sleepTime: String, // 例: "7h 30m"
    val wakeUpTime: String,
    val bedtime: String,
    val snoozeCount: Int, // スヌーズ回数
    val snoozeDuration: String
)

//テスト用ダミーデータ
fun getDummyRecords(): List<SleepRecord> {
    return listOf(
        SleepRecord("2025/12/2", "7h 30m", "07:00", "23:30", 2, "10分"),
        SleepRecord("2025/12/3", "8h 00m", "07:30", "23:30", 0, "0分"),
        SleepRecord("2025/12/4", "6h 45m", "06:30", "23:45", 3, "15分"),
        SleepRecord("2025/12/5", "7h 15m", "07:15", "00:00", 1, "5分"),
        SleepRecord("2025/12/6", "7h 15m", "07:15", "00:00", 1, "5分"),
        SleepRecord("2025/12/7", "5h 15m", "05:15", "00:00", 1, "5分")
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

// ★ グラフの選択項目を定義するSealed Class ★
sealed class GraphType(
    val label: String,
    val unit: String,
    val dataExtractor: (SleepRecord) -> Float // データ抽出関数
) {
    // 睡眠時間
    object SleepDuration : GraphType("総睡眠時間", "h", { parseSleepDuration(it.sleepTime) })

    // スヌーズ回数
    object SnoozeCount : GraphType("スヌーズ回数", "回", { it.snoozeCount.toFloat() })

    companion object {
        fun values() = listOf(SleepDuration, SnoozeCount)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RirekiScreen(modifier: Modifier = Modifier) {
    val allRecords = getDummyRecords()



    // 状態iをRirekScreenで定義 (ステートホイスティング)
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }

    // ★ グラフ選択の状態 ★
    val graphTypes = GraphType.values()
    var selectedGraphType: GraphType by remember { mutableStateOf(GraphType.SleepDuration) }
    var expanded by remember { mutableStateOf(false) }

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
                    val end = endDate!! + TimeUnit.DAYS.toMillis(1) - 1

                    recordDateMillis in start..end
                } catch (e: Exception) {
                    false // 日付パースエラー時は除外
                }
            }
        }
    }

    // ★ 選択されたGraphTypeに基づき、グラフデータを計算 ★
    val barEntries = remember(filteredRecords, selectedGraphType) {
        filteredRecords.mapIndexed { index, record ->
            BarEntry(
                index.toFloat(),
                selectedGraphType.dataExtractor(record) // 選択項目に応じた値を抽出
            )
        }
    }

    // X軸ラベルは日付で固定
    val xAxisLabels = filteredRecords.map { it.date }


    // Column でセクションを縦に配置
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 8.dp)) {

        // ★ ドロップダウンメニューの実装 ★
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            TextField(
                value = selectedGraphType.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("表示項目") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier
                    .menuAnchor(
                        type = MenuAnchorType.PrimaryNotEditable, // 読み取り専用なのでこれを使用
                        enabled = true
                    )
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                graphTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.label) },
                        onClick = {
                            selectedGraphType = type // 選択項目を更新
                            expanded = false
                        }
                    )
                }
            }
        }


        // 1. グラフエリア (加工済みのデータを渡す)
        CustomBarChart(
            barEntries = barEntries, // 加工済みのデータ
            xAxisLabels = xAxisLabels, // X軸ラベル
            graphType = selectedGraphType, // 選択された GraphType を渡す
            graphTypeKey = selectedGraphType.label,
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

// --- グラフコンポーネント (汎用化) ---

@Composable
fun CustomBarChart(
    barEntries: List<BarEntry>, // ★ BarEntryのリストを受け取る
    xAxisLabels: List<String>, //日付を取得する
    graphType: GraphType,      // ★ GraphTypeを受け取る
    graphTypeKey: String,
    modifier: Modifier = Modifier
) {
    // グラフインスタンスへの参照を保持 (LaunchedEffectからアクセスするため)
    val chartRef = remember { mutableStateOf<BarChart?>(null) }

    AndroidView(
        modifier = modifier.fillMaxWidth().height(300.dp),
        factory = { context ->
            // BarChart を使用
            BarChart(context).apply {
                description.isEnabled = false
                isDragEnabled = false
                axisRight.isEnabled = false
                setScaleEnabled(false) // ズーム無効
                isHighlightPerTapEnabled = true // ハイライト有効
                legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                setBackgroundColor(Color.WHITE) // 背景を白に設定
                chartRef.value = this // ★ インスタンスを保持
            }
        },
        update = { chart ->
            val barChart = chart

            if (barEntries.isNotEmpty()) {

                // 棒の色を動的に設定
                val barColor = if (graphType == GraphType.SnoozeCount)
                    Color.rgb(255, 165, 0) // オレンジ
                else
                    Color.rgb(176, 224, 230) // 薄い水色

                val barDataSet = BarDataSet(barEntries, "${graphType.label} (${graphType.unit})").apply {
                    color = barColor
                    valueTextSize = 12f
                    setDrawValues(true)
                }

                val barData = BarData(barDataSet).apply {
                    barWidth = 0.6f
                }
                barChart.data = barData

                // X軸ラベル配列の安全な取得
                val labelsArray = if (xAxisLabels.isNotEmpty()) {
                    xAxisLabels.toTypedArray()
                } else {
                    arrayOf()
                }

                barChart.xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(labelsArray)
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    labelRotationAngle = -45f
                    setDrawGridLines(false)
                }

                barChart.axisLeft.apply {
                    axisMinimum = 0f
                    // Y軸の最大値を項目に応じて動的に設定
                    axisMaximum = if (graphType == GraphType.SnoozeCount) 5f else 10f

                    // 単位を動的に変更するValueFormatter
                    valueFormatter = object : ValueFormatter() {
                        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                            return String.format(Locale.US, "%.1f ${graphType.unit}", value)
                        }
                    }
                }
                // barChart.animateY(800) <--- UPDATEブロックから削除
                barChart.notifyDataSetChanged()
                barChart.invalidate()
            } else {
                barChart.clear()
            }
        }
    )

    // ★ LaunchedEffectでアニメーションを制御 ★
    // graphTypeKey（項目名）が変わったとき、または初回表示時に実行される
    LaunchedEffect(graphTypeKey) {
        chartRef.value?.apply {
            // データが存在することを確認してからアニメーションを実行
            if (data != null && data.entryCount > 0) {
                animateY(800)
            }
        }
    }
}


@Composable
fun LineDataSet(x0: List<Unit>, x1: String) {
    TODO("Not yet implemented")
}

@Composable
fun LineChart(x0: Context) {
    TODO("Not yet implemented")
}

@Composable
fun Entry(x0: Float, x1: Float) {
    TODO("Not yet implemented")
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