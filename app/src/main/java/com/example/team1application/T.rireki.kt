package com.example.team1application

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.text.SimpleDateFormat
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

// --- Gemini管理クラス ---
class GeminiManager(apiKey: String) {
    // モデル名は最も標準的な "gemini-2.5-flash" に固定
    private val generativeModel = GenerativeModel(modelName ="gemini-2.5-flash", apiKey = apiKey)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun analyzeMealImage(bitmap: Bitmap): MealRecord? {
        Log.d("GeminiApp", "========================================")
        Log.d("GeminiApp", "🚀 Gemini解析フェーズ1: リクエスト準備")
        Log.d("GeminiApp", "📸 画像サイズ: ${bitmap.width} x ${bitmap.height}")

        val promptText = "この食事画像を分析して、以下の情報を日本語のJSON形式で返してください。" +
                "{\"menu\": \"料理名\", \"calories\": 500, \"score\": 80, \"advice\": \"アドバイス\"}" +
                "出力はJSONフォーマットのみにしてください。"

        val prompt = content {
            text(promptText)
            image(bitmap)
        }

        return try {
            Log.d("GeminiApp", "📡 Geminiサーバーへ送信中...")
            val response = generativeModel.generateContent(prompt)

            // 👈 ここで「生の応答」を全力でログ出しします
            val rawText = response.text
            Log.d("GeminiApp", "📥 Gemini解析フェーズ2: 受信成功")
            Log.d("GeminiApp", "📝 受信データ(生): $rawText")

            if (rawText.isNullOrBlank()) {
                Log.e("GeminiApp", "❌ エラー: 応答テキストが空です")
                return null
            }

            // JSON部分だけを抽出
            val jsonText = rawText.replace("```json", "").replace("```", "").trim()

            Log.d("GeminiApp", "⚙️ 解析フェーズ3: JSONパース開始")
            val element = json.parseToJsonElement(jsonText).jsonObject

            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
            val record = MealRecord(
                date = sdf.format(Date()),
                type = "AI解析",
                menu = element["menu"]?.jsonPrimitive?.content ?: "不明",
                calories = element["calories"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                score = element["score"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                advice = element["advice"]?.jsonPrimitive?.content ?: ""
            )

            Log.i("GeminiApp", "✅ 全工程完了！料理名: ${record.menu}")
            Log.d("GeminiApp", "========================================")
            record

        } catch (e: Exception) {
            Log.e("GeminiApp", "========================================")
            Log.e("GeminiApp", "❌ 重大なエラーが発生しました")
            Log.e("GeminiApp", "理由: ${e.message}")
            e.printStackTrace() // 詳細なスタックトレースを出力
            Log.e("GeminiApp", "========================================")
            null
        }
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

// --- メイン画面 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataManager = remember { MealDataManager(context) }
    var allRecords by remember { mutableStateOf(dataManager.loadRecords()) }
    var isAnalyzing by remember { mutableStateOf(false) }

    val geminiManager = remember {
        val apiKey = BuildConfig.GEMINI_API_KEY

        // 🔍 診断用ログ：キーがどう見えているか徹底調査
        Log.e("GeminiApp", "=== APIキー診断開始 ===")
        Log.e("GeminiApp", "キーの長さ: ${apiKey.length}")
        Log.e("GeminiApp", "先頭3文字: ${apiKey.take(3)}")
        Log.e("GeminiApp", "末尾3文字: ${apiKey.takeLast(3)}")
        Log.e("GeminiApp", "途中に空白があるか: ${apiKey.contains(" ")}")
        Log.e("GeminiApp", "引用符が含まれているか: ${apiKey.contains("\"")}")
        Log.e("GeminiApp", "=== APIキー診断終了 ===")

        GeminiManager(apiKey)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Log.d("GeminiApp", "📸 ギャラリーから画像を選択: $uri")
            isAnalyzing = true
            scope.launch {
                try {
                    val bitmap = context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                    if (bitmap != null) {
                        val result = geminiManager.analyzeMealImage(bitmap)
                        if (result != null) {
                            val newList = (allRecords + result).sortedByDescending { it.date }
                            allRecords = newList
                            dataManager.saveRecords(newList)
                        }
                    } else {
                        Log.e("GeminiApp", "🖼️ Bitmapの変換に失敗しました")
                    }
                } catch (e: Exception) {
                    Log.e("GeminiApp", "🚨 予期せぬエラー: ${e.message}")
                } finally {
                    isAnalyzing = false
                }
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
                        DailyMealCard(date = date, meals = groupedRecords[date] ?: emptyList(), onDeleteMeal = { id ->
                            val newList = allRecords.filter { it.id != id }
                            allRecords = newList
                            dataManager.saveRecords(newList)
                        })
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

// --- 補助UIコンポーネント ---
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