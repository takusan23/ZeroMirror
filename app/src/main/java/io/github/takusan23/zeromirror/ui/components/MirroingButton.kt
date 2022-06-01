package io.github.takusan23.zeromirror.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.zeromirror.R

/**
 * ミラーリング開始、終了ボタン
 *
 * @param modifier [Modifier]
 * @param onStartClick 開始ボタンおしたら呼ばれる
 * @param onStopClick 終了ボタン押したら呼ばれる
 */
@Composable
fun MirroringButton(
    modifier: Modifier = Modifier,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
) {

    Row(modifier = modifier) {
        Button(
            modifier = Modifier
                .padding(5.dp)
                .weight(1f),
            onClick = onStartClick
        ) {
            Icon(painter = painterResource(id = R.drawable.ic_outline_videocam_24), contentDescription = null)
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = stringResource(id = R.string.mirroring_component_start))
        }
        OutlinedButton(
            modifier = Modifier
                .padding(5.dp)
                .weight(1f),
            onClick = onStopClick
        ) {
            Icon(painter = painterResource(id = R.drawable.ic_outline_close_24), contentDescription = null)
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = stringResource(id = R.string.mirroring_component_end))
        }
    }
}

