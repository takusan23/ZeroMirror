package io.github.takusan23.zeromirror.ui.screen

import android.app.Activity
import android.content.Context
import android.media.projection.MediaProjectionManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import io.github.takusan23.zeromirror.ScreenMirrorService
import io.github.takusan23.zeromirror.setting.dataStore
import io.github.takusan23.zeromirror.tool.IpAddressTool
import io.github.takusan23.zeromirror.ui.components.*

/**
 * ホーム画面、ミラーリングの開始など。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current

    // IPアドレスをFlowで受け取る
    val idAddress = remember { IpAddressTool.collectIpAddress(context) }.collectAsState(initial = null)
    // DataStore、SharedPreferenceの代替
    val dataStore = context.dataStore.data.collectAsState(initial = null)

    // 権限を求めてサービスを起動する
    val mediaProjectionManager = remember { context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    val requestCapture = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // ミラーリングサービス開始
        if (result.resultCode == ComponentActivity.RESULT_OK && result.data != null) {
            // Activity 以外は無いはず...
            ScreenMirrorService.startService((context as Activity), result.resultCode, result.data!!)
        } else {
            Toast.makeText(context, "開始が拒否されました、(ヽ´ω`)", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

            // 現在時刻
            CurrentTimeTitle(modifier = Modifier.fillMaxWidth())

            // 開始 / 終了 ボタン
            MirroringButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                onStartClick = {
                    // キャプチャー権限を求める
                    requestCapture.launch(mediaProjectionManager.createScreenCaptureIntent())
                },
                onStopClick = {
                    // 終了させる
                    ScreenMirrorService.stopService(context)
                }
            )

            // URL表示
            UrlCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                url = "http://${idAddress.value}:2828"
            )

            // 内部音声にはマイク権限
            InternalAudioPermissionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
            )

            // エンコーダー
            StreamInfoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
            )
        }
    }
}