package io.github.takusan23.zeromirror.ui.screen.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.zeromirror.R
import io.github.takusan23.zeromirror.data.MirroringSettingData
import io.github.takusan23.zeromirror.tool.DisplayConverter
import io.github.takusan23.zeromirror.ui.components.SwitchSettingItem
import io.github.takusan23.zeromirror.ui.components.TextBoxSettingItem
import kotlinx.coroutines.launch

/**
 * 画面共有 設定画面
 *
 * @param onBack 戻ってほしいときに呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MirroringSettingScreen(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mirroringData = remember { MirroringSettingData.loadDataStore(context) }.collectAsState(initial = null)
    val snackbarHostState = remember { SnackbarHostState() }

    /**
     * 設定を更新する
     * @param onUpdateData データクラスをコピーして返して
     */
    fun updateSetting(onUpdateData: (MirroringSettingData) -> MirroringSettingData) {
        scope.launch {
            MirroringSettingData.setDataStore(context, onUpdateData(mirroringData.value!!))
        }
    }

    /** ミラーリング設定を初期値に戻す */
    fun resetMirrorSetting() {
        updateSetting {
            it.copy(
                portNumber = MirroringSettingData.DEFAULT_PORT_NUMBER,
                intervalMs = MirroringSettingData.DEFAULT_INTERVAL_MS,
                videoBitRate = MirroringSettingData.DEFAULT_VIDEO_BIT_RATE,
                videoFrameRate = MirroringSettingData.DEFAULT_VIDEO_FRAME_RATE,
                audioBitRate = MirroringSettingData.DEFAULT_AUDIO_BIT_RATE,
                isRecordInternalAudio = false
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "画面共有の設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(id = R.drawable.ic_outline_arrow_back_24), contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Snackbar で本当に消していいか
                        scope.launch {
                            val result = snackbarHostState.showSnackbar("本当にリセットしますか", "リセット")
                            if (result == SnackbarResult.ActionPerformed) {
                                resetMirrorSetting()
                            }
                        }
                    }) { Icon(painter = painterResource(id = R.drawable.ic_outline_restart_alt_24), contentDescription = null) }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
        ) {
            if (mirroringData.value != null) {
                Card(modifier = Modifier.padding(10.dp)) {

                    TextBoxSettingItem(
                        label = "動画の更新頻度 (秒)",
                        inputValue = (mirroringData.value!!.intervalMs / 1000).toString(),
                        description = "短くするとブラウザに流れる映像との遅延が短くなります",
                        iconRes = R.drawable.ic_outline_timer_24,
                        onValueChange = { intervalMs ->
                            // 更新する
                            updateSetting { it.copy(intervalMs = (intervalMs.toLongOrNull() ?: 0) * 1000) }
                        }
                    )

                    Divider()

                    TextBoxSettingItem(
                        label = "映像ビットレート (Kbps)",
                        description = "サイズが大きくなる代わりに画質が上がるはずです",
                        inputValue = (mirroringData.value!!.videoBitRate / 1000).toString(),
                        inputUnderText = DisplayConverter.convert(mirroringData.value!!.videoBitRate),
                        iconRes = R.drawable.ic_outline_videocam_24,
                        onValueChange = { videoBitRate ->
                            updateSetting { it.copy(videoBitRate = (videoBitRate.toIntOrNull() ?: 0) * 1000) }
                        }
                    )

                    TextBoxSettingItem(
                        label = "映像フレームレート",
                        inputValue = mirroringData.value!!.videoFrameRate.toString(),
                        description = "映像の滑らかさですね",
                        iconRes = R.drawable.ic_outline_videocam_24,
                        onValueChange = { videoFps ->
                            updateSetting { it.copy(videoFrameRate = videoFps.toIntOrNull() ?: 30) }
                        }
                    )

                    Divider()

                    SwitchSettingItem(
                        title = "内部音声を含める",
                        description = "Android 10 以降",
                        iconRes = R.drawable.ic_outline_audiotrack_24,
                        isEnable = mirroringData.value!!.isRecordInternalAudio,
                        onValueChange = { isChecked ->
                            updateSetting { it.copy(isRecordInternalAudio = isChecked) }
                        }
                    )

                    TextBoxSettingItem(
                        label = "音声ビットレート (Kbps)",
                        description = "音質です",
                        inputValue = (mirroringData.value!!.audioBitRate / 1000).toString(),
                        inputUnderText = DisplayConverter.convert(mirroringData.value!!.audioBitRate),
                        iconRes = R.drawable.ic_outline_audiotrack_24,
                        onValueChange = { audioBitRate ->
                            updateSetting { it.copy(audioBitRate = (audioBitRate.toIntOrNull() ?: 128) * 1000) }
                        }
                    )

                    Divider()

                    TextBoxSettingItem(
                        label = "ポート番号",
                        description = "基本的にはいじる必要はないと思います",
                        inputValue = mirroringData.value!!.portNumber.toString(),
                        iconRes = R.drawable.ic_outline_open_in_browser_24,
                        onValueChange = { portNumber ->
                            updateSetting { it.copy(portNumber = portNumber.toIntOrNull() ?: MirroringSettingData.DEFAULT_PORT_NUMBER) }
                        }
                    )
                }
            }
        }
    }
}