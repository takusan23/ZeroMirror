package io.github.takusan23.zeromirror.dash

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.os.Build
import io.github.takusan23.hlsserver.Server
import io.github.takusan23.zeromirror.data.MirroringSettingData
import io.github.takusan23.zeromirror.media.InternalAudioEncoder
import io.github.takusan23.zeromirror.media.ScreenVideoEncoder
import io.github.takusan23.zeromirror.media.StreamingInterface
import io.github.takusan23.zerowebm.ZeroWebM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        dashContentManager = DashContentManager(
            parentFolder = parentFolder,
            audioPrefixName = AUDIO_FILE_PREFIX_NAME,
            videoPrefixName = VIDEO_FILE_PREFIX_NAME
        ).apply { deleteGenerateFile() }
        // コンテナファイルに書き込むやつ
        // val tempFile = dashContentManager.generateTempFile(TEMP_VIDEO_FILENAME)
        // dashContainerWriter = DashContainerWriter(tempFile)
        // コーデックにVP8使う場合、基本VP9でいいと思う
        val isVP8 = mirroringSettingData.isVP8
        // エンコーダーの用意
        screenVideoEncoder = ScreenVideoEncoder(context.resources.configuration.densityDpi, mediaProjection).apply {
            // VP9 エンコーダーだと画面解像度を入れると失敗する。1280x720 / 1920x1080 だと成功する
            prepareEncoder(
                videoWidth = mirroringSettingData.videoWidth,
                videoHeight = mirroringSettingData.videoHeight,
                bitRate = mirroringSettingData.videoBitRate,
                frameRate = mirroringSettingData.videoFrameRate,
                codecName = if (isVP8) MediaFormat.MIMETYPE_VIDEO_VP8 else MediaFormat.MIMETYPE_VIDEO_VP9,
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
        // MPEG-DASH の初期化セグメントを作成する
        // 映像と音声は別々の WebM で配信されるのでそれぞれ作る
        if (isInitializedInternalAudioEncoder) {
            dashContentManager.createFile(AUDIO_INIT_SEGMENT_FILENAME).also { init ->
                zeroWebMWriter.createAudioInitSegment(
                    filePath = init.path,
                    channelCount = 2,
                    samplingRate = 48_000
                )
            }
        }
        dashContentManager.createFile(VIDEO_INIT_SEGMENT_FILENAME).also { init ->
            zeroWebMWriter.createVideoInitSegment(
                filePath = init.path,
                codecName = if (isVP8) ZeroWebM.VP8_CODEC_NAME else ZeroWebM.VP9_CODEC_NAME,
                videoWidth = mirroringSettingData.videoWidth,
                videoHeight = mirroringSettingData.videoHeight
            )
        }
        // MPEG-DASHのマニフェストファイルをホスティングする
        dashContentManager.createFile(MANIFEST_FILENAME).apply {
            writeText(DashManifestTool.createManifest(
                fileIntervalSec = (mirroringSettingData.intervalMs / 1_000).toInt(),
                hasAudio = mirroringSettingData.isRecordInternalAudio,
                isVP8 = isVP8
            ))
        }
        // サーバー開始
        server = Server(
            portNumber = mirroringSettingData.portNumber,
            hostingFolder = dashContentManager.outputFolder,
            indexHtml = INDEX_HTML
        ).apply { startServer() }
    }

    override suspend fun startEncode() = withContext(Dispatchers.Default) {
        // MediaMuxer起動
        // dashContainerWriter.resetOrCreateContainerFile()
        // 画面録画エンコーダー、ファイル保存処理
        launch {
            var prevTime = System.currentTimeMillis()
            val intervalMs = mirroringSettingData.intervalMs
            screenVideoEncoder.start(
                onOutputBufferAvailable = { byteBuffer, bufferInfo ->
                    zeroWebMWriter.appendVideoEncodeData(byteBuffer, bufferInfo)
                    // 定期的に動画ファイルを作る処理
                    // 多分別スレッドとかでやると映像が乱れるのでここに書かないとだめ？
                    if ((System.currentTimeMillis() - prevTime) > intervalMs) {
                        dashContentManager.createIncrementVideoFile { segment ->
                            zeroWebMWriter.createVideoMediaSegment(segment.path)
                        }
                        prevTime = System.currentTimeMillis()
                    }
                },
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
                var prevTime = System.currentTimeMillis()
                val intervalMs = mirroringSettingData.intervalMs
                internalAudioEncoder?.start(
                    onOutputBufferAvailable = { byteBuffer, bufferInfo ->
                        zeroWebMWriter.appendAudioEncodeData(byteBuffer, bufferInfo)
                        if ((System.currentTimeMillis() - prevTime) > intervalMs) {
                            dashContentManager.createIncrementAudioFile { segment ->
                                zeroWebMWriter.createAudioMediaSegment(segment.path)
                            }
                            prevTime = System.currentTimeMillis()
                        }
                    },
                    onOutputFormatAvailable = {
                        // dashContainerWriter.setAudioTrack(it)
                        // ここでは start が呼べない、なぜなら音声が再生されてない場合は何もエンコードされないから
                    }
                )
            }
        }
/*
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
            dashContentManager.createIncrementAudioFile { segment ->
                zeroWebMWriter.createAudioMediaSegment(segment.path)
            }
            dashContentManager.createIncrementVideoFile { segment ->
                zeroWebMWriter.createVideoMediaSegment(segment.path)
            }
        }
*/
    }

    @SuppressLint("NewApi")
    override fun release() {
        // dashContainerWriter.release()
        server.stopServer()
        screenVideoEncoder.release()
        internalAudioEncoder?.release()
/*
        GlobalScope.launch {
            dashContentManager.createIncrementFile().also { file ->
                zeroWebMWriter.createInitSegment(file.path)
                zeroWebMWriter.createMediaSegment(file.path)
            }
        }
*/
    }

    companion object {
        /** 音声メディアセグメントの名前 */
        private const val AUDIO_FILE_PREFIX_NAME = "audio"

        /** 映像メディアセグメントの名前 */
        private const val VIDEO_FILE_PREFIX_NAME = "video"

        /** 音声の初期化セグメントの名前 */
        private const val AUDIO_INIT_SEGMENT_FILENAME = "audio_init.webm"

        /** 映像の初期化セグメントの名前 */
        private const val VIDEO_INIT_SEGMENT_FILENAME = "video_init.webm"

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
        <p>再生されない場合は何回か再読み込みしてしたり、シークバーを先頭に移動させてみてください。</p>
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