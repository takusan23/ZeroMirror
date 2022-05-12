package io.github.takusan23.zeromirror.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ミラーリング視聴用URLを表示する
 *
 * @param modifier [Modifier]
 * @param url 視聴用URL
 */
@Composable
fun ConnectCard(
    modifier: Modifier = Modifier,
    url: String,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                modifier = Modifier.padding(5.dp),
                text = url,
                style = TextStyle(fontWeight = FontWeight.Bold),
                fontSize = 20.sp
            )
            Text(
                modifier = Modifier.padding(5.dp),
                text = "視聴用URL",
                fontSize = 20.sp
            )
            Text(
                modifier = Modifier.padding(5.dp),
                text = "このURLをPCやスマホのブラウザのアドレス欄へ入力すると画面共有を見ることが出来ます。",
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                item {
                    Button(
                        onClick = { }
                    ) { Text(text = "共有") }
                }
                item {
                    Button(
                        onClick = { }
                    ) { Text(text = "ブラウザで確認する") }
                }
                item {
                    OutlinedButton(
                        onClick = { }
                    ) { Text(text = "QRコードで共有") }
                }
            }
        }
    }
}