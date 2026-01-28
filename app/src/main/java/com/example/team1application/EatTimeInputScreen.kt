package com.example.team1application

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.draw.scale





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodInputScreen(
    onSaveSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val dataManager = remember { MealDataManager(context) }
    val isDark = isSystemInDarkTheme()

// 💗 選択中：ピンク
    val selectedPink = if (isDark)
        Color(0xFFF06292)   // ダーク時：少し落ち着いたピンク
    else
        Color(0xFFFF4081)   // ライト時：明るいピンク

// 💜 未選択：淡い紫
    val unselectedPurple = if (isDark)
        Color(0xFF4A3A8C)   // ダーク時：深め紫
    else
        Color(0xFFE1BEE7)   // ライト時：淡い紫
    val calendar = remember { Calendar.getInstance() }
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)

    // 時間帯に応じて初期の食事区分を決める
    val initialMealType = when (currentHour) {
        in 4..10 -> "朝ごはん"
        in 11..14 -> "昼ごはん"
        in 17..21 -> "晩ごはん"
        in 22..23, in 0..3 -> "夜食"
        else -> "間食"
    }
    var lastMealBitmap by remember { mutableStateOf<Bitmap?>(null) }



    // --- 状態管理 ---
    var hour by remember { mutableStateOf(currentHour) }
    var minute by remember { mutableStateOf(currentMinute) }
    var mealType by remember { mutableStateOf(initialMealType) }
    val mealTypes = listOf("朝ごはん", "昼ごはん", "晩ごはん", "間食", "夜食", "その他")

    var mealName by remember { mutableStateOf("") }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    // AI解析・表示制御
    var isAnalyzing by remember { mutableStateOf(false) }
    var isAnalysisDone by remember { mutableStateOf(false) }
    var aiScore by remember { mutableStateOf(0) }
    var aiAdvice by remember { mutableStateOf("") }
    var aiCalories by remember { mutableStateOf(0) }
    var isSaved by remember { mutableStateOf(false) }

    val geminiManager = remember {
        val apiKey = BuildConfig.GEMINI_API_KEY
        GeminiManager(apiKey)
    }

    // カメラ・ギャラリー
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            photoBitmap = bitmap
            isAnalysisDone = false
            isSaved = false
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) cameraLauncher.launch(null)
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            @Suppress("DEPRECATION")
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            photoBitmap = bitmap
            isAnalysisDone = false
            isSaved = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(scrollState).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("食事の記録", style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground)

        // 食事区分
        Text("食事区分", style = MaterialTheme.typography.titleMedium)
        mealTypes.chunked(3).forEach { rowTypes ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowTypes.forEach { type ->

                    // ⭐ ここに書く！
                    val scale by animateFloatAsState(
                        targetValue = if (mealType == type) 1.08f else 1.0f,
                        animationSpec = tween(
                            durationMillis = 200,
                            easing = FastOutSlowInEasing
                        ),
                        label = "mealButtonScale"
                    )

                    Button(
                        onClick = { mealType = type },
                        modifier = Modifier
                            .weight(1f)
                            .scale(scale),   // ← ここで使う
                        enabled = !isSaved,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (mealType == type)
                                selectedPink
                            else
                                unselectedPurple,
                            contentColor = Color.White
                        )
                    ) {
                        Text(type, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }

        // 写真表示
        photoBitmap?.let { bitmap ->
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
                if (!isSaved) {
                    IconButton(
                        onClick = { photoBitmap = null; isAnalysisDone = false },
                        modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f))
                    ) { Icon(Icons.Default.Close, contentDescription = "削除", tint = Color.White) }
                }
            }
        }
        if (photoBitmap != null && !isAnalysisDone) {
            Button(
                onClick = {
                    isAnalyzing = true
                    scope.launch {
                        val result = geminiManager.analyzeMealImage(photoBitmap!!)
                        if (result != null) {
                            mealName = result.menu
                            aiScore = result.score
                            aiAdvice = result.advice
                            aiCalories = result.calories
                            isAnalysisDone = true
                        }
                        isAnalyzing = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isAnalyzing,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Text(" 解析中...")
                } else {
                    Text("AIに料理を判定してもらう ✨")
                }
            }
        }

        // AI解析結果表示
        if (isAnalysisDone) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("【AI評価】 $aiScore 点 / 推定 $aiCalories kcal", style = MaterialTheme.typography.titleSmall)
                    Text("アドバイス: $aiAdvice", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        OutlinedTextField(
            value = mealName,
            onValueChange = { if (!isSaved) mealName = it },
            label = { Text("食事内容") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaved
        )
        OutlinedButton(
            onClick = { showPicker = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaved
        ) {
            Text("時刻：%02d:%02d".format(hour, minute))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.weight(1f), enabled = !isSaved) {
                Text("カメラ 📷")
            }
            Button(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f), enabled = !isSaved) {
                Text("ギャラリー 🖼️")
            }
        }

        // 保存ボタン
        if (isAnalysisDone) {
            Button(
                onClick = {
                    if (isSaved) return@Button
                    isSaved = true

                    lastMealBitmap = photoBitmap
                    val newRecord = MealRecord(
                        date = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(Date()),
                        type = mealType,
                        menu = mealName,
                        calories = aiCalories,
                        score = aiScore,
                        advice = aiAdvice
                    )
                    dataManager.saveRecords((dataManager.loadRecords() + newRecord).sortedByDescending { it.date })
                    onSaveSuccess()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaved,
                colors = ButtonDefaults.buttonColors(containerColor = if (isSaved) Color.Gray else MaterialTheme.colorScheme.tertiary)
            ) {
                Text(if (isSaved) "保存完了 ✅" else "この内容で保存する")
            }
        }
        if (!isAnalysisDone) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📷 写真があるとAIの精度が上がります ✨",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }

    // 時刻選択ダイアログ
    if (showPicker) {
        val timePickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    hour = timePickerState.hour
                    minute = timePickerState.minute
                    showPicker = false
                }) { Text("OK") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

// --- GeminiManager ---
class GeminiManager(apiKey: String) {
    private val generativeModel = GenerativeModel(modelName = "gemini-2.5-flash", apiKey = apiKey)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun analyzeMealImage(bitmap: Bitmap): MealRecord? {
        val promptText = """
            あなたはプロの栄養管理士です。
            この食事画像を分析して、以下の情報を日本語のJSON形式で返してください。
            スコアの基準は栄養素バランスと健康度に指定
            {"menu": "料理名", "calories": "数字", "score": "1 ~ 10", "advice": "アドバイス"}
            注意：純粋なJSONのみ出力してください。
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(content { text(promptText); image(bitmap) })
            val rawText = response.text ?: return null
            val start = rawText.indexOf("{"); val end = rawText.lastIndexOf("}")
            if (start == -1 || end == -1) return null
            val element = json.parseToJsonElement(rawText.substring(start, end + 1)).jsonObject

            MealRecord(
                date = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(Date()),
                type = "AI解析",
                menu = element["menu"]?.jsonPrimitive?.content ?: "不明",
                calories = element["calories"]?.jsonPrimitive?.content?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0,
                score = element["score"]?.jsonPrimitive?.content?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0,
                advice = element["advice"]?.jsonPrimitive?.content ?: ""
            )
        } catch (e: Exception) {
            Log.e("GeminiManager", "Error", e)
            null
        }


    }
}

