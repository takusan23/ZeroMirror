package io.github.takusan23.zeromirror.ui.screen.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import io.github.takusan23.zeromirror.R

/**
 * このアプリについて画面
 *
 * @param onBack 戻る押したときに呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingScreen(
    onBack: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "このアプリについて") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(id = R.drawable.ic_outline_arrow_back_24), contentDescription = null)
                    }
                }
            )
        }
    ) {
        Column(modifier = Modifier.padding(it)) {

        }
    }
}