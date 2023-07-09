package io.github.takusan23.zeromirror.ui.screen.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.zeromirror.R
import io.github.takusan23.zeromirror.data.MirroringSettingData
import io.github.takusan23.zeromirror.tool.NumberConverter
import io.github.takusan23.zeromirror.ui.components.SwitchSettingItem
import io.github.takusan23.zeromirror.ui.components.TextBoxInitValueSettingItem
import kotlinx.coroutines.launch

/**
 * 画面共有 設定画面
 *
 * @param onBack 戻ってほしいときに呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MirroringSettingScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mirroringData = remember { MirroringSettingData.loadDataStore(context) }.collectAsState(initial = null)
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

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
        scope.launch {
            // リセットしたら設定画面を閉じる（InitValueなんたらは初期値のみ受け付けるので）
            MirroringSettingData.resetDataStore(context)
            onBack()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(id = R.string.setting_stream_title)) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(id = R.drawable.ic_outline_arrow_back_24), contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Snackbar で本当に消していいか
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(context.getString(R.string.mirroring_setting_reset_message), context.getString(R.string.mirroring_setting_reset))
                            if (result == SnackbarResult.ActionPerformed) {
                                resetMirrorSetting()
                            }
                        }
                    }) { Icon(painter = painterResource(id = R.drawable.ic_outline_restart_alt_24), contentDescription = null) }
                }
            )
        }
    ) {
        val mirroringSettingData = mirroringData.value
        if (mirroringSettingData != null) {
            LazyColumn(
                modifier = Modifier.padding(it),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Card(modifier = Modifier.padding(10.dp)) {

                        TextBoxInitValueSettingItem(
                            title = stringResource(id = R.string.mirroring_setting_video_interval_title),
                            description = stringResource(id = R.string.mirroring_setting_video_interval_description),
                            initValue = (mirroringSettingData.intervalMs / 1000).toString(),
                            iconRes = R.drawable.ic_outline_timer_24,
                            onValueChange = {
                                it.toLongOrNull()?.also { intervalMs ->
                                    // 更新する
                                    updateSetting { it.copy(intervalMs = intervalMs * 1000) }
                                }
                            }
                        )

                        Divider()

                        TextBoxInitValueSettingItem(
                            title = stringResource(id = R.string.mirroring_setting_video_bitrate_title),
                            description = stringResource(id = R.string.mirroring_setting_video_bitrate_description),
                            initValue = (mirroringSettingData.videoBitRate / 1000).toString(),
                            inputUnderText = NumberConverter.convert(mirroringSettingData.videoBitRate),
                            iconRes = R.drawable.ic_outline_videocam_24,
                            onValueChange = {
                                it.toIntOrNull()?.also { videoBitRate ->
                                    updateSetting { it.copy(videoBitRate = videoBitRate * 1000) }
                                }
                            }
                        )

                        TextBoxInitValueSettingItem(
                            title = stringResource(id = R.string.mirroring_setting_video_fps_title),
                            description = stringResource(id = R.string.mirroring_setting_video_fps_description),
                            initValue = mirroringSettingData.videoFrameRate.toString(),
                            iconRes = R.drawable.ic_outline_videocam_24,
                            onValueChange = {
                                it.toIntOrNull()?.also { videoFps ->
                                    updateSetting { it.copy(videoFrameRate = videoFps) }
                                }
                            }
                        )

                        Divider()

                        SwitchSettingItem(
                            title = stringResource(id = R.string.mirroring_setting_resolution),
                            subTitle = stringResource(id = R.string.mirroring_setting_resolution_description),
                            description = stringResource(id = R.string.mirroring_setting_resolution_hint),
                            isEnable = mirroringSettingData.isCustomResolution,
                            iconRes = R.drawable.ic_outline_aspect_ratio_24,
                            onValueChange = { after ->
                                updateSetting { it.copy(isCustomResolution = after) }
                            }
                        )

                        if (mirroringSettingData.isCustomResolution) {

                            TextBoxInitValueSettingItem(
                                title = stringResource(id = R.string.mirroring_setting_resolution_video_height),
                                initValue = mirroringSettingData.videoHeight.toString(),
                                iconRes = R.drawable.ic_outline_aspect_ratio_24,
                                onValueChange = { after ->
                                    after.toIntOrNull()?.also { height ->
                                        updateSetting { it.copy(videoHeight = height) }
                                    }
                                }
                            )

                            TextBoxInitValueSettingItem(
                                title = stringResource(id = R.string.mirroring_setting_resolution_video_width),
                                initValue = mirroringSettingData.videoWidth.toString(),
                                iconRes = R.drawable.ic_outline_aspect_ratio_24,
                                onValueChange = { after ->
                                    after.toIntOrNull()?.also { width ->
                                        updateSetting { it.copy(videoWidth = width) }
                                    }
                                }
                            )
                        }

                        Divider()

                        SwitchSettingItem(
                            title = stringResource(id = R.string.mirroring_setting_internal_audio_title),
                            subTitle = stringResource(id = R.string.mirroring_setting_internal_audio_description),
                            iconRes = R.drawable.ic_outline_audiotrack_24,
                            isEnable = mirroringSettingData.isRecordInternalAudio,
                            onValueChange = { isChecked ->
                                updateSetting { it.copy(isRecordInternalAudio = isChecked) }
                            }
                        )

                        TextBoxInitValueSettingItem(
                            title = stringResource(id = R.string.mirroring_setting_audio_bitrate_title),
                            description = stringResource(id = R.string.mirroring_setting_audio_bitrate_description),
                            initValue = (mirroringSettingData.audioBitRate / 1000).toString(),
                            inputUnderText = NumberConverter.convert(mirroringSettingData.audioBitRate),
                            iconRes = R.drawable.ic_outline_audiotrack_24,
                            onValueChange = {
                                it.toIntOrNull()?.also { audioBitRate ->
                                    updateSetting { it.copy(audioBitRate = audioBitRate * 1000) }
                                }
                            }
                        )
                    }
                }

                item {
                    Card(modifier = Modifier.padding(10.dp)) {

                        Text(
                            modifier = Modifier.padding(10.dp),
                            text = stringResource(id = R.string.mirroring_setting_radvanced),
                            style = TextStyle(fontWeight = FontWeight.Bold),
                            fontSize = 20.sp
                        )

                        TextBoxInitValueSettingItem(
                            title = stringResource(id = R.string.mirroring_setting_port_title),
                            description = stringResource(id = R.string.mirroring_setting_port_description),
                            initValue = mirroringSettingData.portNumber.toString(),
                            iconRes = R.drawable.ic_outline_open_in_browser_24,
                            onValueChange = {
                                it.toIntOrNull()?.also { portNumber ->
                                    updateSetting { it.copy(portNumber = portNumber) }
                                }
                            }
                        )

                        SwitchSettingItem(
                            title = stringResource(id = R.string.mirroring_setting_mpeg_dash_vp8_title),
                            subTitle = stringResource(id = R.string.mirroring_setting_mpeg_dash_vp8_description),
                            iconRes = R.drawable.ic_outline_videocam_24,
                            isEnable = mirroringSettingData.isVP8,
                            onValueChange = { isChecked ->
                                updateSetting { it.copy(isVP8 = isChecked) }
                            }
                        )
                    }
                }
            }
        }
    }
}