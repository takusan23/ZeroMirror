package io.github.takusan23.zeromirror.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.takusan23.zeromirror.tool.IpAddressTool
import io.github.takusan23.zeromirror.ui.components.ConnectCard
import io.github.takusan23.zeromirror.ui.components.MirrorFloatingActionButton
import io.github.takusan23.zeromirror.ui.components.MirroringNoticeCard
import io.github.takusan23.zeromirror.ui.components.StreamEncodeStatusUI

/**
 * ホーム画面、ミラーリングの開始など。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    // IPアドレスをFlowで受け取る
    val idAddress = remember { IpAddressTool.collectIpAddress(context) }.collectAsState(initial = null)

    Scaffold(
        floatingActionButton = {
            MirrorFloatingActionButton {
                // 押したとき...
            }
        }
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            LargeTopAppBar(
                title = { Text(text = "管理画面") }
            )

            // ミラーリング注意してね
            MirroringNoticeCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            )

            // URL表示
            ConnectCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                url = "http://${idAddress.value}:2828"
            )

            // エンコーダー
            StreamEncodeStatusUI()
        }
    }
}