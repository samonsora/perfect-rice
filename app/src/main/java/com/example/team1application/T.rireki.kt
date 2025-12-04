package com.example.team1application

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

//@Composable
//fun RecordCard(record: SleepRecord, modifier: Modifier = Modifier) {
//    Card(modifier = modifier) {
//        Column(modifier = Modifier.padding(16.dp)) {
//            Text(text = " ${record.date}", style = MaterialTheme.typography.titleMedium)
//            Spacer(Modifier.height(4.dp))
//            Text(text = "朝食時間: ${record.sleepTime}", style = MaterialTheme.typography.bodyLarge)
//            Text(text = "昼食時間: ${record.bedtime}")
//            Text(text = "夕食時間: ${record.wakeUpTime}")
//            Text(text = "間食時間: ${record.wakeUpTime}")
//            Text(text = "夜食時間: ${record.wakeUpTime}")
//            Text(text = "スヌーズ回数: ${record.snoozeCount}回")
//            Text(text = "スヌーズ合計時間: ${record.snoozeDuration}")
//        }
//    }
//}