package io.github.takusan23.zeromirror.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 初回説明用の画面
 *
 * ブラウザから見れる！お手軽！
 * でも映像の切り替えが目に見えるレベルであるのでそれが玉に瑕
 *
 * @param onNextClick ボタンを押したときに呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(onNextClick: () -> Unit) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "はじめまして、ぜろみらーです。") }
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier.padding(10.dp),
                textAlign = TextAlign.Center,
                text = """
                    |このアプリは画面を共有するアプリです（ミラーリング）。
                    |同じWi-Fiに接続しているPCやスマホからこのスマホの画面を共有できます。
                    |しかもブラウザで開くだけ。
                    |映像の切り替え時に一瞬読み込みがあるのは仕様です。
                """.trimMargin()
            )
            Button(
                modifier = Modifier.padding(top = 10.dp),
                onClick = onNextClick
            ) { Text(text = "はじめる") }
        }
    }
}