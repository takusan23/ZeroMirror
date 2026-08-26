package io.github.takusan23.zeromirror.ui.screen

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
    val context = LocalContext.current
    fun getFirstScreenLink() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN && ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED) {
        // Android 17 以降は LAN を使うアプリには権限が必要
        MainScreenNavigationLinks.PermissionScreen
    } else {
        MainScreenNavigationLinks.HomeScreen
    }

    // メイン画面のルーティング
    val backStack = rememberNavBackStack(getFirstScreenLink())
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
            // 権限
            entry<MainScreenNavigationLinks.PermissionScreen> {
                PermissionScreen(onGranted = {
                    backStack += MainScreenNavigationLinks.HomeScreen
                    backStack -= MainScreenNavigationLinks.PermissionScreen
                })
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