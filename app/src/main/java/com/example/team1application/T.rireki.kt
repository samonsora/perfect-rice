package com.example.team1application

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
            Log.d("GeminiApp", "💾 ローカル保存完了: ${records.size}件")
        } catch (e: Exception) {
            Log.e("GeminiApp", "💾 保存失敗: ${e.message}")
        }
    }

    fun loadRecords(): List<MealRecord> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return emptyList()
        return try {
            val records = json.decodeFromString<List<MealRecord>>(file.readText())
            Log.d("GeminiApp", "📂 ローカル読込完了: ${records.size}件")
            records
        } catch (e: Exception) {
            Log.e("GeminiApp", "📂 読込失敗: ${e.message}")
            emptyList()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dataManager = remember { MealDataManager(context) }

    // 1. データの読み込み
    var allRecords by remember { mutableStateOf(dataManager.loadRecords()) }

    // 2. 日付ごとにグループ化する（履歴表示用）
    val groupedRecords = remember(allRecords) {
        allRecords.groupBy { it.date }.toSortedMap(compareByDescending { it })
    }

    // 3. 画面のレイアウト
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("食事管理ダッシュボード") })
        }
    ) { padding ->
        // 4. 中身の表示
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            Text(
                "日別摂取カロリー推移",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )

            // 📊 グラフの表示
            MealBarChart(
                dailyCalories = allRecords.groupBy { it.date }
                    .mapValues { it.value.sumOf { r -> r.calories } }
                    .toSortedMap(),
                modifier = Modifier.height(240.dp).padding(vertical = 8.dp)
            )

            Text(
                "履歴",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // 📋 履歴リストの表示
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groupedRecords.keys.toList()) { date ->
                    DailyMealCard(
                        date = date,
                        meals = groupedRecords[date] ?: emptyList(),
                        onDeleteMeal = { id ->
                            // 削除処理
                            val newList = allRecords.filter { it.id != id }
                            allRecords = newList
                            dataManager.saveRecords(newList)
                        }
                    )
                }
            }
        }
    }
}

// --- 補助UIコンポーネント ---
@Composable
fun DailyMealCard(date: String, meals: List<MealRecord>, onDeleteMeal: (String) -> Unit) {
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
                        if (meal.score > 0) {
                            Text("スコア: ${meal.score}点 / 💡 ${meal.advice}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                    IconButton(onClick = { onDeleteMeal(meal.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
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
                axisLeft.addLimitLine(LimitLine(2500f, "男性目標").apply { lineColor = GraphColor.RED; textColor = GraphColor.RED; lineWidth = 2f })
                axisLeft.addLimitLine(LimitLine(2000f, "女性目標").apply { lineColor = GraphColor.MAGENTA; textColor = GraphColor.MAGENTA; lineWidth = 2f })
                setTouchEnabled(true)
            }
        },
        update = { chart ->
            if (entries.isNotEmpty()) {
                val dataSet = BarDataSet(entries, "合計カロリー").apply { color = GraphColor.rgb(255, 127, 80); valueTextSize = 10f }
                chart.data = BarData(dataSet).apply { barWidth = 0.6f }
                chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels.toTypedArray())
                chart.invalidate()
            } else { chart.clear() }
        }
    )
}