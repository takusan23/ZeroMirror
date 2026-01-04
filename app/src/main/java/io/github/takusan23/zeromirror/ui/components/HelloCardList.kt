package io.github.takusan23.zeromirror.ui.components

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import io.github.takusan23.zeromirror.setting.SettingKeyObject
import io.github.takusan23.zeromirror.setting.dataStore
import io.github.takusan23.zeromirror.ui.screen.MainScreenNavigationLinks
import kotlinx.coroutines.launch

/**
 * あいさつカードたち
 * 消せるやつ
 *
 * [HelloCard]
 * [HelloPartialMirroringCard]
 *
 * @param modifier [Modifier]
 * @param onNavigate 画面遷移
 */
@Composable
fun HelloCardList(
    modifier: Modifier = Modifier,
    onNavigate: (MainScreenNavigationLinks) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStore = remember { context.dataStore.data }.collectAsState(initial = null)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        // はじめまして画面誘導カード
        if (dataStore.value?.contains(SettingKeyObject.IS_HIDE_HELLO_CARD) == false) {
            HelloCard(
                modifier = Modifier.fillMaxWidth(),
                onHelloClick = { onNavigate(MainScreenNavigationLinks.HelloScreen) },
                onClose = {
                    // もう出さない
                    scope.launch {
                        context.dataStore.edit { it[SettingKeyObject.IS_HIDE_HELLO_CARD] = true }
                    }
                }
            )
        }

        // 部分的な画面共有出来るよ
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && dataStore.value?.contains(SettingKeyObject.IS_HIDE_HELLO_PARTIAL_MIRRORING_CARD) == false) {
            HelloPartialMirroringCard(
                modifier = Modifier.fillMaxWidth(),
                onClose = {
                    scope.launch {
                        context.dataStore.edit { it[SettingKeyObject.IS_HIDE_HELLO_PARTIAL_MIRRORING_CARD] = true }
                    }
                }
            )
        }
    }
}