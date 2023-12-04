package io.github.takusan23.zeromirror.ui.screen

import android.app.Activity
import android.content.Context
import android.media.projection.MediaProjectionManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import io.github.takusan23.zeromirror.R
import io.github.takusan23.zeromirror.ScreenMirroringService
import io.github.takusan23.zeromirror.data.MirroringSettingData
import io.github.takusan23.zeromirror.setting.SettingKeyObject
import io.github.takusan23.zeromirror.setting.dataStore
import io.github.takusan23.zeromirror.tool.DisplayTool
import io.github.takusan23.zeromirror.tool.IntentTool
import io.github.takusan23.zeromirror.tool.IpAddressTool
import io.github.takusan23.zeromirror.tool.PermissionTool
import io.github.takusan23.zeromirror.ui.components.CurrentTimeTitle
import io.github.takusan23.zeromirror.ui.components.HelloCard
import io.github.takusan23.zeromirror.ui.components.InternalAudioPermissionCard
import io.github.takusan23.zeromirror.ui.components.MirroringButton
import io.github.takusan23.zeromirror.ui.components.PostNotificationPermissionCard
import io.github.takusan23.zeromirror.ui.components.StreamInfo
import io.github.takusan23.zeromirror.ui.components.StreamingTypeCard
import io.github.takusan23.zeromirror.ui.components.UrlCard
import kotlinx.coroutines.launch

/**
 * ホーム画面、ミラーリングの開始など。
 *
 * @param onSettingClick 設定を押したとき
 * @param onNavigate 画面遷移の際に呼ばれる、パスが渡されます
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSettingClick: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // スクロールしたら AppBar を小さくするやつ
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    // IPアドレスをFlowで受け取る
    val idAddress = remember { IpAddressTool.collectIpAddress(context) }.collectAsState(initial = null)
    // ミラーリング情報
    val mirroringData = remember { MirroringSettingData.loadDataStore(context) }.collectAsState(initial = null)
    // DataStore監視
    val dataStore = remember { context.dataStore.data }.collectAsState(initial = null)
    // ミラーリングサービスとバインドして、インスタンスを取得する
    val mirroringService = remember { ScreenMirroringService.bindScreenMirrorService(context, lifecycleOwner.lifecycle) }.collectAsState(initial = null)
    // ミラーリング中かどうか
    val isScreenMirroring = mirroringService.value?.isScreenMirroring?.collectAsState()

    // マイク録音権限があるか、Android 10 以前は対応していないので一律 false、Android 10 以降は権限がなければtrueになる
    val isGrantedRecordAudio = remember {
        mutableStateOf(
            if (PermissionTool.isAndroidQAndHigher) {
                !PermissionTool.isGrantedRecordPermission(context)
            } else false
        )
    }
    // 通知権限があるか、フォアグラウンドサービス実行中を通知として発行したいので
    val isGrantedPostNotification = remember {
        mutableStateOf(
            if (PermissionTool.isAndroidTiramisuAndHigher) {
                !PermissionTool.isGrantedPostNotificationPermission(context)
            } else false
        )
    }

    // 権限を求めてサービスを起動する
    val mediaProjectionManager = remember { context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    val requestCapture = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // ミラーリングサービス開始
        if (result.resultCode == ComponentActivity.RESULT_OK && result.data != null) {
            val (height, width) = DisplayTool.getDisplaySize((context as Activity))
            mirroringService.value?.startScreenMirroring(result.resultCode, result.data!!, height, width)
        } else {
            Toast.makeText(context, context.getString(R.string.home_screen_permission_result_fail), Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // 現在時刻
            CurrentTimeTitle(
                modifier = Modifier.fillMaxWidth(),
                scrollBehavior = scrollBehavior,
                onSettingClick = onSettingClick
            )
        }
    ) { paddingValues ->

        LazyColumn(modifier = Modifier.padding(paddingValues)) {

            item {
                // 開始 / 終了 ボタン
                MirroringButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    isScreenMirroring = isScreenMirroring?.value == true,
                    onStartClick = {
                        // キャプチャー権限を求める
                        requestCapture.launch(mediaProjectionManager.createScreenCaptureIntent())
                    },
                    onStopClick = {
                        // 終了させる
                        mirroringService.value?.stopScreenMirroringAndServerRestart()
                    }
                )
            }

            // はじめまして画面誘導カード
            if (dataStore.value?.contains(SettingKeyObject.IS_HIDE_HELLO_CARD) == false) {
                item {
                    HelloCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        onHelloClick = { onNavigate(MainScreenNavigationLinks.HelloScreen) },
                        onClose = {
                            // もう出さない
                            scope.launch {
                                context.dataStore.edit { it[SettingKeyObject.IS_HIDE_HELLO_CARD] = true }
                            }
                        }
                    )
                }
            }

            if (mirroringData.value != null && idAddress.value != null) {
                item {
                    // URLを作る
                    val url = "http://${idAddress.value}:${mirroringData.value?.portNumber}"
                    UrlCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        url = url,
                        onShareClick = {
                            context.startActivity(IntentTool.createShareIntent(url))
                        },
                        onOpenBrowserClick = {
                            context.startActivity(IntentTool.createOpenBrowserIntent(url))
                        }
                    )
                }
            }

            // 内部音声にはマイク権限
            if (isGrantedRecordAudio.value) {
                item {
                    InternalAudioPermissionCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        permissionResult = { isGranted ->
                            // trueなら非表示にするためfalseを入れる
                            isGrantedRecordAudio.value = !isGranted
                            // 内部音声を収録する設定を有効にする
                            scope.launch {
                                val updatedData = mirroringData.value?.copy(isRecordInternalAudio = true) ?: return@launch
                                MirroringSettingData.setDataStore(context, updatedData)
                            }
                        }
                    )
                }
            }

            // 通知権限があるといいよ
            if (isGrantedPostNotification.value) {
                item {
                    PostNotificationPermissionCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        permissionResult = { isGranted ->
                            isGrantedPostNotification.value = !isGranted
                        }
                    )
                }
            }

            // ストリーミング方式の選択
            if (mirroringData.value != null) {
                item {
                    StreamingTypeCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        currentType = mirroringData.value!!.streamingType,
                        onClick = { type ->
                            // 設定を保存
                            scope.launch {
                                MirroringSettingData.setDataStore(context, mirroringData.value!!.copy(streamingType = type))
                            }
                        }
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                ) {
                    // エンコーダー
                    if (mirroringData.value != null) {
                        StreamInfo(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            mirroringData = mirroringData.value!!,
                            isGrantedAudioPermission = isGrantedRecordAudio.value,
                            onSettingClick = { onNavigate(MainScreenNavigationLinks.SettingMirroringSettingScreen) }
                        )
                    }
                }
            }

        }
    }
}