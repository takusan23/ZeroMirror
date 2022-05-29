package io.github.takusan23.zeromirror.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.zeromirror.R
import io.github.takusan23.zeromirror.data.MirroringSettingData
import io.github.takusan23.zeromirror.tool.DisplayConverter

/**
 * 更新間隔、ビットレート、fpsとかを表示する
 *
 * @param modifier [Modifier]
 * @param mirroringData [MirroringSettingData]
 * @param isGrantedAudioPermission 録音権限があればtrue
 * @param onSettingClick 設定遷移ボタンを押したときに呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamInfo(
    modifier: Modifier = Modifier,
    mirroringData: MirroringSettingData,
    isGrantedAudioPermission: Boolean,
    onSettingClick: () -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier.padding(5.dp),
            text = "画面共有 情報",
            style = TextStyle(fontWeight = FontWeight.Bold),
            fontSize = 20.sp,
            color = LocalContentColor.current
        )
        Spacer(modifier = Modifier.size(10.dp))

        Column(modifier = Modifier.padding(5.dp)) {
            Text(fontSize = 20.sp, text = "内部音声を含める (Android 10 以降)")
            Text(text = if (mirroringData.isRecordInternalAudio) "有効" else "無効")
            if (isGrantedAudioPermission) {
                Text(text = "(マイクの権限が付与されていないため、利用できません)")
            }
        }
        Column(modifier = Modifier.padding(5.dp)) {
            Text(fontSize = 20.sp, text = "更新間隔 (秒)")
            Text(text = (mirroringData.intervalMs / 1000).toInt().toString())
        }
        Column(modifier = Modifier.padding(5.dp)) {
            Text(fontSize = 20.sp, text = "映像ビットレート")
            Text(text = DisplayConverter.convert(mirroringData.videoBitRate))
        }
        Column(modifier = Modifier.padding(5.dp)) {
            Text(fontSize = 20.sp, text = "映像フレームレート (fps)")
            Text(text = mirroringData.videoFrameRate.toString())
        }
        Column(modifier = Modifier.padding(5.dp)) {
            Text(fontSize = 20.sp, text = "音声ビットレート")
            Text(text = DisplayConverter.convert(mirroringData.audioBitRate))
        }

        OutlinedButton(
            modifier = Modifier
                .align(Alignment.End),
            onClick = onSettingClick,
        ) {
            Icon(painter = painterResource(id = R.drawable.ic_outline_settings_24), contentDescription = null)
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = "設定を変更する")
        }
    }
}
