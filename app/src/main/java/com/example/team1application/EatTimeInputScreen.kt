package com.example.team1application

import android.Manifest
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EatTimeInputScreen(
    onSave: (String, String, Int, Int) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current

    // 食事区分
    var mealType by remember { mutableStateOf("朝ごはん") }
    val mealTypes = listOf("朝ごはん", "昼ごはん", "晩ごはん")

    var mealName by remember { mutableStateOf("") }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var hour by remember { mutableStateOf(12) }
    var minute by remember { mutableStateOf(0) }
    var showPicker by remember { mutableStateOf(false) }

    // カメラ
    val cameraLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicturePreview()
        ) { bitmap ->
            photoBitmap = bitmap
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) cameraLauncher.launch(null)
        }

    // ギャラリー
    val galleryLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(
                    context.contentResolver,
                    it
                )
                photoBitmap = bitmap
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text("食事の記録", style = MaterialTheme.typography.headlineSmall)

        // 食事区分（ボタン選択）
        Text("食事区分", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            mealTypes.forEach { type ->
                Button(
                    onClick = { mealType = type },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            if (mealType == type)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                        contentColor =
                            if (mealType == type)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(type)
                }
            }
        }

        // 写真表示
        photoBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "食事の写真",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }

        // 食事内容
        OutlinedTextField(
            value = mealName,
            onValueChange = { mealName = it },
            label = { Text("食事内容") },
            modifier = Modifier.fillMaxWidth()
        )

        // 時刻
        OutlinedButton(
            onClick = { showPicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("時刻：%02d:%02d".format(hour, minute))
        }

        // 写真撮影
        Button(
            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("写真を撮る")
        }

        // ギャラリー
        Button(
            onClick = { galleryLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ギャラリーから選ぶ")
        }

        // 保存
        Button(
            onClick = {
                onSave(mealType, mealName, hour, minute)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存")
        }
    }

    // ===== 時刻選択ダイアログ =====
    if (showPicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    hour = timePickerState.hour
                    minute = timePickerState.minute
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("キャンセル") }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}
