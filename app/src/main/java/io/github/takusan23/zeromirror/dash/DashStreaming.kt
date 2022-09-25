package io.github.takusan23.zeromirror.dash

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
 * MPEG-DASH でミラーリングをストリーミングする場合に利用するクラス。
 * WebM を定期的に切り出して MPEG-DASH で配信する。
 *
 * [init]関数のサイズはVP9の場合は無視されます。
 *
 * @param context [Context]
 * @param mirroringSettingData ミラーリング設定情報
 */
class DashStreaming(
    private val context: Context,
    private val mirroringSettingData: MirroringSettingData,
) : StreamingInterface {
    /** 生成したファイルの管理 */
    private lateinit var dashContentManager: DashContentManager

    /** コンテナに書き込むクラス */
    // private lateinit var dashContainerWriter: DashContainerWriter
    private val zeroWebMWriter = ZeroWebMWriter()

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
        dashContentManager = DashContentManager(parentFolder, VIDEO_FILE_NAME).apply {
            deleteGenerateFile()
        }
        // コンテナファイルに書き込むやつ
        // val tempFile = dashContentManager.generateTempFile(TEMP_VIDEO_FILENAME)
        // dashContainerWriter = DashContainerWriter(tempFile)
        // エンコーダーの用意
        screenVideoEncoder = ScreenVideoEncoder(context.resources.configuration.densityDpi, mediaProjection).apply {
            // VP9 エンコーダーだと画面解像度を入れると失敗する。1280x720 / 1920x1080 だと成功する
            prepareEncoder(
                videoWidth = mirroringSettingData.videoWidth,
                videoHeight = mirroringSettingData.videoHeight,
                bitRate = mirroringSettingData.videoBitRate,
                frameRate = mirroringSettingData.videoFrameRate,
                isVp9 = true,
            )
        }
        // 内部音声を一緒にエンコードする場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mirroringSettingData.isRecordInternalAudio) {
            internalAudioEncoder = InternalAudioEncoder(mediaProjection).apply {
                prepareEncoder(
                    bitRate = mirroringSettingData.audioBitRate,
                    isOpus = true,
                )
            }
        }
        // MPEG-DASHのマニフェストファイルをホスティングする
        dashContentManager.createFile(MANIFEST_FILENAME).apply {
            writeText(DashManifestTool.createManifest(
                fileIntervalSec = (mirroringSettingData.intervalMs / 1_000).toInt(),
                hasAudio = /*mirroringSettingData.isRecordInternalAudio*/ false
            ))
        }
        // MPEG-DASH の初期化セグメントを作成する
        dashContentManager.createFile(INIT_SEGMENT_FILENAME).also { initSegment ->
            zeroWebMWriter.createInitSegment(initSegment.path)
        }
        // サーバー開始
        server = Server(
            portNumber = mirroringSettingData.portNumber,
            hostingFolder = dashContentManager.outputFolder,
            indexHtml = INDEX_HTML
        ).apply {
            startServer()
        }
    }

    override suspend fun startEncode() = withContext(Dispatchers.Default) {
        // MediaMuxer起動
        // dashContainerWriter.resetOrCreateContainerFile()
        // 画面録画エンコーダー
        launch {
            screenVideoEncoder.start(
                onOutputBufferAvailable = { byteBuffer, bufferInfo -> zeroWebMWriter.appendVideoEncodeData(byteBuffer, bufferInfo) },
                onOutputFormatAvailable = {
                    // dashContainerWriter.setVideoTrack(it)
                    // 開始する
                    // dashContainerWriter.start()
                }
            )
        }
        // 内部音声エンコーダー
        if (isInitializedInternalAudioEncoder) {
            launch {
                internalAudioEncoder?.start(
                    onOutputBufferAvailable = { byteBuffer, bufferInfo -> zeroWebMWriter.appendAudioEncodeData(byteBuffer, bufferInfo) },
                    onOutputFormatAvailable = {
                        // dashContainerWriter.setAudioTrack(it)
                        // ここでは start が呼べない、なぜなら音声が再生されてない場合は何もエンコードされないから
                    }
                )
            }
        }
        // セグメントファイルを作る
        // 後は MPEG-DASHプレイヤー側 で定期的に取得してくれる
        while (isActive) {
            // intervalMs 秒待機したら新しいファイルにする
            delay(mirroringSettingData.intervalMs)
//            // 初回時だけ初期化セグメントを作る
//            if (!dashContainerWriter.isGeneratedInitSegment) {
//                dashContentManager.createFile(INIT_SEGMENT_FILENAME).also { initSegment ->
//                    dashContainerWriter.sliceInitSegmentFile(initSegment.path)
//                }
//            }
            // MediaMuxerで書き込み中のファイルから定期的にデータをコピーして（セグメントファイルが出来る）クライアントで再生する
            // この方法だと、MediaMuxerとMediaMuxerからコピーしたデータで二重に容量を使うけど後で考える
            dashContentManager.createIncrementFile().also { segment ->
                zeroWebMWriter.createMediaSegment(segment.path)
            }
        }
    }

    @SuppressLint("NewApi")
    override fun release() {
        // dashContainerWriter.release()
        server.stopServer()
        screenVideoEncoder.release()
        internalAudioEncoder?.release()
    }

    companion object {
        /** クライアントに見せる最終的な動画のファイル名、拡張子はあとから付ける */
        private const val VIDEO_FILE_NAME = "file_"

        /** mp4で moovブロック 移動前のファイル名、MediaMuxerでシーク可能にするために一時的に使う */
        private const val TEMP_VIDEO_FILENAME = "temp_video_file"

        /** 初期化セグメントの名前 */
        private const val INIT_SEGMENT_FILENAME = "init.webm"

        /** マニフェストファイルの名前 */
        private const val MANIFEST_FILENAME = "manifest.mpd"

        /**
         * index.html
         * TODO 直書きを辞める
         */
        private const val INDEX_HTML = """
<!doctype html>
<html>
<head>
    <title>ぜろみらー MPEG-DASH</title>
    <style>
        video {
            width: 640px;
            height: 360px;
        }
    </style>
</head>
<body>
    <div>
        <p>再生されない場合は何回か再読み込みしてみてください。</p>
        <video id="videoPlayer" controls muted autoplay></video>
    </div>
    <script src="https://cdn.dashjs.org/latest/dash.all.debug.js"></script>
    <script>
        (function () {
            var url = "manifest.mpd";
            var player = dashjs.MediaPlayer().create();
            player.initialize(document.querySelector("#videoPlayer"), url, true);
        })();
    </script>
</body>
</html>
"""

    }

}