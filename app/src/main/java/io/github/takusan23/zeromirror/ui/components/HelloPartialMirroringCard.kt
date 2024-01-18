package io.github.takusan23.zeromirror.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.zeromirror.R

/**
 * Android 14 QPR2 から追加されて部分的な画面共有に対応したよカード
 * （全画面ではなく一つのアプリだけ画面共有出来るようになった）
 */
@Composable
fun HelloPartialMirroringCard(
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    OutlinedCard(modifier = modifier) {
        Icon(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            painter = painterResource(id = R.drawable.partial_mirroring_pause),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Divider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.hello_partial_mirroring_card_description)
            )
            IconButton(
                onClick = onClose
            ) { Icon(painter = painterResource(id = R.drawable.ic_outline_close_24), contentDescription = null) }
        }
    }
}