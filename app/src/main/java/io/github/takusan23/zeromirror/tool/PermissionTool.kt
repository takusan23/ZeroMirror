package io.github.takusan23.zeromirror.tool

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** 権限関連 */
object PermissionTool {

    /**
     * マイク権限があるかどうかを確認する
     *
     * @param context [Context]
     * @return 権限があればtrue
     */
    fun isGrantedRecordPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 内部音声を収録するためにはAndroid 10 以降が必須
     *
     * @return Android 10 以降ならtrue
     */
    fun isAndroidQAndHigher(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }
}