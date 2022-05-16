package io.github.takusan23.zeromirror.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.zeromirror.R


/**
 * 設定の各アイテム
 *
 * @param title タイトル
 * @param description 説明
 * @param iconRes アイコンのリソース画像
 * @param onClick 押したとき
 * */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String = "",
    iconRes: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 10.dp),
                painter = painterResource(id = iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    modifier = Modifier.padding(bottom = 5.dp),
                    fontSize = 20.sp
                )
                Text(text = description)
            }
        }
    }
}

/**
 * 入力機能がある設定項目
 *
 * @param modifier [Modifier]
 * @param label タイトル
 * @param description 説明
 * @param inputUnderText 入力の下にある説明
 * @param iconRes アイコンのリソース画像
 * @param inputValue テキストボックスに入れる値
 * @param onValueChange 変更時に呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextBoxSettingItem(
    modifier: Modifier = Modifier,
    label: String,
    inputValue: String,
    description: String? = null,
    inputUnderText: String? = null,
    iconRes: Int,
    onValueChange: (String) -> Unit,
) {
    Surface(
        modifier = modifier,
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 10.dp),
                painter = painterResource(id = iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
            Column(
                modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth(),
                    value = inputValue,
                    label = { Text(text = label) },
                    onValueChange = onValueChange,
                    singleLine = true,
                )

                if (inputUnderText != null) {
                    Text(
                        modifier = Modifier.padding(5.dp),
                        text = inputUnderText
                    )
                }

                if (description != null) {
                    OutlinedCard(
                        modifier = Modifier.padding(5.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier.padding(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(painter = painterResource(id = R.drawable.ic_outline_info_24), contentDescription = null)
                            Text(
                                modifier = Modifier.padding(start = 5.dp),
                                text = description
                            )
                        }
                    }
                }
            }
        }
    }
}