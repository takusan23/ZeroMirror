package io.github.takusan23.zeromirror.media

import android.media.projection.MediaProjection
import io.github.takusan23.zeromirror.data.MirroringSettingData
import java.io.File

/**
 * 以下のクラスの基礎インターフェース。
 * [destroyEncoder] 関数以外は呼ばれない場合があります。（ミラーリング配信しなかった場合は [prepareEncoder] が呼ばれない）
 *
 * [io.github.takusan23.zeromirror.dash.DashStreaming]
 * [io.github.takusan23.zeromirror.websocket.WSStreaming]
 */
interface StreamingInterface {

    /** 保存先 */
    val parentFolder: File

    /** ミラーリング設定 */
    val mirroringSettingData: MirroringSettingData

    /**
     * ブラウザ視聴のためのWebサーバーを開始する。
     * ミラーリング開始前でもアクセスできるように。
     */
    suspend fun startServer()

    /**
     * エンコーダーの初期化をする
     *
     * @param mediaProjection [MediaProjection]
     * @param videoHeight 動画の高さ
     * @param videoWidth 動画の幅
     */
    suspend fun prepareEncoder(
        mediaProjection: MediaProjection,
        videoHeight: Int,
        videoWidth: Int,
    )

    /**
     * 映像、内部音声のエンコードをして、ファイルを連続で生成していく。
     * エンコード中はすっと一時停止します。
     * ミラーリング終了時はコルーチンがキャンセルされるので、try-finally でリソース開放してください。
     */
    suspend fun startEncode()

    /** 破棄時に呼ばれる。[startServer]のリソース開放に */
    fun destroy()
}