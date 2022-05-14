package io.github.takusan23.zeromirror.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.takusan23.zeromirror.tool.IpAddressTool
import io.github.takusan23.zeromirror.ui.components.CurrentTimeTitle
import io.github.takusan23.zeromirror.ui.components.LiveStreamStatusUI
import io.github.takusan23.zeromirror.ui.components.MirroringButton
import io.github.takusan23.zeromirror.ui.components.UrlCard

/**
 * ホーム画面、ミラーリングの開始など。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    // IPアドレスをFlowで受け取る
    val idAddress = remember { IpAddressTool.collectIpAddress(context) }.collectAsState(initial = null)

    Scaffold {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

            // 現在時刻
            CurrentTimeTitle(modifier = Modifier.fillMaxWidth())

            // 開始 / 終了 ボタン
            MirroringButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
            )

            // URL表示
            UrlCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                url = "http://${idAddress.value}:2828"
            )

            // エンコーダー
            LiveStreamStatusUI(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
            )
        }
    }
}