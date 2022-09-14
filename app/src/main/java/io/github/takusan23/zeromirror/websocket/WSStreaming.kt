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
            hostingFolder = wsContentManager.outputFolder,
            indexHtml = INDEX_HTML
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

        /**
         * index.html
         * TODO 直書きやめたい
         */
        private const val INDEX_HTML = """
<!DOCTYPE html>
<html lang="ja">

<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ぜろみらーくらいあんと</title>
</head>

<body>
    <div align="center">
        <p id="title">動画データの到着を待っています...</p>
        <p><button id="cache_button">切り替えを安定させる（実験的）</button></p>
        <video id="video_player" width="300" controls autoplay muted></video>
    </div>
</body>

<script>
    //@ts-check

    // 一番上の文字
    const titleElement = document.getElementById('title')
    // 動画プレイヤー
    const videoElement = document.getElementById('video_player')
    // キャッシュボタン
    const cacheButton = document.getElementById('cache_button')

    // 動画をすべてダウンロードしてから再生するか
    let isFullVideoDl = false

    // ボタンを押したら切り替える
    cacheButton.onclick = () => {
        isFullVideoDl = !isFullVideoDl
    }

    // 動画をロードする
    // 引数は 動画のパス
    const loadVideo = (rawUrlOrPreloadedUrl) => {
        // 一つ前の動画をロードする
        videoElement.pause()
        videoElement.src = rawUrlOrPreloadedUrl
        videoElement.load()
    }

    // WebSocket 接続を作成
    const socket = new WebSocket(`ws://${'$'}{location.host}/wsvideo`)

    // 接続が開いたときのイベント
    socket.addEventListener('open', function (event) {
        socket.send('WebSocket接続がオープンしました。')
    })

    // メッセージの待ち受け
    socket.addEventListener('message', function (event) {
        console.log('動画データが到着しました。', event.data)
        // 新しい動画の相対URL
        const url = JSON.parse(event.data)["url"]
        if (isFullVideoDl) {
            // ダウンロードしてから再生する場合
            fetch(url)
                .then(response => response.blob())
                .then(blob => {
                    // 前回の BlobURL を無効にする
                    URL.revokeObjectURL(videoElement.src)
                    // 今回の動画の BlobURL を作成する
                    const rawUrlOrPreloadedUrl = URL.createObjectURL(blob)
                    loadVideo(rawUrlOrPreloadedUrl)
                })
        } else {
            // そのまま渡す
            loadVideo(url)
        }
        // 上の文字を消す
        titleElement.style.display = 'none'
    })

</script>

</html>
"""
    }

}