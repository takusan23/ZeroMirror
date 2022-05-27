package io.github.takusan23.zeromirror.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.zeromirror.R

/**
 * このアプリについてのトップ
 *
 * @param modifier [Modifier]
 * @param onGitHubClick GitHubで開くを押したときに呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutTopCard(
    modifier: Modifier = Modifier,
    onGitHubClick: () -> Unit,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                modifier = Modifier
                    .padding(10.dp)
                    .size(80.dp),
                painter = painterResource(id = R.drawable.zeromirror_android),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )

            Text(
                modifier = Modifier.padding(5.dp),
                text = stringResource(id = R.string.app_name),
                style = TextStyle(fontWeight = FontWeight.Bold),
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                modifier = Modifier.padding(5.dp),
                textAlign = TextAlign.Center,
                text = "ブラウザーがあれば使えるお手軽ミラーリングアプリ"
            )

            Button(
                modifier = Modifier.padding(5.dp),
                onClick = onGitHubClick
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_outline_open_in_browser_24), contentDescription = null)
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = "GitHubで開く (オープンソース)")
            }
        }
    }
}

/**
 * アプリのバージョンとかを表示している部分
 *
 * @param modifier [Modifier]
 * @param appVersion アプリのバージョン
 * @param twitterId TwitterId
 * @param onTwitterClick Twitter押したとき
 */
@Composable
fun AboutInfoListCard(
    modifier: Modifier = Modifier,
    appVersion: String,
    twitterId: String,
    onTwitterClick: () -> Unit,
) {
    Column(modifier = modifier) {
        SettingItem(
            title = appVersion,
            description = "バージョン",
            iconRes = R.drawable.ic_outline_info_24,
            onClick = { }
        )
        SettingItem(
            title = twitterId,
            description = "Twitter",
            iconRes = R.drawable.ic_outline_info_24,
            onClick = { onTwitterClick() }
        )
    }
}