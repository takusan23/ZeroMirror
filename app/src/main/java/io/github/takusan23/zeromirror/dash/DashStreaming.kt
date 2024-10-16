package io.github.takusan23.zeromirror.dash

import android.content.Context
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.os.Build
import io.github.takusan23.hlsserver.Server
import io.github.takusan23.zeromirror.data.MirroringSettingData
import io.github.takusan23.zeromirror.media.InternalAudioEncoder
import io.github.takusan23.zeromirror.media.ScreenVideoEncoder
import io.github.takusan23.zeromirror.media.StreamingInterface
import io.github.takusan23.zeromirror.tool.PartialMirroringPauseImageTool
import io.github.takusan23.zerowebm.ZeroWebM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * MPEG-DASH でミラーリングをストリーミングする場合に利用するクラス。
 * WebM を定期的に切り出して MPEG-DASH で配信する。
 *
 * [prepareEncoder]関数のサイズはVP9の場合は無視されます。
 *
 * @param context [Context]
 * @param mirroringSettingData ミラーリング設定情報
 */
class DashStreaming(
    private val context: Context,
    override val parentFolder: File,
    override val mirroringSettingData: MirroringSettingData,
) : StreamingInterface {
    /** 生成したファイルの管理 */
    private var dashContentManager: DashContentManager? = null

    /** コンテナに書き込むクラス */
    private var zeroWebMWriter: ZeroWebMWriter? = null

    /** 映像エンコーダー */
    private var screenVideoEncoder: ScreenVideoEncoder? = null

    /** 内部音声エンコーダー。利用しない場合は null になる */
    private var internalAudioEncoder: InternalAudioEncoder? = null

    /** サーバー これだけ別モジュール */
    private var server: Server? = null

    /** 内部音声エンコーダーを初期化したか */
    private val isInitializedInternalAudioEncoder: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && internalAudioEncoder != null

    override suspend fun startServer() = withContext(Dispatchers.Default) {
        // サーバー開始。この段階でブラウザ側の視聴ページは利用可能になる
        server = Server(
            portNumber = mirroringSettingData.portNumber,
            hostingFolder = DashContentManager.getOutputFolder(parentFolder),
            indexHtml = INDEX_HTML
        ).apply { startServer() }
    }

    override suspend fun prepareAndStartEncode(
        mediaProjection: MediaProjection,
        videoHeight: Int,
        videoWidth: Int
    ) = withContext(Dispatchers.Default) {
        // エンコーダーを初期化する
        // コンテンツ管理。前回のデータを消す
        dashContentManager = DashContentManager(
            parentFolder = parentFolder,
            audioPrefixName = AUDIO_FILE_PREFIX_NAME,
            videoPrefixName = VIDEO_FILE_PREFIX_NAME
        ).apply { deleteGenerateFile() }
        // コーデックにVP8使う場合、基本VP9でいいと思う
        val isVP8 = mirroringSettingData.isVP8
        // エンコーダーの用意
        screenVideoEncoder = ScreenVideoEncoder(context.resources.configuration.densityDpi, mediaProjection).apply {
            // MediaProjection コールバックを設定
            registerMediaProjectionCallback(
                onMediaProjectionVisibilityChanged = { /* do nothing */ },
                onMediaProjectionResize = { _, _ -> /* do nothing */ },
                onMediaProjectionStop = {
                    // このコルーチンスコープをキャンセルさせる。並列で動いているエンコーダー等も終了する
                    this@withContext.cancel()
                }
            )
            // VP9 エンコーダーだと画面解像度を入れると失敗する。1280x720 / 1920x1080 だと成功する
            prepareEncoder(
                videoWidth = mirroringSettingData.videoWidth,
                videoHeight = mirroringSettingData.videoHeight,
                bitRate = mirroringSettingData.videoBitRate,
                frameRate = mirroringSettingData.videoFrameRate,
                isMirroringExternalDisplay = mirroringSettingData.isMirroringExternalDisplay,
                codecName = if (isVP8) MediaFormat.MIMETYPE_VIDEO_VP8 else MediaFormat.MIMETYPE_VIDEO_VP9,
                altImageBitmap = PartialMirroringPauseImageTool.generatePartialMirroringPauseImage(context, mirroringSettingData.videoWidth, mirroringSettingData.videoHeight)
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
        zeroWebMWriter = ZeroWebMWriter()
        val zeroWebMWriter = zeroWebMWriter!!
        val dashContentManager = dashContentManager!!
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
            writeText(
                DashManifestTool.createManifest(
                    fileIntervalSec = (mirroringSettingData.intervalMs / 1_000).toInt(),
                    hasAudio = mirroringSettingData.isRecordInternalAudio,
                    isVP8 = isVP8
                )
            )
        }

        // エンコード開始。終わるまで一時停止する
        // 画面録画エンコーダー、ファイル保存処理
        val videoEncoderJob = launch {
            try {
                screenVideoEncoder!!.start(
                    onOutputBufferAvailable = { byteBuffer, bufferInfo ->
                        zeroWebMWriter.appendVideoEncodeData(byteBuffer, bufferInfo)
                    },
                    onOutputFormatAvailable = {
                        // do nothing
                    }
                )
            } finally {
                screenVideoEncoder!!.destroy()
            }
        }
        // 内部音声エンコーダー
        val audioEncoderJob = if (isInitializedInternalAudioEncoder) {
            launch {
                try {
                    internalAudioEncoder?.start(
                        onOutputBufferAvailable = { byteBuffer, bufferInfo ->
                            zeroWebMWriter.appendAudioEncodeData(byteBuffer, bufferInfo)
                        },
                        onOutputFormatAvailable = {
                            // do nothing
                        }
                    )
                } finally {
                    internalAudioEncoder?.release()
                }
            }
        } else null
        // ファイル保存処理
        val segmentFileCreateJob = launch {
            // 初回時
            delay(mirroringSettingData.intervalMs)
            // 定期的にファイルを作り直して、MPEG-DASH で使うセグメントファイルを連番で作る
            while (isActive) {
                // ファイル保存にかかる時間を測る
                val time = measureTimeMillis {
                    // 新しいセグメントファイルを作る
                    // ブラウザがこのファイルをアクセスしに来る
                    // 一つファイル作るのに 50ms くらいかかるので並列にしてみる
                    listOf(
                        launch {
                            dashContentManager.createIncrementAudioFile { segment ->
                                zeroWebMWriter.createAudioMediaSegment(segment.path)
                            }
                        },
                        launch {
                            dashContentManager.createIncrementVideoFile { segment ->
                                zeroWebMWriter.createVideoMediaSegment(segment.path)
                            }
                        }
                    ).joinAll()
                }
                // ファイル保存にかかった時間分引かないと、保存にかかる時間がちりつもして、セグメントファイルの生成が間に合わなくなる
                // MPEG-DASH は多分？開始時間から逆算してセグメントファイルをリクエストするので、時間通りにセグメントファイルを作る必要がある
                delay(mirroringSettingData.intervalMs - time)
            }
        }
        // キャンセルされるまで join で待機する
        videoEncoderJob.join()
        audioEncoderJob?.join()
        segmentFileCreateJob.join()
    }

    override fun destroy() {
        server?.stopServer()
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