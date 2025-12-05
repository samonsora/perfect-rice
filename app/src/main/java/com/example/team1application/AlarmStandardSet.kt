package com.example.team1application

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.math.abs

// --- 定数とヘルパー関数 (TimePickerWheelに必要なもの) ---

private val ITEM_HEIGHT_DP = 48.dp
private const val LIST_COUNT = 1000
private const val MAX_SCALE_FACTOR = 1.3f
private const val MIN_ALPHA = 0.4f
private const val INITIAL_OFFSET = LIST_COUNT / 2

/**
 * 減速係数を調整したカスタム FlingBehavior
 *
 * @param decayAnimationSpec 慣性スクロールの減衰特性を定義するスペック。
 * @param decelerationFactor 減速の強さを制御する係数。値が大きいほど減速が速くなる。
 */
private class SlowDownFlingBehavior(
    private val decayAnimationSpec: DecayAnimationSpec<Float>,
    private val decelerationFactor: Float
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        // initialVelocity が小さすぎる場合はフリングしない
        if (abs(initialVelocity) < 1f) return initialVelocity

        // 減速をより速くするために initialVelocity を「小さく」する効果を与える
        // velocity に decelerationFactor を適用し、実質的なフリング距離を短縮する
        val effectiveVelocity = initialVelocity / decelerationFactor

        // AnimationState を初期化
        val animationState = AnimationState(
            initialValue = 0f,
            initialVelocity = effectiveVelocity
        )

        var lastValue = 0f

        // アニメーションを実行
        // 修正点: AnimationState の animateDecay 拡張関数を呼び出し、引数を修正
        animationState.animateDecay(
            animationSpec = decayAnimationSpec
        ) {
            // スクロール距離を計算 (現在の値 - 前回の値)
            val delta = value - lastValue

            // スクロールを適用
            val consumed = scrollBy(delta)

            // 消費されなかった場合は停止
            if (abs(delta - consumed) > 0.5f) {
                // 修正点: AnimationScope の cancelAnimation を呼び出す
                this.cancelAnimation()
            }

            lastValue = value
        }

        // 最終的な残りの速度を返す (減速係数を元に戻す)
        return animationState.velocity * decelerationFactor
    }
}

/**
 * カスタムの SlowDownFlingBehavior を覚える関数
 */
@Composable
private fun rememberSlowDownFlingBehavior(): FlingBehavior {
    // スプラインベースの減衰アニメーションを使用
    val decay = splineBasedDecay<Float>(LocalDensity.current)
    // 減速係数: 値が大きいほど、フリングが早く停止する（慣性が弱い）
    // デフォルトは 1.0f 相当。ここでは 5.0f に設定し、より強く減速させる。
    val decelerationFactor = 5.0f

    return remember(decay, decelerationFactor) {
        SlowDownFlingBehavior(decay, decelerationFactor)
    }
}

/**
 * 汎用的な時間/分ホイールピッカーコンポーネント
 */
@Composable
fun TimePickerWheel(
    currentValue: Int,
    rangeSize: Int,
    itemHeight: Dp,
    onValueChange: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }

    val listState = rememberLazyListState()
    val slowDownFlingBehavior = rememberSlowDownFlingBehavior() // 👈 変更点：カスタム FlingBehavior を使用

    var isInitialScrollComplete by remember { mutableStateOf(false) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // --- 1. 初期スクロールロジック ---
    LaunchedEffect(currentValue, containerSize.height) {
        val targetIndex = INITIAL_OFFSET * rangeSize + currentValue
        if (containerSize.height > 0) {
            coroutineScope.launch {
                val offsetToCenter = (containerSize.height / 2f - itemHeightPx / 2f).toInt()
                listState.scrollToItem(targetIndex, -offsetToCenter)
                isInitialScrollComplete = true
            }
        }
    }

    // --- 2. スナップバックロジック ---
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && isInitialScrollComplete) {
            val containerCenterY = containerSize.height / 2f

            // 修正点: minByOrNull のラムダ内での型推論の問題を回避するため、item?.let { } を使用するか、
            // 処理が複雑な場合は minByOrNull ではなく minBy の使用を検討する
            // 現在のコードでは問題なく動作するはずだが、念のため安全な呼び出しを使用
            val closestItem = listState.layoutInfo.visibleItemsInfo.minByOrNull { item ->
                val itemCenterY = item.offset + item.size / 2f
                abs(itemCenterY - containerCenterY)
            } ?: return@LaunchedEffect

            val newValue = closestItem.index % rangeSize

            if (newValue != currentValue) {
                onValueChange(newValue)
            }
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = slowDownFlingBehavior, // 👈 変更点：カスタム FlingBehavior を適用
        modifier = Modifier
            .height(itemHeight * 3f)
            .width(40.dp)
            .onSizeChanged { containerSize = it },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        items(LIST_COUNT * rangeSize) { index: Int ->
            val value = index % rangeSize

            Text(
                text = value.toString().padStart(2, '0'),
                fontSize = 32.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .height(itemHeight)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .graphicsLayer {
                        val layoutInfo = listState.layoutInfo
                        val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }
                        val itemHeightPxLocal = itemHeightPx
                        val containerCenterY = containerSize.height / 2f

                        val centerFactor = if (itemInfo != null && containerSize.height > 0) {
                            val itemCenterY = itemInfo.offset + itemInfo.size / 2f
                            val distance = abs(itemCenterY - containerCenterY)
                            val maxDistance = containerCenterY + itemHeightPxLocal / 2f

                            1f - (distance / maxDistance).coerceIn(0f, 1f)
                        } else 0f

                        this.scaleX = 1f + (MAX_SCALE_FACTOR - 1f) * centerFactor
                        this.scaleY = this.scaleX
                        this.alpha = MIN_ALPHA + (1f - MIN_ALPHA) * centerFactor
                    }
                    .clickable {
                        coroutineScope.launch {
                            val offsetToCenter = (containerSize.height / 2f - itemHeightPx / 2f).toInt()
                            listState.animateScrollToItem(index, -offsetToCenter)
                            onValueChange(value)
                        }
                    }
            )
        }
    }
}

