package io.github.takusan23.zeromirror.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.zeromirror.R

/**
 * はじめまして画面 へ遷移するカード
 *
 * @param modifier [Modifier]
 * @param onHelloClick カード押したら呼ばれる
 * @param onClose 閉じるボタン押したら呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelloCard(
    modifier: Modifier = Modifier,
    onHelloClick: () -> Unit,
    onClose: () -> Unit,
) {
    Card(
        modifier = modifier,
        onClick = onHelloClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.padding(10.dp),
                painter = painterResource(id = R.drawable.zeromirror_android),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier
                    .padding(top = 5.dp, bottom = 5.dp)
                    .weight(1f)
            ) {
                Text(
                    text = stringResource(id = R.string.hello_card_title),
                    style = TextStyle(fontWeight = FontWeight.Bold),
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = stringResource(id = R.string.hello_card_description))
            }
            IconButton(
                onClick = onClose)
            { Icon(painter = painterResource(id = R.drawable.ic_outline_close_24), contentDescription = null) }
        }
    }
}