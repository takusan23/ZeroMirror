package io.github.takusan23.zeromirror.ui.screen.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.zeromirror.R
import io.github.takusan23.zeromirror.ui.components.TextBoxSettingItem

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
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "画面共有の設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(id = R.drawable.ic_outline_arrow_back_24), contentDescription = null)
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
        ) {
            val test = remember { mutableStateOf("100") }

            Card(modifier = Modifier.padding(10.dp)) {

                TextBoxSettingItem(
                    label = "映像ビットレート",
                    inputUnderText = "192 Kbps",
                    description = "サイズが大きくなる代わりに高画質になるはずです。",
                    inputValue = test.value,
                    iconRes = R.drawable.ic_outline_videocam_24,
                    onValueChange = { test.value = it }
                )

                Divider()

                TextBoxSettingItem(
                    label = "映像フレームレート",
                    inputUnderText = "30fps",
                    inputValue = test.value,
                    iconRes = R.drawable.ic_outline_videocam_24,
                    onValueChange = { test.value = it }
                )

            }
        }
    }
}