package com.example.team1application

import android.content.Context
// グラフ用とUI用のColorが競合しないように別名で扱うか、明示的に指定します
import android.graphics.Color as GraphColor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Compose UI用のColor
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class MealRecord(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val type: String,
    val menu: String,
    val calories: Int
)

class MealDataManager(private val context: Context) {
    private val fileName = "meal_records_v3.json"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun saveRecords(records: List<MealRecord>) {
        try {
            val jsonString = json.encodeToString(records)
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { it.write(jsonString.toByteArray()) }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun loadRecords(): List<MealRecord> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return createDemoData()
        return try {
            json.decodeFromString<List<MealRecord>>(file.readText())
        } catch (e: Exception) { createDemoData() }
    }

    private fun createDemoData(): List<MealRecord> {
        val demoList = mutableListOf<MealRecord>()
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
        for (i in 0 until 7) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = sdf.format(calendar.time)
            demoList.add(MealRecord(date = dateStr, type = "朝食", menu = "サラダ", calories = 150 + (0..50).random()))
            demoList.add(MealRecord(date = dateStr, type = "昼食", menu = "うどん", calories = 420 + (0..50).random()))
            demoList.add(MealRecord(date = dateStr, type = "夕食", menu = "牛丼", calories = 750 + (0..50).random()))
        }
        val sortedData = demoList.sortedBy { it.date }
        saveRecords(sortedData)
        return sortedData
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dataManager = remember { MealDataManager(context) }
    var allRecords by remember { mutableStateOf(dataManager.loadRecords()) }

    val groupedRecords = remember(allRecords) {
        allRecords.groupBy { it.date }.toSortedMap(compareByDescending { it })
    }

    val dailyCalories = remember(allRecords) {
        allRecords.groupBy { it.date }
            .mapValues { entry -> entry.value.sumOf { it.calories } }
            .toSortedMap()
    }

    Scaffold { padding ->
        Column(modifier = modifier.padding(padding).padding(horizontal = 12.dp).fillMaxSize()) {
            Text("日別摂取カロリー推移", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))

            MealBarChart(
                dailyCalories = dailyCalories,
                modifier = Modifier.height(260.dp).padding(vertical = 8.dp)
            )

            Text("履歴 (1日ごと)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(groupedRecords.keys.toList()) { date ->
                    val mealsOfDay = groupedRecords[date] ?: emptyList()
                    DailyMealCard(
                        date = date,
                        meals = mealsOfDay,
                        onDeleteMeal = { mealId ->
                            val newList = allRecords.filter { it.id != mealId }
                            allRecords = newList
                            dataManager.saveRecords(newList)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DailyMealCard(date: String, meals: List<MealRecord>, onDeleteMeal: (String) -> Unit) {
    val totalCal = meals.sumOf { it.calories }
    val displayDate = if (date.length >= 10) date.substring(5) else date

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = displayDate, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = "合計: ${totalCal} kcal",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            meals.forEach { meal ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "${meal.type}: ${meal.menu}", style = MaterialTheme.typography.bodyMedium)
                        // ここでColor.Grayを使用
                        Text(text = "${meal.calories} kcal", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    IconButton(onClick = { onDeleteMeal(meal.id) }, modifier = Modifier.size(24.dp)) {
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
                axisLeft.axisMinimum = 0f
                axisLeft.axisMaximum = 3500f
                // GraphColor (android.graphics.Color) を使用
                axisLeft.addLimitLine(LimitLine(2500f, "男性目標").apply { lineColor = GraphColor.RED; textColor = GraphColor.RED; lineWidth = 2f })
                axisLeft.addLimitLine(LimitLine(2000f, "女性目標").apply { lineColor = GraphColor.MAGENTA; textColor = GraphColor.MAGENTA; lineWidth = 2f })
                setTouchEnabled(true)
            }
        },
        update = { chart ->
            if (entries.isNotEmpty()) {
                val dataSet = BarDataSet(entries, "合計カロリー").apply {
                    // GraphColor を使用
                    color = GraphColor.rgb(255, 127, 80)
                    valueTextSize = 10f
                }
                chart.data = BarData(dataSet).apply { barWidth = 0.6f }
                chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels.toTypedArray())
                chart.invalidate()
            } else { chart.clear() }
        }
    )
}