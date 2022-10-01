package io.github.takusan23.zeromirror.tool

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** 権限関連 */
object PermissionTool {

    /**
     * 内部音声を収録するためにはAndroid 10 以降が必須
     *
     * @return Android 10 以降ならtrue
     */
    val isAndroidQAndHigher: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /**
     * Android 13 以上の場合は true
     * 通知権限を貰う必要があるか。フォアグラウンドサービス実行時はいらないけど、通知領域に出すためには必要、
     */
    val isAndroidTiramisuAndHigher: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /**
     * マイク権限があるかどうかを確認する
     *
     * @param context [Context]
     * @return 権限があればtrue
     */
    fun isGrantedRecordPermission(context: Context) = isGrantedPermission(context, android.Manifest.permission.RECORD_AUDIO)

    /**
     * 通知権限があるかどうかを確認する
     * Android 13 以降のみ利用
     *
     * @param context [context]
     * @return Android 13 未満は一律 false
     */
    fun isGrantedPostNotificationPermission(context: Context) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        isGrantedPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
    } else {
        false
    }

    /**
     * 権限があればtrue
     *
     * @param context [Context]
     * @param permission 権限の名前
     */
    private fun isGrantedPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

}