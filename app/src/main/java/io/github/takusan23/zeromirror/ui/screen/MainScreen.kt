package io.github.takusan23.zeromirror.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.example.compose.ZeroMirrorTheme
import io.github.takusan23.zeromirror.setting.SettingKeyObject
import io.github.takusan23.zeromirror.setting.dataStore
import io.github.takusan23.zeromirror.ui.screen.setting.AboutSettingScreen
import io.github.takusan23.zeromirror.ui.screen.setting.LicenseScreen
import io.github.takusan23.zeromirror.ui.screen.setting.MirroringSettingScreen
import io.github.takusan23.zeromirror.ui.screen.setting.SettingScreen
import io.github.takusan23.zeromirror.ui.tool.SetNavigationBarColor
import io.github.takusan23.zeromirror.ui.tool.SetStatusBarColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** メイン画面、Activityに置いてる画面です */
@Composable
fun MainScreen() {
    ZeroMirrorTheme {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        // システムバーの色
        SetStatusBarColor()
        SetNavigationBarColor()

        // メイン画面のルーティング
        val mainScreenNavigation = rememberNavController()

        // 初回時のみ出す はじめまして画面 を出すかどうか
        LaunchedEffect(key1 = Unit, block = {
            // まだ表示したことないので出す
            val isAlreadyFirstScreen = context.dataStore.data.first()[SettingKeyObject.IS_ALREADY_FIRST_SCREEN] ?: false
            if (!isAlreadyFirstScreen) {
                mainScreenNavigation.navigate(MainScreenNavigationLinks.HelloScreen)
            }
        })

        Surface(color = MaterialTheme.colorScheme.background) {
            NavHost(navController = mainScreenNavigation, startDestination = MainScreenNavigationLinks.HomeScreen) {
                // 最初に出す画面
                composable(MainScreenNavigationLinks.HelloScreen) {
                    HelloScreen(onNextClick = {
                        // 戻るキーを押しても最初の画面に戻らないように inclusive を true にする
                        mainScreenNavigation.navigate(
                            route = MainScreenNavigationLinks.HomeScreen,
                            navOptions = navOptions { this.popUpTo(MainScreenNavigationLinks.HelloScreen) { this.inclusive = true } }
                        )
                        // 表示したから保存
                        scope.launch {
                            context.dataStore.edit { it[SettingKeyObject.IS_ALREADY_FIRST_SCREEN] = true }
                        }
                    })
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