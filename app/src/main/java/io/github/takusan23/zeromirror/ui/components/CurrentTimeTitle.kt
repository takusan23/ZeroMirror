package io.github.takusan23.zeromirror.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.takusan23.zeromirror.R
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
 * @param onSettingClick 設定押したとき
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentTimeTitle(
    modifier: Modifier = Modifier,
    onSettingClick: () -> Unit,
) {
    val currentTime = remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(key1 = Unit) {
        while (isActive) {
            delay(1000L)
            currentTime.value = System.currentTimeMillis()
        }
    }
    LargeTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = """
                    ${stringResource(id = R.string.time_component_time_hello)}
                    ${stringResource(id = R.string.time_component_time_now)} ${timeToFormat(currentTime.value)}
                """.trimIndent()
            )
        },
        actions = {
            IconButton(onClick = onSettingClick) {
                Icon(painter = painterResource(id = R.drawable.ic_outline_settings_24), contentDescription = null)
            }
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