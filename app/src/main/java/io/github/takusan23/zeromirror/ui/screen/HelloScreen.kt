package io.github.takusan23.zeromirror.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.takusan23.zeromirror.R

/**
 * 初回説明用の画面
 *
 * ブラウザから見れる！お手軽！
 * でも映像の切り替えが目に見えるレベルであるのでそれが玉に瑕
 *
 * @param onNextClick ボタンを押したときに呼ばれる
 * @param onBack 戻ってほしいときに呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelloScreen(onNextClick: () -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(id = R.string.hello_sceren_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(id = R.drawable.ic_outline_arrow_back_24), contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .aspectRatio(1f),
                painter = painterResource(id = R.drawable.first_screen_android),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )

            Text(
                modifier = Modifier.padding(10.dp),
                textAlign = TextAlign.Center,
                text = stringResource(id = R.string.hello_screen_message)
            )
            Button(
                modifier = Modifier.padding(top = 10.dp),
                onClick = onNextClick
            ) { Text(text = stringResource(id = R.string.hello_screen_start)) }
        }
    }
}