package io.github.takusan23.zeromirror.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ミラーリング注意Card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MirroringNoticeCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.Transparent,
        contentColor = contentColorFor(backgroundColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                modifier = Modifier.padding(5.dp),
                text = "ミラーリング / 画面共有",
                style = TextStyle(fontWeight = FontWeight.Bold),
                fontSize = 20.sp
            )
            Text(
                modifier = Modifier.padding(5.dp),
                text = "起動の際に画面録画の権限を求めます。ミラーリングの際は見せちゃダメなものに注意してね！",
                fontSize = 15.sp
            )
        }
    }
}