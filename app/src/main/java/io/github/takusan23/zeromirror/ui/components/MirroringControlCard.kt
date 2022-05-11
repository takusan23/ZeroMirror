package io.github.takusan23.zeromirror.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.zeromirror.R

/**
 * ミラーリング開始ボタンがあるCard
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MirroringControlCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(width = 3.dp, color = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "ミラーリング / 画面共有",
                fontSize = 20.sp
            )
            Row(Modifier.padding(top = 10.dp)) {
                CardButton(
                    modifier = Modifier
                        .padding(5.dp)
                        .weight(1f),
                    title = "ミラーリングを開始"
                )
                CardButton(
                    modifier = Modifier
                        .padding(5.dp)
                        .weight(1f),
                    title = "ミラーリングを終了"
                )
            }
        }
    }
}

/**
 * Card内に表示するボタン
 *
 * @param modifier [Modifier]
 * @param title タイトル
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardButton(modifier: Modifier = Modifier, title: String) {
    Surface(
        selected = false,
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(10.dp),
        onClick = { }
    ) {
        Column(modifier = Modifier.padding(5.dp)) {
            Icon(
                modifier = Modifier.padding(5.dp),
                painter = painterResource(id = R.drawable.ic_outline_settings_24),
                contentDescription = null
            )
            Text(
                modifier = Modifier.padding(5.dp),
                text = title
            )
        }
    }
}