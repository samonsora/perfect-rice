package com.example.team1application

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.plus
import kotlin.collections.sortedByDescending
import kotlin.text.replace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodInputScreen(
    // 保存ボタンが押された時に、すべての情報を渡せるように引数を増やします
    onSave: (MealRecord) -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope() // AIを動かすための魔法の杖

    // --- 入力状態の管理 ---
    var mealType by remember { mutableStateOf("朝ごはん") }
    val mealTypes = listOf("朝ごはん", "昼ごはん", "晩ごはん")
    var mealName by remember { mutableStateOf("") }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var hour by remember { mutableStateOf(12) }
    var minute by remember { mutableStateOf(0) }
    var showPicker by remember { mutableStateOf(false) }

    // --- AI解析用の状態 ---
    var isAnalyzing by remember { mutableStateOf(false) }
    var aiScore by remember { mutableStateOf(0) }
    var aiAdvice by remember { mutableStateOf("") }

    // AIの準備 (モデル名は1.5-flashに修正)
    val geminiManager = remember {
        val apiKey = BuildConfig.GEMINI_API_KEY
        GeminiManager(apiKey)
    }

    // --- カメラ・ギャラリーの設定 ---
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) {
        photoBitmap = it
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) cameraLauncher.launch(null)
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            photoBitmap = bitmap
        }
    }

    // --- 画面レイアウト ---
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).verticalScroll(scrollState).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("食事の記録", style = MaterialTheme.typography.headlineSmall)

        // 食事区分（朝・昼・晩）
        Text("食事区分", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            mealTypes.forEach { type ->
                Button(
                    onClick = { mealType = type },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (mealType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) { Text(type, maxLines = 1) }
            }
        }

        // 写真表示
        photoBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
        }

        if (photoBitmap != null) {
            Button(
                onClick = {
                    /* 一時的にコメントアウトして何もしないようにします
                    isAnalyzing = true
                    scope.launch {
                        val result = geminiManager.analyzeMealImage(photoBitmap!!)
                        if (result != null) {
                            mealName = result.menu
                            aiScore = result.score
                            aiAdvice = result.advice
                        }
                        isAnalyzing = false
                    }
                    */
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = true, // ボタンは押せる状態にしておきます
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                // ボタンの表示もシンプルにしておきます
                Text("AIに料理を判定してもらう（停止中） ✨")
            }
        }

        // AIの答えを表示するエリア
        if (aiScore > 0) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))) {
                Text("【AI評価】 $aiScore 点\nアドバイス: $aiAdvice", modifier = Modifier.padding(12.dp))
            }
        }

        // 食事内容の入力欄
        OutlinedTextField(
            value = mealName,
            onValueChange = { mealName = it },
            label = { Text("食事内容（AIが入力します）") },
            modifier = Modifier.fillMaxWidth()
        )

        // 時刻設定ボタン
        OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text("時刻：%02d:%02d".format(hour, minute))
        }

        // 写真準備ボタン
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.weight(1f)) {
                Text("カメラ 📷")
            }
            Button(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                Text("ギャラリー 🖼️")
            }
        }

        // 保存ボタン
        Button(
            onClick = {
                val record = MealRecord(
                    date = SimpleDateFormat("yyyy/MM/dd").format(Date()),
                    type = mealType,
                    menu = mealName,
                    calories = 0, // 必要ならAI結果から取得
                    score = aiScore,
                    advice = aiAdvice
                )
                onSave(record)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Text("この内容で保存する")
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // (時刻選択ダイアログはそのまま)
    if (showPicker) {
        val timePickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = { TextButton(onClick = { hour = timePickerState.hour; minute = timePickerState.minute; showPicker = false }) { Text("OK") } },
            text = { TimePicker(state = timePickerState) }
        )
    }
}
class GeminiManager(apiKey: String) {
    private val generativeModel = GenerativeModel(modelName ="gemini-2.5-flash", apiKey = apiKey)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun analyzeMealImage(bitmap: Bitmap): MealRecord? {
        val promptText = """
    この食事画像を分析して、以下の情報を日本語のJSON形式で返してください。
    {
      "menu": "料理名",
      "calories": 500,
      "score": 80,
      "advice": "アドバイス"
    }
    出力はJSONフォーマットのみにしてください。
""".trimIndent()

        val prompt = content {
            text(promptText)
            image(bitmap)
        }

        return try {
            val response = generativeModel.generateContent(prompt)
            val rawText = response.text ?: return null
            val jsonText = rawText.replace("```json", "").replace("```", "").trim()
            val element = json.parseToJsonElement(jsonText).jsonObject

            MealRecord(
                date = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(Date()),
                type = "AI解析",
                menu = element["menu"]?.jsonPrimitive?.content ?: "不明",
                calories = element["calories"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                score = element["score"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                advice = element["advice"]?.jsonPrimitive?.content ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
}