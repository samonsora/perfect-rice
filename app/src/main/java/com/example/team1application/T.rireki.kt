package com.example.team1application

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*
import android.graphics.Color as GraphColor
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

// --- データモデル ---
@Serializable
data class MealRecord(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val type: String,
    val menu: String,
    val calories: Int,
    val score: Int = 0,
    val advice: String = ""
)

// --- データ管理クラス ---
class MealDataManager(private val context: Context) {
    private val fileName = "meal_records_v3.json"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun saveRecords(records: List<MealRecord>) {
        try {
            val jsonString = json.encodeToString(records)
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { it.write(jsonString.toByteArray()) }
        } catch (e: Exception) {
            Log.e("GeminiApp", "💾 保存失敗: ${e.message}")
        }
    }

    fun loadRecords(): List<MealRecord> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString<List<MealRecord>>(file.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dataManager = remember { MealDataManager(context) }
    var allRecords by remember { mutableStateOf(dataManager.loadRecords()) }
    var editingMeal by remember { mutableStateOf<MealRecord?>(null) }

    val groupedRecords = remember(allRecords) {
        allRecords.groupBy { it.date }.toSortedMap(compareByDescending { it })
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("食事管理ダッシュボード") }) }
    ) { padding ->
        Column(
            modifier = modifier.padding(padding).fillMaxSize().padding(horizontal = 12.dp)
        ) {
            Text("日別摂取カロリー推移 (直近7日間)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))

            // 📊 グラフの表示（🌟 データの加工処理を追加）
            val chartData = remember(allRecords) {
                allRecords.groupBy { it.date }
                    .mapValues { it.value.sumOf { r -> r.calories } }
                    .toSortedMap()
                    .toList()
                    .takeLast(7) // 🌟 最新の7日間のみ取得
                    .toMap()
            }

            MealBarChart(
                dailyCalories = chartData,
                modifier = Modifier.height(240.dp).padding(vertical = 8.dp)
            )

            Text("履歴", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(groupedRecords.keys.toList()) { date ->
                    DailyMealCard(
                        date = date,
                        meals = groupedRecords[date] ?: emptyList(),
                        onDeleteMeal = { id ->
                            val newList = allRecords.filter { it.id != id }
                            allRecords = newList
                            dataManager.saveRecords(newList)
                        },
                        onEditMeal = { meal -> editingMeal = meal }
                    )
                }
            }
        }

        editingMeal?.let { meal ->
            EditMealDialog(
                meal = meal,
                onDismiss = { editingMeal = null },
                onConfirm = { updatedMeal ->
                    val updatedList = allRecords.map { if (it.id == updatedMeal.id) updatedMeal else it }
                    val sortedList = updatedList.sortedByDescending { it.date }
                    allRecords = sortedList
                    dataManager.saveRecords(sortedList)
                    editingMeal = null
                }
            )
        }
    }
}

@Composable
fun DailyMealCard(date: String, meals: List<MealRecord>, onDeleteMeal: (String) -> Unit, onEditMeal: (MealRecord) -> Unit) {
    val totalCal = meals.sumOf { it.calories }
    val displayDate = if (date.length >= 10) date.substring(5) else date
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = displayDate, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = "合計: $totalCal kcal", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            meals.forEach { meal ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${meal.type}: ${meal.menu}", style = MaterialTheme.typography.bodyMedium)
                        Text("${meal.calories} kcal", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    IconButton(onClick = { onEditMeal(meal) }) {
                        Icon(Icons.Default.Edit, contentDescription = "編集", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onDeleteMeal(meal.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MealBarChart(dailyCalories: Map<String, Int>, modifier: Modifier) {
    val entries = dailyCalories.values.mapIndexed { i, cal -> BarEntry(i.toFloat(), cal.toFloat()) }
    val labels = dailyCalories.keys.map { if (it.length >= 10) it.substring(5) else it }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                axisRight.isEnabled = false
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)

                // 🌟 1件の時の重複を防ぐ
                xAxis.granularity = 1f
                xAxis.isGranularityEnabled = true

                axisLeft.axisMinimum = 0f
                axisLeft.axisMaximum = 3500f
                axisLeft.addLimitLine(LimitLine(2500f, "男性目標").apply { lineColor = GraphColor.RED; textColor = GraphColor.RED; lineWidth = 2f })
                axisLeft.addLimitLine(LimitLine(2000f, "女性目標").apply { lineColor = GraphColor.MAGENTA; textColor = GraphColor.MAGENTA; lineWidth = 2f })
                setTouchEnabled(true)
            }
        },
        update = { chart ->
            if (entries.isNotEmpty()) {
                val dataSet = BarDataSet(entries, "合計カロリー").apply {
                    color = GraphColor.rgb(255, 127, 80)
                    valueTextSize = 10f
                }

                // 🌟 データ数に応じて棒の太さを調整
                chart.data = BarData(dataSet).apply { barWidth = if (entries.size == 1) 0.3f else 0.6f }
                chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels.toTypedArray())
                chart.xAxis.setLabelCount(labels.size, false)

                // 🌟 1件の時に中央に配置する調整
                if (entries.size == 1) {
                    chart.xAxis.axisMinimum = -0.5f
                    chart.xAxis.axisMaximum = 0.5f
                } else {
                    chart.xAxis.resetAxisMinimum()
                    chart.xAxis.resetAxisMaximum()
                }

                chart.invalidate()
            } else { chart.clear() }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMealDialog(meal: MealRecord, onDismiss: () -> Unit, onConfirm: (MealRecord) -> Unit) {
    var menu by remember { mutableStateOf(meal.menu) }
    var calories by remember { mutableStateOf(meal.calories.toString()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).parse(meal.date)?.time
    )
    val formattedDate = datePickerState.selectedDateMillis?.let {
        SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(Date(it))
    } ?: meal.date

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("記録の編集") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("日付: $formattedDate")
                }
                OutlinedTextField(value = menu, onValueChange = { menu = it }, label = { Text("食事内容") })
                OutlinedTextField(value = calories, onValueChange = { if (it.all { c -> c.isDigit() }) calories = it }, label = { Text("カロリー (kcal)") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(meal.copy(date = formattedDate, menu = menu, calories = calories.toIntOrNull() ?: 0)) }) { Text("更新") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("OK") } }
        ) { DatePicker(state = datePickerState) }
    }
}