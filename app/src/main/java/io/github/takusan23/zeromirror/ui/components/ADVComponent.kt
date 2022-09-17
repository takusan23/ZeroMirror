package io.github.takusan23.zeromirror.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.zeromirror.R

// このアプリについてのコンポーネント

/**
 * アイコンが出てるところ
 * ADVだとヒロインとテキストが出てるところ
 *
 * @param modifier [Modifier]
 * @param version アプリのバージョン
 */
@Composable
fun AdvMain(
    modifier: Modifier = Modifier,
    version: String,
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                modifier = Modifier
                    .padding(10.dp)
                    .size(100.dp),
                painter = painterResource(id = R.drawable.zeromirror_android),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
        }
        // テキストがでてる
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                modifier = Modifier
                    .padding(bottom = 5.dp),
                text = "${stringResource(id = R.string.app_name)} ( $version ) ",
                fontSize = 20.sp
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
            ) {
                Box(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = stringResource(id = R.string.about_this_app_message),
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}

/**
 * メニュー
 * ADVだとメニューのやつ
 *
 * @param isScrollable スクロールする場合はtrue
 * @param onTwitterClick Twitter開いてほしいときに呼ばれる
 * @param onGitHubClick GitHub開いてほしいときに呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvMenu(
    isScrollable: Boolean,
    onTwitterClick: () -> Unit,
    onGitHubClick: () -> Unit,
) {

    Row(
        modifier = if (isScrollable) {
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        } else Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        AdvMenuItem(text = stringResource(id = R.string.about_this_app_twitter), onClick = onTwitterClick)
        AdvMenuItem(text = stringResource(id = R.string.about_this_app_open_github), onClick = onGitHubClick)
    }
}

/**
 * メニューの各コンポーネント
 *
 * @param text テキスト
 * @param onClick メニュー押したら呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvMenuItem(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        shape = CutCornerShape(10.dp, 0.dp, 0.dp, 0.dp),
        color = Color.Transparent,
        onClick = onClick
    ) {
        Box(modifier = Modifier.padding(10.dp)) {
            Text(text = text)
        }
    }
}
