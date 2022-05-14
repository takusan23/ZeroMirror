package io.github.takusan23.zeromirror.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.zeromirror.R

/**
 * 内部音声
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternalAudioPermissionCard(modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                modifier = Modifier.padding(5.dp),
                text = "Android 10 以降 内部音声を共有できます",
                style = TextStyle(fontWeight = FontWeight.Bold),
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(10.dp))

            Column(modifier = Modifier.padding(5.dp)) {
                Text(text = "画面共有に内部の音声を含めることができます。内部音声を含めるためには、マイク権限が必要ですのでお願いします。")
            }
            OutlinedButton(
                modifier = Modifier
                    .align(Alignment.End),
                onClick = { }
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_outline_settings_24), contentDescription = null)
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = "マイクの権限を付与")
            }
        }
    }
}