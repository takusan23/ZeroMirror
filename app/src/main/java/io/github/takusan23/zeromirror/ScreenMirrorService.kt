package io.github.takusan23.zeromirror

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.window.layout.WindowMetricsCalculator
import io.github.takusan23.hlsserver.Server
import io.github.takusan23.zeromirror.media.AudioEncoder
import io.github.takusan23.zeromirror.media.MediaContainer
import io.github.takusan23.zeromirror.media.VideoEncoder
import io.github.takusan23.zeromirror.tool.IpAddressTool
import io.github.takusan23.zeromirror.tool.UniqueFileTool
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * ミラーリングサービス
 */
class ScreenMirrorService : Service() {
    /** コルーチンスコープ */
    private val coroutineScope = CoroutineScope(Job())

    /** ファイル関係 */
    private val uniqueFileTool by lazy { UniqueFileTool(getExternalFilesDir(null)!!, "videofile", "mp4") }

    // ミラーリングで使う
    private val mediaProjectionManager by lazy { getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    /** 映像エンコーダー */
    private val videoEncoder by lazy { VideoEncoder() }
    private var encoderSurface: Surface? = null

    /** 内部音声を取るのに使う */
    private var audioRecord: AudioRecord? = null

    /** 内部音声エンコーダー */
    private val audioEncoder by lazy { AudioEncoder() }

    /** mp4に書き込むクラス */
    private val mediaContainer by lazy { MediaContainer(this, uniqueFileTool) }

    // ポート番号
    private var portNumber = 10_000

    /** サーバー これだけ別モジュール */
    private val server by lazy { Server(portNumber = portNumber, hostingFolder = getExternalFilesDir(null)!!) }

    // 画面サイズ
    private var displayHeight = 0
    private var displayWidth = 0

    /** フレームレート */
    private val frameRate = 30

    /** ビットレート */
    private val bitRate = 5_000_000 // 1Mbps

    /** 何秒間隔でmp4ファイルに切り出すか、ミリ秒 */
    private val intervalMs = 5_000

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val resultCode = intent?.getIntExtra(KEY_INTENT_RESULT_CODE, -1)
        val resultData = intent?.getParcelableExtra<Intent>(KEY_INTENT_RESULT_INTENT)
        displayHeight = intent?.getIntExtra(KEY_INTENT_HEIGHT, 0) ?: 0
        displayWidth = intent?.getIntExtra(KEY_INTENT_WIDTH, 0) ?: 0

        // 今までのファイルを消す
        uniqueFileTool.deleteParentFolderChildren()
        // 通知発行
        notifyForegroundNotification()

        // 起動できる場合は起動
        if (resultCode != null && resultData != null) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)
            // 画面ミラーリング
            setupScreenMirroring()
            // 内部音声を一緒にエンコードする場合
            if (availableInternalAudio()) {
                setupInternalAudioCapture()
            }
        } else {
            stopSelf()
        }

        // IPアドレスを通知として出す
        IpAddressTool.collectIpAddress(this@ScreenMirrorService).onEach { ipAddress ->
            notifyForegroundNotification("IPアドレス：http://$ipAddress:$portNumber")
            println("ここから見れます：http://$ipAddress:$portNumber")
        }.launchIn(coroutineScope)

        // エンコーダーを開始
        coroutineScope.launch {
            startEncode()
        }

        // サーバー開始
        server.startServer()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        videoEncoder.release()
        audioEncoder.release()
        mediaContainer.release()
        mediaProjection?.stop()
        virtualDisplay?.release()
        server.stopServer()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    /** ミラーリング内容をエンコードして動画にする */
    private suspend fun startEncode() = withContext(Dispatchers.Default) {
        // mp4ファイル作成日時
        var createdDateMs = System.currentTimeMillis()

        /** コンテナファイルへデータを入れたら呼ぶ */
        fun onAfterContainerInput() {
            // 次のファイルにすべきならする
            if (intervalMs < System.currentTimeMillis() - createdDateMs) {

                // 多分ちょっと待たないと静的ファイルとして配信できない？
                // ので次のファイル生成時に一個前のデータを送信するようにする
                mediaContainer.getPrevVideoFile()?.also { prevVideoFile ->
                    // クライアント側へ通知する
                    // WebSocketを使っている
                    // server.updateVideoFileName(prevVideoFile.name)
                }

                createdDateMs = System.currentTimeMillis()
                // ファイルを完成させる
                mediaContainer.release()
                val resultFile = mediaContainer.startMix()
                server.updateVideoFileName(resultFile.name)

                mediaContainer.createContainer()
                // 次のファイルの用意のため、MediaFormatをセットする
                mediaContainer.setVideoFormat(videoEncoder.outputVideoFormat.value!!)
                // 内部音声も取る場合は
                if (availableInternalAudio()) {
                    mediaContainer.setAudioFormat(audioEncoder.outputAudioFormat.value!!)
                }
            }
        }

        // 映像エンコーダー開始
        launch {
            videoEncoder.startVideoEncode { byteBuffer, bufferInfo ->
                if (mediaContainer.isVideoStart) {
                    // 書き込む
                    mediaContainer.writeVideoData(byteBuffer, bufferInfo)
                    // 次のファイルに切り替えるか
                    onAfterContainerInput()
                }
            }
        }
        // 内部音声も収録する場合は内部音声用エンコーダーも起動
        if (availableInternalAudio()) {
            launch {
                audioEncoder.startAudioEncode(
                    onRecordInput = { bytes -> audioRecord!!.read(bytes, 0, bytes.size) },
                    onOutputBufferAvailable = { byteBuffer, bufferInfo ->
                        if (mediaContainer.isAudioStart) {
                            // 書き込む
                            mediaContainer.writeAudioData(byteBuffer, bufferInfo)
                            // 次のファイルに切り替えるか
                            onAfterContainerInput()
                        }
                    }
                )
            }
        }

        // 出力フォーマットをMediaMuxerへ入れる、これらはエンコーダーが開始してないと貰えない
        // 最初に流れてくるので
        mediaContainer.setVideoFormat(videoEncoder.outputVideoFormat.filterNotNull().first())
        // 音声エンコーダー
        // 内部音声を収録する場合
        if (availableInternalAudio()) {
            mediaContainer.setAudioFormat(audioEncoder.outputAudioFormat.filterNotNull().first())
        }
    }

    /** ミラーリング用意 */
    private fun setupScreenMirroring() {
        // 初期化
        videoEncoder.prepareEncoder(
            videoWidth = displayWidth,
            videoHeight = displayHeight,
            bitRate = bitRate,
            frameRate = frameRate,
            iFrameInterval = 1
        )
        encoderSurface = videoEncoder.createInputSurface()
        virtualDisplay = createVirtualDisplay()
    }

    /** [VirtualDisplay]を作る、各初期化後に呼ぶ */
    private fun createVirtualDisplay(): VirtualDisplay {
        return mediaProjection!!.createVirtualDisplay(
            "io.github.takusan23.zeromirror",
            displayWidth,
            displayHeight,
            resources.configuration.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            encoderSurface!!,
            null,
            null
        )
    }

    /**
     * Android 10 以降は念願の内部音声を録音出来るようになったので
     *
     * マイク権限（RECORD_AUDIO）があることを確認してください。
     */
    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setupInternalAudioCapture() {
        // 内部音声取るのに使う
        val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!).apply {
            addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            addMatchingUsage(AudioAttributes.USAGE_GAME)
            addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
        }.build()
        val audioFormat = AudioFormat.Builder().apply {
            setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            setSampleRate(44_100)
            setChannelMask(AudioFormat.CHANNEL_IN_MONO)
        }.build()
        audioRecord = AudioRecord.Builder().apply {
            setAudioPlaybackCaptureConfig(playbackConfig)
            setAudioFormat(audioFormat)
        }.build()
        // 音声エンコーダー
        audioEncoder.prepareEncoder()
        // 録音開始
        audioRecord?.startRecording()
    }

    /**
     * 内部音声の収録に対応している場合はtrueを返す
     *
     * @return 内部音声を取る場合はtrue
     */
    private fun availableInternalAudio() =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /**
     * フォアグラウンドサービスの通知を発行する
     *
     * @param contentText 通知本文
     */
    private fun notifyForegroundNotification(contentText: String = "ブラウザから見れます") {
        // 通知チャンネル
        val notificationManagerCompat = NotificationManagerCompat.from(this)
        //通知チャンネルが存在しないときは登録する
        if (notificationManagerCompat.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            val channel = NotificationChannelCompat.Builder(NOTIFICATION_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW).apply {
                setName("ぜろみらー起動中通知")
            }.build()
            notificationManagerCompat.createNotificationChannel(channel)
        }
        //通知作成
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).apply {
            setContentTitle("ぜろみらー起動中")
            setContentText(contentText)
            setSmallIcon(R.drawable.ic_launcher_foreground)
        }.build()
        startForeground(NOTIFICATION_ID, notification)
    }

    companion object {

        /** 通知ID */
        private const val NOTIFICATION_ID = 4545

        /** 通知チャンネルID */
        private const val NOTIFICATION_CHANNEL_ID = "running_screen_mirror_service"

        /** onActivityResult でもらえるCode */
        private const val KEY_INTENT_RESULT_CODE = "key_intent_result_code"

        /** onActivityResult でもらえるIntent */
        private const val KEY_INTENT_RESULT_INTENT = "key_intent_result_intent"

        /** 画面の高さ */
        private const val KEY_INTENT_HEIGHT = "key_intent_height"

        /** 画面の幅 */
        private const val KEY_INTENT_WIDTH = "key_intent_width"

        /**
         * ミラーリングサービスを開始させる
         *
         * @param activity [Activity]、画面サイズがほしいのでActivityです。
         * @param resultCode onActivityResult でもらえるCode
         * @param data onActivityResult でもらえるIntent
         */
        fun startService(activity: Activity, resultCode: Int, data: Intent) {
            // 画面サイズを出す、AndroidXによるバックポート付き
            val metrics = WindowMetricsCalculator.getOrCreate().computeMaximumWindowMetrics(activity)
            val intent = Intent(activity, ScreenMirrorService::class.java).apply {
                putExtra(KEY_INTENT_RESULT_CODE, resultCode)
                putExtra(KEY_INTENT_RESULT_INTENT, data)
                putExtra(KEY_INTENT_HEIGHT, metrics.bounds.height())
                putExtra(KEY_INTENT_WIDTH, metrics.bounds.width())
            }
            ContextCompat.startForegroundService(activity, intent)
        }

        /**
         * ミラーリングサービスを終了させる
         *
         * @param context [Context]
         */
        fun stopService(context: Context) {
            context.stopService(Intent(context, ScreenMirrorService::class.java))
        }
    }
}