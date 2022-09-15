package io.github.takusan23.zeromirror.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.zeromirror.R
import io.github.takusan23.zeromirror.data.StreamingType

/**
 * ストリーミング方式（WebSocketでmp4を送る方式 / MPEG-DASHのライブ配信方式）
 *
 * @param modifier [Modifier]
 * @param currentType 選択中の[StreamingType]
 * @param onClick 押したら呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamingTypeCard(
    modifier: Modifier,
    currentType: StreamingType,
    onClick: (StreamingType) -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.secondary
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                modifier = Modifier.padding(5.dp),
                text = stringResource(id = R.string.streaming_setting_type_title),
                style = TextStyle(fontWeight = FontWeight.Bold),
                color = LocalContentColor.current,
                fontSize = 20.sp
            )
            StreamingTypeMenu(
                currentType = currentType,
                onClick = onClick
            )
            Text(
                modifier = Modifier.padding(5.dp),
                text = stringResource(id = currentType.message)
            )
        }
    }
}

/**
 * 選択メニュー
 *
 * @param modifier [Modifier]
 * @param currentType 選択中の[StreamingType]
 * @param onClick 押したら呼ばれる
 */
@ExperimentalMaterial3Api
@Composable
private fun StreamingTypeMenu(
    modifier: Modifier = Modifier,
    currentType: StreamingType,
    onClick: (StreamingType) -> Unit,
) {
    Surface(
        modifier = modifier,
        color = Color.Transparent,
        border = BorderStroke(1.dp, LocalContentColor.current),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row {
            StreamingType.values().forEach { type ->
                val isSelected = type == currentType
                val color = if (isSelected) MaterialTheme.colorScheme.primary else contentColorFor(MaterialTheme.colorScheme.surface)
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Transparent,
                    contentColor = color,
                    border = if (isSelected) BorderStroke(2.dp, color) else null,
                    onClick = { onClick(type) }
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            modifier = Modifier.size(40.dp),
                            painter = painterResource(id = type.icon),
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(id = type.title),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}