package io.github.takusan23.zeromirror.ui.screen

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** メイン画面の遷移先 */
sealed interface MainScreenNavigationLinks : NavKey {
    /** ホーム画面 */
    @Serializable
    data object HomeScreen : MainScreenNavigationLinks

    /** Android 17 以上。ACCESS_LOCAL_NETWORK 権限を貰う画面 */
    @Serializable
    data object PermissionScreen : MainScreenNavigationLinks

    /** アプリの説明画面 */
    @Serializable
    data object HelloScreen : MainScreenNavigationLinks

    /** 設定画面 */
    @Serializable
    data object SettingScreen : MainScreenNavigationLinks

    /** 画面共有の設定 */
    @Serializable
    data object SettingMirroringSettingScreen : MainScreenNavigationLinks

    /** このアプリについて */
    @Serializable
    data object SettingAboutSettingScreen : MainScreenNavigationLinks

    /** ライセンス */
    @Serializable
    data object SettingLicenseSettingScreen : MainScreenNavigationLinks
}