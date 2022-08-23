package io.github.takusan23.zeromirror.websocket

import android.annotation.SuppressLint
import android.content.Context
import android.media.projection.MediaProjection
import android.os.Build
import io.github.takusan23.hlsserver.Server
import io.github.takusan23.zeromirror.data.MirroringSettingData
import io.github.takusan23.zeromirror.media.InternalAudioEncoder
import io.github.takusan23.zeromirror.media.ScreenVideoEncoder
import io.github.takusan23.zeromirror.media.StreamingInterface
import kotlinx.coroutines.*
import java.io.File

/**
 * WebSocketでミラーリングをストリーミングする場合に利用するクラス
 *
 * @param context [Context]
 * @param mirroringSettingData ミラーリング設定情報
 */
class WSStreaming(
    private val context: Context,
    private val mirroringSettingData: MirroringSettingData,
) : StreamingInterface {

    /** 生成したファイルの管理 */
    private lateinit var wsContentManager: WSContentManager

    /** コンテナに書き込むクラス */
    private lateinit var wsContainerWriter: WSContainerWriter

    /** 映像エンコーダー */
    private lateinit var screenVideoEncoder: ScreenVideoEncoder

    /** 内部音声エンコーダー。利用しない場合は null になる */
    private var internalAudioEncoder: InternalAudioEncoder? = null

    /** サーバー これだけ別モジュール */
    private lateinit var server: Server

    /** 内部音声エンコーダーを初期化したか */
    private val isInitializedInternalAudioEncoder: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && internalAudioEncoder != null

    override suspend fun init(
        parentFolder: File,
        mediaProjection: MediaProjection,
        videoHeight: Int,
        videoWidth: Int,
    ) {
        // 前回のデータを消す
        wsContentManager = WSContentManager(parentFolder, VIDEO_FILE_NAME).apply {
            deleteGenerateFile()
        }
        // コンテナファイルに書き込むやつ
        val tempFile = wsContentManager.generateTempFile(TEMP_VIDEO_FILENAME)
        wsContainerWriter = WSContainerWriter(tempFile)
        // エンコーダーの用意
        screenVideoEncoder = ScreenVideoEncoder(context.resources.configuration.densityDpi, mediaProjection).apply {
            prepareEncoder(
                videoWidth = videoWidth,
                videoHeight = videoHeight,
                bitRate = mirroringSettingData.videoBitRate,
                frameRate = mirroringSettingData.videoFrameRate,
                isVp9 = mirroringSettingData.isVP9,
            )
        }
        // 内部音声を一緒にエンコードする場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mirroringSettingData.isRecordInternalAudio) {
            internalAudioEncoder = InternalAudioEncoder(mediaProjection).apply {
                prepareEncoder(
                    bitRate = mirroringSettingData.audioBitRate,
                    isOpus = mirroringSettingData.isVP9,
                )
            }
        }
        // サーバー開始
        server = Server(
            portNumber = mirroringSettingData.portNumber,
            hostingFolder = wsContentManager.outputFolder
        ).apply {
            startServer()
        }
    }

    override suspend fun startEncode() = withContext(Dispatchers.Default) {
        // 初回用
        wsContainerWriter.createContainer(wsContentManager.generateNewFile().path)
        // 画面録画エンコーダー
        launch {
            screenVideoEncoder.start(
                onOutputBufferAvailable = { byteBuffer, bufferInfo -> wsContainerWriter.writeVideo(byteBuffer, bufferInfo) },
                onOutputFormatAvailable = {
                    wsContainerWriter.setVideoTrack(it)
                    // 開始する
                    wsContainerWriter.start()
                }
            )
        }
        // 内部音声エンコーダー
        if (isInitializedInternalAudioEncoder) {
            launch {
                internalAudioEncoder?.start(
                    onOutputBufferAvailable = { byteBuffer, bufferInfo -> wsContainerWriter.writeAudio(byteBuffer, bufferInfo) },
                    onOutputFormatAvailable = {
                        wsContainerWriter.setAudioTrack(it)
                        // ここでは start が呼べない、なぜなら音声が再生されてない場合は何もエンコードされないから
                    }
                )
            }
        }
        // コルーチンの中なので whileループ できます
        while (isActive) {
            // intervalMs 秒待機したら新しいファイルにする
            delay(mirroringSettingData.intervalMs)
            // クライアント側にWebSocketでファイルが出来たことを通知する
            val publishFile = wsContainerWriter.stopAndRelease()
            server.updateVideoFileName(publishFile.name)
            // エンコーダー内部で持っている時間をリセットする
            screenVideoEncoder.resetInternalTime()
            if (isInitializedInternalAudioEncoder) {
                internalAudioEncoder?.resetInternalTime()
            }
            // それぞれ格納するファイルを用意
            wsContainerWriter.createContainer(wsContentManager.generateNewFile().path)
            wsContainerWriter.start()
        }
    }

    @SuppressLint("NewApi")
    override fun release() {
        wsContainerWriter.release()
        server.stopServer()
        screenVideoEncoder.release()
        internalAudioEncoder?.release()
    }

    companion object {
        /** クライアントに見せる最終的な動画のファイル名、拡張子はあとから付ける */
        private const val VIDEO_FILE_NAME = "file_"

        /** mp4で moovブロック 移動前のファイル名、MediaMuxerでシーク可能にするために一時的に使う */
        private const val TEMP_VIDEO_FILENAME = "temp_video_file"
    }

}