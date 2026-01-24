package com.example.team1application

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
import java.text.SimpleDateFormat

// --- データモデル & 管理クラス ---
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

class MealDataManager(private val context: Context) {
    private val fileName = "meal_records_v3.json"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    fun saveRecords(records: List<MealRecord>) {
        try {
            val jsonString = json.encodeToString(records)
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { it.write(jsonString.toByteArray()) }
        } catch (e: Exception) { Log.e("MealApp", "save error: ${e.message}") }
    }
    fun loadRecords(): List<MealRecord> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return emptyList()
        return try { json.decodeFromString<List<MealRecord>>(file.readText()) } catch (e: Exception) { emptyList() }
    }
}

object CalendarHelper {
    private val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
    fun getStartOfWeek(calendar: Calendar): Calendar {
        val cal = calendar.clone() as Calendar
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }
    fun formatDate(calendar: Calendar): String = sdf.format(calendar.time)
    fun getDaysOfWeek(startOfWeek: Calendar): List<String> {
        val days = mutableListOf<String>()
        val cal = startOfWeek.clone() as Calendar
        repeat(7) { days.add(formatDate(cal)); cal.add(Calendar.DAY_OF_MONTH, 1) }
        return days
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dataManager = remember { MealDataManager(context) }
    var allRecords by remember { mutableStateOf(dataManager.loadRecords()) }
    var editingMeal by remember { mutableStateOf<MealRecord?>(null) }

    var currentWeekStart by remember {
        mutableStateOf(CalendarHelper.getStartOfWeek(Calendar.getInstance(Locale.JAPAN)))
    }

    val weekDays = CalendarHelper.getDaysOfWeek(currentWeekStart)
    val weekEnd = (currentWeekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 6) }
    val weekRangeText = "${CalendarHelper.formatDate(currentWeekStart)} 〜 ${CalendarHelper.formatDate(weekEnd)}"

    val chartData = remember(allRecords, currentWeekStart) {
        val dailySum = allRecords.groupBy { it.date }.mapValues { it.value.sumOf { r -> r.calories } }
        weekDays.associateWith { date -> dailySum[date] ?: 0 }
    }

    val weeklyAverage = remember(chartData) {
        val recordedDays = chartData.values.filter { it > 0 }
        if (recordedDays.isNotEmpty()) recordedDays.average().toInt() else 0
    }

    val groupedRecords = remember(allRecords) {
        allRecords.groupBy { it.date }.toSortedMap(compareByDescending { it })
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("食事管理ダッシュボード") }) }
    ) { padding ->
        Column(
            modifier = modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)
        ) {
            // ヘッダー（影付きの奥行き案を取り入れつつシンプルに）
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("摂取カロリー推移", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                null,
                                tint = Color(0xFFFF7043),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                " 平均 $weeklyAverage kcal",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
            }

            // 📊 グラフエリアに影をつけて浮かせる
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(vertical = 8.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp)), // ここで奥行きを出す
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                MealBarChart(dailyCalories = chartData, modifier = Modifier.padding(8.dp))
            }

            // 週選択セレクター
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentWeekStart = (currentWeekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -7) } }) {
                    Icon(Icons.Default.ArrowBackIosNew, null, modifier = Modifier.size(20.dp))
                }
                Surface(
                    modifier = Modifier.weight(1f).height(40.dp).shadow(2.dp, RoundedCornerShape(20.dp)),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = weekRangeText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = { currentWeekStart = (currentWeekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7) } }) {
                    Icon(Icons.Default.ArrowForwardIos, null, modifier = Modifier.size(20.dp))
                }
            }

            Text("履歴一覧 (${allRecords.size}件)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))

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
                    allRecords = updatedList.sortedByDescending { it.date }
                    dataManager.saveRecords(allRecords)
                    editingMeal = null
                }
            )
        }
    }
}
@Composable
fun MealBarChart(dailyCalories: Map<String, Int>, modifier: Modifier) {
    val entries = dailyCalories.values.mapIndexed { i, cal -> BarEntry(i.toFloat(), cal.toFloat()) }
    val labels = dailyCalories.keys.map { if (it.length >= 10) it.substring(5) else it }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                axisRight.isEnabled = false
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.granularity = 1f
                axisLeft.axisMinimum = 0f
                axisLeft.axisMaximum = 3500f
                axisLeft.addLimitLine(LimitLine(2500f, "男性目標").apply { lineColor = GraphColor.RED; lineWidth = 1f; textColor = GraphColor.RED })
                axisLeft.addLimitLine(LimitLine(2000f, "女性目標").apply { lineColor = GraphColor.MAGENTA; lineWidth = 1f; textColor = GraphColor.MAGENTA })
                setTouchEnabled(true)
                setScaleEnabled(false)
            }
        },
        update = { chart ->
            val dataSet = BarDataSet(entries, "kcal").apply {
                color = GraphColor.rgb(255, 127, 80)
                valueTextSize = 8f
                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = if(value > 0) "${value.toInt()}" else ""
                }
            }
            chart.data = BarData(dataSet).apply { barWidth = 0.6f }
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.xAxis.setLabelCount(7, false)
            chart.invalidate()
        }
    )
}

@Composable
fun DailyMealCard(date: String, meals: List<MealRecord>, onDeleteMeal: (String) -> Unit, onEditMeal: (MealRecord) -> Unit) {
    val totalCal = meals.sumOf { it.calories }
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = date, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "合計: $totalCal kcal", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
            meals.forEach { meal ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${meal.type}: ${meal.menu}", style = MaterialTheme.typography.bodyMedium)
                        Text("${meal.calories} kcal", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    IconButton(onClick = { onEditMeal(meal) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { onDeleteMeal(meal.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMealDialog(meal: MealRecord, onDismiss: () -> Unit, onConfirm: (MealRecord) -> Unit) {
    var menu by remember { mutableStateOf(meal.menu) }
    var calories by remember { mutableStateOf(meal.calories.toString()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).parse(meal.date)?.time)
    val formattedDate = datePickerState.selectedDateMillis?.let { SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(Date(it)) } ?: meal.date
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("記録の編集") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) { Text("日付: $formattedDate") }
                OutlinedTextField(value = menu, onValueChange = { menu = it }, label = { Text("食事内容") })
                OutlinedTextField(value = calories, onValueChange = { if (it.all { c -> c.isDigit() }) calories = it }, label = { Text("カロリー (kcal)") })
            }
        },
        confirmButton = { Button(onClick = { onConfirm(meal.copy(date = formattedDate, menu = menu, calories = calories.toIntOrNull() ?: 0)) }, enabled = menu.isNotBlank() && calories.isNotEmpty()) { Text("更新") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
    )
    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("OK") } }) { DatePicker(state = datePickerState) }
    }
}