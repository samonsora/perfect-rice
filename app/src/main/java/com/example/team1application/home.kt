package com.example.team1application

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// 1. 大元のホーム画面
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    // 📖 ページ設定
    val pagerState = rememberPagerState(pageCount = { 3 }, initialPage = 1)
    val scope = rememberCoroutineScope()

    // 🌙 睡眠モードか食事モードか
    var isSleepMode by remember { mutableStateOf(true) }

    // ⚙️ 設定画面を開いているかどうか
    var isSettingsOpen by remember { mutableStateOf(false) }

    // ✨ 設定画面が開いていたら、そっちを表示！
    if (isSettingsOpen) {
        SettingsScreen(onBack = { isSettingsOpen = false })
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    val leftLabel = if (isSleepMode) "睡眠記録" else "食事記録"
                    val leftIcon = if (isSleepMode) Icons.Default.Star else Icons.Default.Edit
                    val rightLabel = if (isSleepMode) "アラーム" else "食事入力"
                    val rightIcon = if (isSleepMode) Icons.Default.Notifications else Icons.Default.Restaurant

                    val items = listOfNotNull(
                        BottomNavItem(leftLabel, leftIcon, 0),
                        BottomNavItem("ホーム", Icons.Default.Home, 1),
                        BottomNavItem(rightLabel, rightIcon, 2),
                    )

                    items.forEach { item ->
                        NavigationBarItem(
                            selected = pagerState.currentPage == item.page,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(item.page) }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) { page ->
                when (page) {
                    // 👇 左画面を MealScreen
                    0 -> if (isSleepMode) {
                        RirekiScreen()
                    } else {
                        MealScreen() // 🍳 ここ食履歴完成したらコメント外し！
                    }

                    // 真ん中（ホーム）
                    1 -> HomeMainContent(
                        isSleepMode = isSleepMode,
                        onModeChange = { isSleepMode = it },
                        onSettingsClick = { isSettingsOpen = true }
                    )

                    // 右画面（アラーム / 食入力）
                    2 -> if (isSleepMode) {
                        AlarmScreen()
                    } else {
                        FoodInputScreen()
                    }

                    else -> Text("準備中...", modifier = Modifier.fillMaxSize(), color = Color.Gray)
                }
            }
        }
    }
}

// 📦 ボタン情報のデータクラス
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val page: Int
)

// --------------------------------------------------
// 🏠 ホーム画面の中身
// --------------------------------------------------
@Composable
fun HomeMainContent(
    isSleepMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    onSettingsClick: () -> Unit
) {
    var timeString by remember { mutableStateOf("00:00") }
    // 初期画像はとりあえずバナナにしておく
    var currentBgImage by remember { mutableStateOf(R.drawable.banana) }

    LaunchedEffect(isSleepMode) { // 👈 モードが変わった時も再チェックするように修正
        while (isActive) {
            val calendar = Calendar.getInstance()
            calendar.timeZone = TimeZone.getTimeZone("Asia/Tokyo")
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            formatter.timeZone = TimeZone.getTimeZone("Asia/Tokyo")
            timeString = formatter.format(calendar.time)

            val hour = calendar.get(Calendar.HOUR_OF_DAY)

            //  分岐
            if (isSleepMode) {
                // 🌙 睡眠モードの時の画像ルール（今まで通り）
                currentBgImage = when (hour) {
                    in 6..15 -> R.drawable.banana       // 朝〜昼はバナナ
                    in 16..18 -> R.drawable.tomato      // 夕方はトマト
                    else -> R.drawable.kabotyaneko      // 夜はカボチャ猫
                }
            } else {
                // 🍳 食事モードの時の画像ルール（ここを自由に設定！）
                // ※ 例として、もっと細かく時間を分けてみるね
                currentBgImage = when (hour) {
                    in 6..10 -> R.drawable.tyousyoku2       // 朝ごはん（例：パンの画像とかに変えてもOK）
                    in 11..13 -> R.drawable.hirumesi      // お昼ごはん
                    in 14..16 -> R.drawable.oyatu      // おやつタイム
                    in 17..20 -> R.drawable.banmesi      // 晩ごはん
                    else -> R.drawable.yasyoku // 夜食？
                }
            }

            delay(1000)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 背景
        Image(
            painter = painterResource(id = currentBgImage),
            contentDescription = "時間帯背景",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.8f
        )

        // コンテンツ
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 時計エリア
            Box(
                modifier = Modifier.weight(1f).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = timeString,
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 110.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black, blurRadius = 10f
                        )
                    )
                )
            }

            // ボタンエリア
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SwitchButton(text = "睡眠", isSelected = isSleepMode, onClick = { onModeChange(true) })
                    Spacer(modifier = Modifier.width(8.dp))
                    SwitchButton(text = "食事", isSelected = !isSleepMode, onClick = { onModeChange(false) })
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if(isSleepMode) "現在: 睡眠モード" else "現在: 食事モード",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(color = Color.Black, blurRadius = 5f)
                    )
                )
            }
        }

        // 設定ボタン
        FloatingActionButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(56.dp),
            containerColor = Color.White.copy(alpha = 0.9f),
            contentColor = Color.Black
        ) {
            Icon(Icons.Default.Settings, contentDescription = "設定画面へ")
        }
    }
}

@Composable
fun SwitchButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF6200EE) else Color.Transparent,
            contentColor = if (isSelected) Color.White else Color.Black
        ),
        elevation = if(isSelected) ButtonDefaults.buttonElevation(defaultElevation = 6.dp) else ButtonDefaults.buttonElevation(0.dp),
        shape = RoundedCornerShape(50)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}
