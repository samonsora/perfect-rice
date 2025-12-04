package com.example.team1application

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
//import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold // ← 足場の魔法
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// 1. 大元のホーム画面
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    // 📖 ページは5枚構成（0:食, 1:睡眠, 2:ホーム, 3:アラーム, 4:設定）
    // 初期表示は真ん中の「2:ホーム」
    val pagerState = rememberPagerState(pageCount = { 5 }, initialPage = 2)
    val scope = rememberCoroutineScope() // ページ移動用

    // 🔑 アラームデータの管理
    val allAlarms = remember { getDummyAlarmSettings() }

    // 🔄 データ更新
    val onToggleActive: (Int, Boolean) -> Unit = { alarmId, newState ->
        val index = allAlarms.indexOfFirst { it.id == alarmId }
        if (index != -1) {
            val oldAlarm = allAlarms[index]
            allAlarms[index] = oldAlarm.copy(isActive = newState)
        }
    }

    // 🕰️ 時間計算（ユーザー様のロジック：+0時間）
    val targetTimeDisplay = remember(allAlarms.toList()) {
        val activeAlarm = allAlarms.firstOrNull { it.isActive }
        if (activeAlarm != null) {
            calculate12HoursLater(activeAlarm.time)
        } else {
            "--:--"
        }
    }

    // 🏗️ Scaffoldで画面の「下（bottomBar）」を作るよ！
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            // ✨✨ ナビゲーションバー ✨✨
            NavigationBar {
                // ボタンのリスト
                val items = listOfNotNull(
                    // 0: 食記録（コメントアウト中）
                    // BottomNavItem("食記録", Icons.Default.Restaurant, 0),

                    // 1: 睡眠記録
                    BottomNavItem("睡眠記録", Icons.Default.Star, 1),

                    // 2: ホーム
                    BottomNavItem("ホーム", Icons.Default.Home, 2),

                    // 3: アラーム
                    BottomNavItem("アラーム", Icons.Default.Notifications, 3),

                    // 4: 設定（コメントアウト中）
                    // BottomNavItem("設定", Icons.Default.Settings, 4)
                )

                items.forEach { item ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == item.page,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(item.page)
                            }
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
                .padding(innerPadding) // ナビバーに隠れないように隙間を空ける
        ) { page ->
            when (page) {
                // 0 -> 食記録画面... (コメントアウト中)
                1 -> RirekiScreen()
                2 -> HomeMainContent(targetTime = targetTimeDisplay)
                3 -> AlarmScreen(
                    alarms = allAlarms,
                    onToggleActive = onToggleActive
                )
                // 4 -> ClockScreen() (コメントアウト中)
                else -> Text("準備中...")
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
fun HomeMainContent(targetTime: String) {
    var timeString by remember { mutableStateOf("00:00") }

    // 🎨 プレートの色
    val plateColor = Color(0xFFCFD8DC)

    LaunchedEffect(Unit) {
        while (isActive) {
            val calendar = Calendar.getInstance()
            calendar.timeZone = TimeZone.getTimeZone("Asia/Tokyo")
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            formatter.timeZone = TimeZone.getTimeZone("Asia/Tokyo")
            timeString = formatter.format(calendar.time)
            delay(1000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 🟥 上のエリア（時計）
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = timeString,
                fontSize = 120.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray,
                lineHeight = 110.sp
            )
        }

        // 🟦 下のエリア
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            // ✨✨ 魔法のプレート ✨✨
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(plateColor)
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "次のアラーム時間",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black
                )
                Text(
                    text = targetTime,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 🔘 ボタン
            Button(
                onClick = { /* 動作 */ },
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                Text("記録")
            }

            Spacer(modifier = Modifier.weight(0.5f))
        }
    }
}

// 🧙‍♀️ 計算用の魔法（+0時間）
fun calculate12HoursLater(originalTime: String): String {
    return try {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = formatter.parse(originalTime) ?: return "--:--"
        val calendar = Calendar.getInstance()
        calendar.time = date
        // ユーザー様の変更通り、12ではなく0を加算（アラーム時刻そのものを表示）
        calendar.add(Calendar.HOUR_OF_DAY, 0)
        formatter.format(calendar.time)
    } catch (e: Exception) {
        "--:--"
    }
}