package io.github.takusan23.zeromirror.media

import android.media.projection.MediaProjection
import java.io.File

/**
 * 以下のクラスの基礎インターフェース
 *
 * [io.github.takusan23.zeromirror.dash.DashStreaming]
 * [io.github.takusan23.zeromirror.websocket.WSStreaming]
 */
interface StreamingInterface {

    /**
     * 初期化する
     *
     * @param parentFolder 保存先
     * @param mediaProjection [MediaProjection]
     * @param videoHeight 動画の高さ
     * @param videoWidth 動画の幅
     */
    suspend fun init(
        parentFolder: File,
        mediaProjection: MediaProjection,
        videoHeight: Int,
        videoWidth: Int,
    )

    /**
     * 映像、内部音声のエンコードをして、ファイルを連続で生成していく。
     * エンコード中はずっと一時停止します。
     */
    suspend fun startEncode()

    /** リソース開放 */
    fun release()
}