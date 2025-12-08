package com.example.team1application

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
//import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
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
    // 📖 ページ設定
    val pagerState = rememberPagerState(pageCount = { 5 }, initialPage = 2)
    val scope = rememberCoroutineScope()

    // ✨✨ データ管理コードは全部消しました！スッキリ！ ✨✨

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                val items = listOfNotNull(
                    // BottomNavItem("食記録", Icons.Default.Restaurant, 0),
                    BottomNavItem("睡眠記録", Icons.Default.Star, 1),
                    BottomNavItem("ホーム", Icons.Default.Home, 2),
                    BottomNavItem("アラーム", Icons.Default.Notifications, 3),
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
                .padding(innerPadding)
        ) { page ->
            when (page) {
                1 -> RirekiScreen()
                2 -> HomeMainContent()
                3 -> AlarmScreen() // 👈 引数なしで呼べるようになりました！
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
// 🏠 ホーム画面の中身（時計とボタンだけ）
// --------------------------------------------------
@Composable
fun HomeMainContent() {
    var timeString by remember { mutableStateOf("00:00") }

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

        // 🟦 下のエリア（ボタン）
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

        }
    }
}