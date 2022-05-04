package io.github.takusan23.zeromirror

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.view.Surface
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.window.layout.WindowMetricsCalculator
import io.github.takusan23.zeromirror.media.MediaContainer
import io.github.takusan23.zeromirror.media.VideoEncoder
import io.github.takusan23.zeromirror.tool.UniqueFileTool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

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

    /** エンコーダー */
    private val videoEncoder by lazy { VideoEncoder() }
    private var encoderSurface: Surface? = null

    /** mp4に書き込むクラス */
    private var mediaContainer: MediaContainer? = null

    // 画面サイズ
    private var displayHeight = 0
    private var displayWidth = 0

    /** フレームレート */
    private val frameRate = 30

    /** ビットレート */
    private val bitRate = 1_000_000 // 1Mbps

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
            setupScreenMirroring(resultCode, resultData)
        } else {
            stopSelf()
        }

        // エンコーダーを開始
        coroutineScope.launch {
            startEncode()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        videoEncoder.release()
        mediaContainer?.release()
        mediaProjection?.stop()
        virtualDisplay?.release()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    /** ミラーリング内容をエンコードして動画にする */
    private suspend fun startEncode() {
        // コンテナフォーマットに格納するクラス、mp4生成器
        mediaContainer = MediaContainer(uniqueFileTool)
        // 前回のMediaFormat
        var mediaFormat: MediaFormat? = null
        // mp4作成日時
        var createdDateMs = System.currentTimeMillis()

        videoEncoder.start(
            onOutputBufferAvailable = { byteBuffer, bufferInfo ->
                // データを書き込む
                mediaContainer!!.writeVideoData(byteBuffer, bufferInfo)

                // 次のファイルにすべきならする
                if (intervalMs < System.currentTimeMillis() - createdDateMs) {
                    createdDateMs = System.currentTimeMillis()
                    // ファイルを完成させる
                    val resultFile = mediaContainer!!.release()
                    mediaContainer!!.createContainer()
                    // MediaFormatをセットする
                    mediaContainer!!.setVideoFormat(mediaFormat!!)
                    println("次のファイルになりました ${resultFile.path}")
                }
            },
            onOutputFormatChanged = { format ->
                mediaFormat = format
                mediaContainer!!.setVideoFormat(format)
            }
        )
    }

    /**
     * ミラーリング開始
     *
     * @param resultCode onActivityResult のときのCode
     * @param resultData onActivityResult のときのIntent
     */
    private fun setupScreenMirroring(resultCode: Int, resultData: Intent) {
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)
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
            setContentText("ぜろみらー起動中")
            setContentTitle(contentText)
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