package com.example.team1application

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as GraphColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
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
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// --- データモデル ---
@Serializable
data class MealRecord(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val type: String,
    val menu: String,
    val calories: Int,
    val score: Int = 0,       // 栄養スコアを追加
    val advice: String = ""    // アドバイスを追加
)

// --- Gemini管理クラス ---
class GeminiManager(apiKey: String) {
    private val generativeModel = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = apiKey)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun analyzeMealImage(bitmap: Bitmap): MealRecord? {
        val prompt = content {
            text("この食事画像を分析して、以下の情報を日本語のJSON形式で返してください。" +
                    "{\"menu\": \"料理名\", \"calories\": 500, \"score\": 80, \"advice\": \"アドバイス\"}" +
                    "出力はJSONフォーマットのみにしてください。")
            image(bitmap)
        }
        return try {
            val response = generativeModel.generateContent(prompt)
            val jsonText = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: ""
            val element = json.parseToJsonElement(jsonText).jsonObject
            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
            MealRecord(
                date = sdf.format(Date()),
                type = "AI解析",
                menu = element["menu"]?.jsonPrimitive?.content ?: "不明",
                calories = element["calories"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                score = element["score"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                advice = element["advice"]?.jsonPrimitive?.content ?: ""
            )
        } catch (e: Exception) { null }
    }
}

// --- データ管理クラス ---
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
        if (!file.exists()) return emptyList() // デモデータ不要とのことなので空リスト
        return try {
            json.decodeFromString<List<MealRecord>>(file.readText())
        } catch (e: Exception) { emptyList() }
    }
}

// --- メイン画面 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataManager = remember { MealDataManager(context) }
    var allRecords by remember { mutableStateOf(dataManager.loadRecords()) }
    var isAnalyzing by remember { mutableStateOf(false) }

    // local.propertiesから読み込んだAPIキーを使用
    val geminiManager = remember { GeminiManager("AIzaSyBURteWWlyFJoz6RLBlPkLQLCTcZkhtbvM") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            isAnalyzing = true
            scope.launch {
                val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                if (bitmap != null) {
                    val result = geminiManager.analyzeMealImage(bitmap)
                    if (result != null) {
                        val newList = (allRecords + result).sortedByDescending { it.date }
                        allRecords = newList
                        dataManager.saveRecords(newList)
                    }
                }
                isAnalyzing = false
            }
        }
    }

    val groupedRecords = remember(allRecords) {
        allRecords.groupBy { it.date }.toSortedMap(compareByDescending { it })
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                text = { Text("AIで食事を解析") }
            )
        }
    ) { padding ->
        Box(modifier = modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                Text("日別摂取カロリー推移", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))

                MealBarChart(
                    dailyCalories = allRecords.groupBy { it.date }.mapValues { it.value.sumOf { r -> r.calories } }.toSortedMap(),
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
                            }
                        )
                    }
                }
            }

            if (isAnalyzing) {
                Surface(color = Color.Black.copy(alpha = 0.5f), modifier = Modifier.fillMaxSize()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = Color.White)
                        Text("Geminiが解析中...", color = Color.White, modifier = Modifier.padding(top = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DailyMealCard(date: String, meals: List<MealRecord>, onDeleteMeal: (String) -> Unit) {
    val totalCal = meals.sumOf { it.calories }
    val displayDate = if (date.length >= 10) date.substring(5) else date

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = displayDate, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = "合計: ${totalCal} kcal", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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