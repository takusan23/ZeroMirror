package io.github.takusan23.zeromirror.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
 * @param isScreenMirroring 画面共有中なら true
 * @param onStartClick 開始ボタンおしたら呼ばれる
 * @param onStopClick 終了ボタン押したら呼ばれる
 */
@Composable
fun MirroringButton(
    modifier: Modifier = Modifier,
    isScreenMirroring: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
) {
    Box(modifier = modifier) {
        if (isScreenMirroring) {
            OutlinedButton(
                modifier = Modifier
                    .padding(5.dp)
                    .fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                onClick = onStopClick
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_outline_close_24), contentDescription = null)
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = stringResource(id = R.string.mirroring_component_end))
            }
        } else {
            Button(
                modifier = Modifier
                    .padding(5.dp)
                    .fillMaxWidth(),
                onClick = onStartClick
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_outline_videocam_24), contentDescription = null)
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = stringResource(id = R.string.mirroring_component_start))
            }
        }
    }
}

