package io.github.takusan23.zeromirror

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.window.layout.WindowMetricsCalculator
import io.github.takusan23.zeromirror.dash.DashStreaming
import io.github.takusan23.zeromirror.data.MirroringSettingData
import io.github.takusan23.zeromirror.data.StreamingType
import io.github.takusan23.zeromirror.media.StreamingInterface
import io.github.takusan23.zeromirror.tool.IpAddressTool
import io.github.takusan23.zeromirror.tool.QrCodeGeneratorTool
import io.github.takusan23.zeromirror.websocket.WSStreaming
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ミラーリングサービス
 */
class ScreenMirrorService : Service() {
    /** コルーチンスコープ */
    private val coroutineScope = CoroutineScope(Job())

    // ミラーリングで使う
    private val mediaProjectionManager by lazy { getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    private lateinit var mediaProjection: MediaProjection

    /** ミラーリング情報、ビットレートとか */
    private lateinit var mirroringSettingData: MirroringSettingData

    /** ミラーリングするやつ */
    private lateinit var streaming: StreamingInterface

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val resultCode = intent?.getIntExtra(KEY_INTENT_RESULT_CODE, -1)
        val resultData = intent?.getParcelableExtra<Intent>(KEY_INTENT_RESULT_INTENT)
        val displayHeight = intent?.getIntExtra(KEY_INTENT_HEIGHT, 0) ?: 0
        val displayWidth = intent?.getIntExtra(KEY_INTENT_WIDTH, 0) ?: 0

        coroutineScope.launch {
            // 通知発行
            notifyForegroundNotification()
            // 設定を読み出す
            mirroringSettingData = MirroringSettingData.loadDataStore(this@ScreenMirrorService).first()

            // 起動できる場合は起動
            if (resultCode != null && resultData != null) {
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)
                // ミラーリング用意
                streaming = if (mirroringSettingData.streamingType == StreamingType.MpegDash) {
                    DashStreaming(this@ScreenMirrorService, mirroringSettingData)
                } else {
                    WSStreaming(this@ScreenMirrorService, mirroringSettingData)
                }.apply {
                    init(getExternalFilesDir(null)!!, mediaProjection, displayHeight, displayWidth)
                }
                // エンコーダー開始
                launch { streaming.startEncode() }
            } else {
                stopSelf()
            }

            // IPアドレスを通知として出す
            IpAddressTool.collectIpAddress(this@ScreenMirrorService).onEach { ipAddress ->
                val url = "http://$ipAddress:${mirroringSettingData.portNumber}"
                notifyForegroundNotification(url = url, contentText = "${getString(R.string.ip_address)}：$url")
                Log.d(TAG, url)
            }.launchIn(coroutineScope)
        }
        return START_NOT_STICKY
    }

    @SuppressLint("NewApi")
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        streaming.release()
        mediaProjection.stop()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    /**
     * フォアグラウンドサービスの通知を発行する
     *
     * @param contentText 通知本文
     * @param url QRコードにするURL、あるなら
     */
    private fun notifyForegroundNotification(
        contentText: String = getString(R.string.zeromirror_service_notification_content),
        url: String? = null,
    ) {
        // 通知チャンネル
        val notificationManagerCompat = NotificationManagerCompat.from(this)
        //通知チャンネルが存在しないときは登録する
        if (notificationManagerCompat.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            val channel = NotificationChannelCompat.Builder(NOTIFICATION_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW).apply {
                setName(getString(R.string.notification_channel_title))
            }.build()
            notificationManagerCompat.createNotificationChannel(channel)
        }
        //通知作成
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).also { builder ->
            builder.setContentTitle(getString(R.string.zeromirror_service_notification_title))
            builder.setContentText(contentText)
            builder.setSmallIcon(R.drawable.zeromirror_android)
            url?.let { QrCodeGeneratorTool.generateQrCode(it) }
                ?.also { bitmap -> builder.setLargeIcon(bitmap) }
        }.build()
        startForeground(NOTIFICATION_ID, notification)
    }

    companion object {
        private val TAG = ScreenMirrorService::class.simpleName

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