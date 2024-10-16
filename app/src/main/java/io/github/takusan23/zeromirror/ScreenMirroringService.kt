package io.github.takusan23.zeromirror

import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.github.takusan23.zeromirror.dash.DashStreaming
import io.github.takusan23.zeromirror.data.MirroringSettingData
import io.github.takusan23.zeromirror.data.StreamingType
import io.github.takusan23.zeromirror.media.StreamingInterface
import io.github.takusan23.zeromirror.tool.IpAddressTool
import io.github.takusan23.zeromirror.tool.QrCodeGeneratorTool
import io.github.takusan23.zeromirror.websocket.WSStreaming
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

/**
 * ミラーリングサービス。
 * バインドして利用する。
 */
class ScreenMirroringService : Service() {
    // コルーチン
    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())
    private var mirroringJob: Job? = null
    private val _isScreenMirroring = MutableStateFlow(false)

    /** バインドする */
    private val localBinder = LocalBinder(this)

    /** DataStore にある設定を読み出す */
    private val mirroringSettingDataFlow by lazy { MirroringSettingData.loadDataStore(this@ScreenMirroringService) }

    // ミラーリングで使う
    private val mediaProjectionManager by lazy { getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    private var mediaProjection: MediaProjection? = null

    /** ミラーリングするやつ */
    private var streaming: StreamingInterface? = null

    /** ミラーリング中かどうか */
    val isScreenMirroring = _isScreenMirroring.asStateFlow()

    override fun onBind(intent: Intent): IBinder = localBinder

    override fun onCreate() {
        super.onCreate()
        // DataStore の設定内容を監視し反映させる
        coroutineScope.launch {
            mirroringSettingDataFlow.collect { mirroringSettingData ->
                // もしミラーリング中なら終了させる
                if (isScreenMirroring.value) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ScreenMirroringService, R.string.zeromirror_service_stop_because_setting_update, Toast.LENGTH_SHORT).show()
                    }
                }
                mirroringJob?.cancelAndJoin()
                streaming?.destroy()

                // インスタンスを生成し、Webサーバー起動
                streaming = when (mirroringSettingData.streamingType) {

                    StreamingType.MpegDash -> DashStreaming(
                        context = this@ScreenMirroringService,
                        parentFolder = getExternalFilesDir(null)!!,
                        mirroringSettingData = mirroringSettingData
                    )

                    StreamingType.WebSocket -> WSStreaming(
                        context = this@ScreenMirroringService,
                        parentFolder = getExternalFilesDir(null)!!,
                        mirroringSettingData = mirroringSettingData
                    )
                }.apply { startServer() }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // ミラーリング中であれば、サービスを終了しない。
        // ミラーリングしていないなら、終了してリソース開放する
        if (!isScreenMirroring.value) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        mirroringJob?.cancel()
        streaming?.destroy()
    }

    /**
     * ミラーリングを開始する
     *
     * @param resultCode onActivityResult でもらえる Code
     * @param resultData onActivityResult でもらえる Intent
     * @param displayHeight 画面の高さ
     * @param displayWidth 画面の幅
     */
    fun startScreenMirroring(
        resultCode: Int,
        resultData: Intent,
        displayHeight: Int,
        displayWidth: Int
    ) {
        coroutineScope.launch {
            // すでに起動中なら終了。join で終わるのを待つ
            mirroringJob?.cancelAndJoin()
            mirroringJob = launch {

                // フォアグラウンドサービスにするため、通知を出す
                notifyIpAddress()
                val ipAddressNotifyJob = launch {
                    val mirroringSettingData = mirroringSettingDataFlow.first()
                    IpAddressTool.collectIpAddress(this@ScreenMirroringService)
                        .distinctUntilChanged()
                        .collect { ipAddress ->
                            val url = "http://$ipAddress:${mirroringSettingData.portNumber}"
                            notifyIpAddress(contentText = "${getString(R.string.ip_address)}：$url", url = url)
                            Log.d(TAG, url)
                        }
                }

                try {
                    mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)
                    _isScreenMirroring.value = true
                    // ミラーリングの準備と開始
                    // エンコード中はコルーチンが一時停止する
                    streaming?.prepareAndStartEncode(mediaProjection!!, displayHeight, displayWidth)
                } finally {
                    // コルーチンキャンセル時にリソース開放をする
                    // フォアグラウンドサービスも解除
                    _isScreenMirroring.value = false
                    ipAddressNotifyJob.cancel()
                    mediaProjection?.stop()
                    ServiceCompat.stopForeground(this@ScreenMirroringService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                }
            }
        }
    }

    /** ミラーリングを終了する。フォアグラウンドサービスも解除される。 */
    fun stopScreenMirroring() {
        mirroringJob?.cancel()
    }

    /**
     * 通知を作って出す
     *
     * @param contentText 通知本文
     * @param url QRコードにするURL、あるなら
     */
    private fun notifyIpAddress(contentText: String = getString(R.string.zeromirror_service_notification_content), url: String? = null) {
        // 通知チャンネル
        val notificationManagerCompat = NotificationManagerCompat.from(this@ScreenMirroringService)
        //通知チャンネルが存在しないときは登録する
        if (notificationManagerCompat.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            val channel = NotificationChannelCompat.Builder(NOTIFICATION_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW).apply {
                setName(getString(R.string.notification_channel_title))
            }.build()
            notificationManagerCompat.createNotificationChannel(channel)
        }
        //通知作成
        val notification = NotificationCompat.Builder(this@ScreenMirroringService, NOTIFICATION_CHANNEL_ID).also { builder ->
            builder.setContentTitle(getString(R.string.zeromirror_service_notification_title))
            builder.setContentText(contentText)
            builder.setSmallIcon(R.drawable.zeromirror_android)
            builder.setContentIntent(PendingIntent.getActivity(this, 1, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            if (url != null) {
                val bitmap = QrCodeGeneratorTool.generateQrCode(url)
                builder.setLargeIcon(bitmap)
            }
        }.build()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            // Android 10 未満は使われてい無さそう
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else 0
        )
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
         * ミラーリング中にアプリ一覧画面から56したとき、[onTaskRemoved]でミラーリング中であれば終了しないようにします。
         *
         * @param context [Context]
         * @param lifecycle [Lifecycle]
         */
        fun bindScreenMirrorService(
            context: Context,
            lifecycle: Lifecycle
        ) = callbackFlow {
            val serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val screenMirroringService = (service as LocalBinder).service
                    trySend(screenMirroringService)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    trySend(null)
                }
            }
            // ライフサイクルを監視してバインド、バインド解除する
            val lifecycleObserver = object : DefaultLifecycleObserver {
                val intent = Intent(context, ScreenMirroringService::class.java)
                override fun onStart(owner: LifecycleOwner) {
                    super.onStart(owner)
                    context.startService(intent)
                    context.bindService(intent, serviceConnection, BIND_AUTO_CREATE)
                }

                override fun onStop(owner: LifecycleOwner) {
                    super.onStop(owner)
                    context.unbindService(serviceConnection)
                }
            }
            lifecycle.addObserver(lifecycleObserver)
            awaitClose { lifecycle.removeObserver(lifecycleObserver) }
        }
    }
}