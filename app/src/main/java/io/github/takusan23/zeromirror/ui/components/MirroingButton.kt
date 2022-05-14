package io.github.takusan23.zeromirror.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import io.github.takusan23.zeromirror.R

/**
 * ミラーリング開始、終了ボタン
 *
 * @param modifier [Modifier]
 * @param isRunning 実行中の場合はtrueにする
 */
@Composable
fun MirroringButton(
    modifier: Modifier = Modifier,
    isRunning: Boolean = false,
) {
    val iconLabelPair = if (isRunning) {
        "ミラーリング終了" to R.drawable.ic_outline_close_24
    } else {
        "ミラーリング開始" to R.drawable.ic_outline_videocam_24
    }

    Button(
        modifier = modifier,
        onClick = { }
    ) {
        Icon(painter = painterResource(id = iconLabelPair.second), contentDescription = null)
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Text(text = iconLabelPair.first, fontSize = 20.sp)
    }
}