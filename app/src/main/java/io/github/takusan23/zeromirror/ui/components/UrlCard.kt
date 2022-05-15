package io.github.takusan23.zeromirror.ui.components

import androidx.compose.foundation.layout.*
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
 * @param onShareClick 共有押したとき
 * @param onOpenBrowserClick ブラウザーで開く押したとき
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlCard(
    modifier: Modifier = Modifier,
    url: String,
    onShareClick: () -> Unit,
    onOpenBrowserClick: () -> Unit,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                modifier = Modifier.padding(5.dp),
                text = "視聴用URL",
                style = TextStyle(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 20.sp
            )
            Text(
                modifier = Modifier.padding(5.dp),
                text = url,
                fontSize = 20.sp
            )
            Text(
                modifier = Modifier.padding(5.dp),
                text = "このURLをPCやスマホのブラウザのアドレス欄へ入力すると画面共有を見ることが出来ます。",
            )

            Row {
                Button(
                    onClick = onShareClick
                ) { Text(text = "共有") }
                Spacer(modifier = Modifier.size(10.dp))

                OutlinedButton(
                    onClick = onOpenBrowserClick
                ) { Text(text = "ブラウザで確認する") }
            }
        }
    }
}