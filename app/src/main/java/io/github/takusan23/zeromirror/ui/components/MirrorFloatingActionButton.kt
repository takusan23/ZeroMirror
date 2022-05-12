package io.github.takusan23.zeromirror.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import io.github.takusan23.zeromirror.R

/**
 * ミラーリング、開始終了のFAB
 *
 * @param onClick 押したときに呼ばれる
 */
@Composable
fun MirrorFloatingActionButton(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.End) {
        ExtendedFloatingActionButton(onClick = onClick) {
            Icon(painter = painterResource(id = R.drawable.ic_outline_videocam_24), contentDescription = null)
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = "ミラーリング開始 / 終了")
        }
    }
}