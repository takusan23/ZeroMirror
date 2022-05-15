package io.github.takusan23.zeromirror.ui.screen.setting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import io.github.takusan23.zeromirror.R
import io.github.takusan23.zeromirror.ui.components.SettingItem
import io.github.takusan23.zeromirror.ui.screen.MainScreenNavigationLinks

/**
 * 設定画面
 *
 * @param onBack 戻ってほしいときに呼ばれる
 * @param onNavigate 画面遷移の際に呼ばれる、パスが渡されます
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "設定画面") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(id = R.drawable.ic_outline_arrow_back_24), contentDescription = null)
                    }
                }
            )
        }
    ) {
        Box(modifier = Modifier.padding(it)) {
            LazyColumn {
                item {
                    SettingItem(
                        modifier = Modifier.fillMaxWidth(),
                        title = "画質、更新頻度の設定",
                        iconRes = R.drawable.ic_outline_videocam_24,
                        description = "ビットレートとか",
                        onClick = { onNavigate(MainScreenNavigationLinks.SettingMirroringSettingScreen) }
                    )
                }
                item {
                    SettingItem(
                        modifier = Modifier.fillMaxWidth(),
                        title = "このアプリについて",
                        iconRes = R.drawable.ic_outline_info_24,
                        description = "オープンソースです",
                        onClick = { }
                    )
                }
            }
        }
    }
}