package io.github.takusan23.zeromirror.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.compose.ZeroMirrorTheme
import io.github.takusan23.zeromirror.ui.screen.setting.AboutSettingScreen
import io.github.takusan23.zeromirror.ui.screen.setting.LicenseScreen
import io.github.takusan23.zeromirror.ui.screen.setting.MirroringSettingScreen
import io.github.takusan23.zeromirror.ui.screen.setting.SettingScreen
import io.github.takusan23.zeromirror.ui.tool.SetNavigationBarColor
import io.github.takusan23.zeromirror.ui.tool.SetStatusBarColor

/** メイン画面、Activityに置いてる画面です */
@Composable
fun MainScreen() {
    ZeroMirrorTheme {
        // システムバーの色
        SetStatusBarColor()
        SetNavigationBarColor()

        // メイン画面のルーティング
        val mainScreenNavigation = rememberNavController()

        Surface(color = MaterialTheme.colorScheme.background) {
            NavHost(navController = mainScreenNavigation, startDestination = MainScreenNavigationLinks.HomeScreen) {
                // 最初に出す画面
                composable(MainScreenNavigationLinks.HelloScreen) {
                    HelloScreen(
                        onNextClick = { mainScreenNavigation.popBackStack() },
                        onBack = { mainScreenNavigation.popBackStack() }
                    )
                }
                // ホーム画面
                composable(MainScreenNavigationLinks.HomeScreen) {
                    HomeScreen(
                        onSettingClick = { mainScreenNavigation.navigate(MainScreenNavigationLinks.SettingScreen) },
                        onNavigate = { mainScreenNavigation.navigate(it) }
                    )
                }
                // 設定画面
                composable(MainScreenNavigationLinks.SettingScreen) {
                    SettingScreen(
                        onBack = { mainScreenNavigation.popBackStack() },
                        onNavigate = { mainScreenNavigation.navigate(it) }
                    )
                }
                // 画面共有設定
                composable(MainScreenNavigationLinks.SettingMirroringSettingScreen) {
                    MirroringSettingScreen(onBack = { mainScreenNavigation.popBackStack() })
                }
                // このアプリについて
                composable(MainScreenNavigationLinks.SettingAboutSettingScreen) {
                    AboutSettingScreen(onBack = { mainScreenNavigation.popBackStack() })
                }
                // ライセンス
                composable(MainScreenNavigationLinks.SettingLicenseSettingScreen) {
                    LicenseScreen(onBack = { mainScreenNavigation.popBackStack() })
                }
            }
        }
    }
}