package io.github.takusan23.zeromirror.ui.screen

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import io.github.takusan23.zeromirror.ui.screen.setting.AboutSettingScreen
import io.github.takusan23.zeromirror.ui.screen.setting.LicenseScreen
import io.github.takusan23.zeromirror.ui.screen.setting.MirroringSettingScreen
import io.github.takusan23.zeromirror.ui.screen.setting.SettingScreen

/** メイン画面、Activityに置いてる画面です */
@Composable
fun MainScreen() {
    // メイン画面のルーティング
    val backStack = rememberNavBackStack(MainScreenNavigationLinks.HomeScreen)
    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            // アプリの説明
            entry<MainScreenNavigationLinks.HelloScreen> {
                HelloScreen(
                    onNextClick = { backStack.removeLastOrNull() },
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            // ホーム画面
            entry<MainScreenNavigationLinks.HomeScreen> {
                HomeScreen(
                    onSettingClick = { backStack += MainScreenNavigationLinks.SettingScreen },
                    onNavigate = { backStack += it }
                )
            }
            // 設定画面
            entry<MainScreenNavigationLinks.SettingScreen> {
                SettingScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onNavigate = { backStack += it }
                )
            }
            // 画面共有設定
            entry<MainScreenNavigationLinks.SettingMirroringSettingScreen> {
                MirroringSettingScreen(onBack = { backStack.removeLastOrNull() })
            }
            // このアプリについて
            entry<MainScreenNavigationLinks.SettingAboutSettingScreen> {
                AboutSettingScreen(onBack = { backStack.removeLastOrNull() })
            }
            // ライセンス
            entry<MainScreenNavigationLinks.SettingLicenseSettingScreen> {
                LicenseScreen(onBack = { backStack.removeLastOrNull() })
            }
        }
    )
}