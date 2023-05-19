package io.github.takusan23.zeromirror

import android.annotation.SuppressLint
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * ミラーリングサービス
 */
class ScreenMirroringService : Service() {
    // コルーチン
    private val coroutineScope = CoroutineScope(Job())
    private var mirroringJob: Job? = null
    private val _isScreenMirroring = MutableStateFlow(false)

    /** ミラーリング中かどうか */
    val isScreenMirroring = _isScreenMirroring.asStateFlow()

    /** バインドする */
    private val localBinder = LocalBinder(this)

    // ミラーリングで使う
    private val mediaProjectionManager by lazy { getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    private lateinit var mediaProjection: MediaProjection

    /** ミラーリングするやつ */
    private lateinit var streaming: StreamingInterface

    @SuppressLint("NewApi")
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        streaming.release()
        mediaProjection.stop()
    }

    override fun onBind(intent: Intent): IBinder = localBinder

    /**
     * ミラーリングを開始する
     *
     * @param resultCode onActivityResult でもらえる Code
     * @param resultData onActivityResult でもらえる Intent
     * @param displayHeight 画面の高さ
     * @param displayWidth 画面の幅
     */
    fun startMirroring(
        resultCode: Int,
        resultData: Intent,
        displayHeight: Int,
        displayWidth: Int
    ) {
        coroutineScope.launch {
            // すでに起動中なら終了
            mirroringJob?.cancelAndJoin()
            mirroringJob = launch {
                // フォアグラウンドサービスにするため、通知を出す
                notifyForegroundNotification()
                // 設定を読み出す
                val mirroringSettingData = MirroringSettingData.loadDataStore(this@ScreenMirroringService).first()

                // 起動できる場合は起動
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)
                // ミラーリング用意
                streaming = if (mirroringSettingData.streamingType == StreamingType.MpegDash) {
                    DashStreaming(this@ScreenMirroringService, mirroringSettingData)
                } else {
                    WSStreaming(this@ScreenMirroringService, mirroringSettingData)
                }.apply {
                    init(getExternalFilesDir(null)!!, mediaProjection, displayHeight, displayWidth)
                }

                // エンコーダー開始
                launch { streaming.startEncode() }
                _isScreenMirroring.value = true

                // IPアドレスを通知として出す
                IpAddressTool.collectIpAddress(this@ScreenMirroringService).onEach { ipAddress ->
                    val url = "http://$ipAddress:${mirroringSettingData.portNumber}"
                    notifyForegroundNotification(url = url, contentText = "${getString(R.string.ip_address)}：$url")
                    Log.d(TAG, url)
                }.launchIn(coroutineScope)
            }
        }
    }

    /**
     * ミラーリングを終了する。
     * フォアグラウンドサービスも解除される。
     */
    fun stopMirroring() {
        coroutineScope.launch {
            mirroringJob?.cancelAndJoin()
            ServiceCompat.stopForeground(this@ScreenMirroringService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            _isScreenMirroring.value = false
        }
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

    private class LocalBinder(service: ScreenMirroringService) : Binder() {
        val serviceRef = WeakReference(service)
        val service: ScreenMirroringService
            get() = serviceRef.get()!!
    }

    companion object {
        private val TAG = ScreenMirroringService::class.simpleName

        /** 通知ID */
        private const val NOTIFICATION_ID = 4545

        /** 通知チャンネルID */
        private const val NOTIFICATION_CHANNEL_ID = "running_screen_mirror_service"

        /**
         * ミラーリングサービスとバインドして、サービスのインスタンスを取得する。
         * ライフサイクルを追跡して自動で解除します。
         *
         * @param context [Context]
         * @param lifecycleOwner [LifecycleOwner]
         */
        fun bindScreenMirrorService(context: Context, lifecycleOwner: LifecycleOwner) = callbackFlow {
            val serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val encoderService = (service as LocalBinder).service
                    trySend(encoderService)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    trySend(null)
                }
            }
            // ライフサイクルを監視してバインド、バインド解除する
            val lifecycleObserver = object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    super.onStart(owner)
                    val intent = Intent(context, ScreenMirroringService::class.java)
                    context.startService(intent)
                    context.bindService(intent, serviceConnection, BIND_AUTO_CREATE)
                }

                override fun onStop(owner: LifecycleOwner) {
                    super.onStop(owner)
                    context.unbindService(serviceConnection)
                }
            }
            lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
            awaitClose { lifecycleOwner.lifecycle.removeObserver(lifecycleObserver) }
        }
    }
}