//---

/**
 * アラーム時刻のピッカーUIコンポーネント (TimePickerWheelを利用)
 */
@Composable
fun AlarmTimePicker(
    timeInput: TextFieldValue,
    timeFormat: SimpleDateFormat,
    onTimeChange: (TextFieldValue) -> Unit
) {
    val calendar = remember(timeInput.text) {
        val targetTimeZone = timeFormat.timeZone
        val cal = Calendar.getInstance(targetTimeZone)
        try {
            cal.time = timeFormat.parse(timeInput.text) ?: cal.time
        } catch (_: Exception) {}
        cal
    }

    // 時刻のフィールドを直接設定するヘルパー関数 (ホイールピッカー用)
    val updateTime: (Int, Int) -> Unit = { field, newValue ->
        calendar.set(field, newValue)
        val newTimeString = timeFormat.format(calendar.time)
        onTimeChange(
            TextFieldValue(newTimeString, selection = TextRange(newTimeString.length))
        )
    }

    // Calendarの値を操作し、結果をtimeInputステートに反映するヘルパー関数 (加減算用)
    val addTime: (Int, Int) -> Unit = { field, amount ->
        calendar.add(field, amount)
        val newTimeString = timeFormat.format(calendar.time)
        onTimeChange(
            TextFieldValue(newTimeString, selection = TextRange(newTimeString.length))
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        // ピッカーとボタンの行 (全体)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 時のホイールピッカー
            TimePickerWheel(
                currentValue = calendar.get(Calendar.HOUR_OF_DAY),
                rangeSize = 24,
                itemHeight = ITEM_HEIGHT_DP,
                onValueChange = { newHour ->
                    updateTime(Calendar.HOUR_OF_DAY, newHour)
                }
            )

            Text(" : ", fontSize = 32.sp, modifier = Modifier.padding(horizontal = 8.dp))

            // 分のホイールピッカー
            TimePickerWheel(
                currentValue = calendar.get(Calendar.MINUTE),
                rangeSize = 60,
                itemHeight = ITEM_HEIGHT_DP,
                onValueChange = { newMinute ->
                    updateTime(Calendar.MINUTE, newMinute)
                }
            )

            // ピッカーとボタンの間のスペース
            Spacer(Modifier.width(6.dp))

            // カスタム操作ボタンの縦4列 Column
            Column {
                // ボタンサイズを固定 (80dp x 40dp) に設定
                val buttonModifier = Modifier.size(width = 80.dp, height = 40.dp)

                OutlinedButton(
                    onClick = { addTime(Calendar.MINUTE, 10) },
                    modifier = buttonModifier
                ) {
                    Text("+10分", fontSize = 14.sp)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { addTime(Calendar.MINUTE, 5) },
                    modifier = buttonModifier
                ) {
                    Text("+5分", fontSize = 14.sp)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { addTime(Calendar.MINUTE, -5) },
                    modifier = buttonModifier
                ) {
                    Text("-5分", fontSize = 14.sp)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { addTime(Calendar.MINUTE, -10) },
                    modifier = buttonModifier
                ) {
                    Text("-10分", fontSize = 14.sp)
                }
            }
        }
    }
}