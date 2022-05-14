package io.github.takusan23.zeromirror.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.*

/**
 * 今の時刻を表示しているタイトルの部分
 *
 * いま何時！そうねだいだいたいねえ～♪
 *
 * @param modifier [Modifier]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentTimeTitle(modifier: Modifier = Modifier) {
    val currentTime = remember { mutableStateOf(0L) }
    LaunchedEffect(key1 = Unit, block = {
        while (isActive) {
            delay(100L)
            currentTime.value = System.currentTimeMillis()
        }
    })
    LargeTopAppBar(
        modifier = modifier,
        title = {
            Text(text = """
            こんにちは
            今の時間 ${timeToFormat(currentTime.value)}
        """.trimIndent())
        }
    )
}

/** フォーマット形式 */
private val simpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.JAPAN)

/**
 * UnixTimeをフォーマットして返す
 *
 * @param unixTime UnixTime (ms)
 * @return
 */
private fun timeToFormat(unixTime: Long) = simpleDateFormat.format(unixTime)