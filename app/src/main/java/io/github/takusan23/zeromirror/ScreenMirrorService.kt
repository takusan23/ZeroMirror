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
import io.github.takusan23.hlsserver.Server
import io.github.takusan23.zeromirror.data.MirroringSettingData
import io.github.takusan23.zeromirror.media.ContainerFileWriter
import io.github.takusan23.zeromirror.media.InternalAudioEncoder
import io.github.takusan23.zeromirror.media.ScreenVideoEncoder
import io.github.takusan23.zeromirror.tool.IpAddressTool
import io.github.takusan23.zeromirror.tool.PermissionTool
import io.github.takusan23.zeromirror.tool.QrCodeGeneratorTool
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * ミラーリングサービス
 */
class ScreenMirrorService : Service() {
    /** コルーチンスコープ */
    private val coroutineScope = CoroutineScope(Job())

    /** 生成したファイルを管理する */
    private var captureVideoManager: CaptureVideoManager? = null

    /** エンコードされたデータをコンテナファイルに書き込む */
    private var containerFileWriter: ContainerFileWriter? = null

    /** サーバー これだけ別モジュール */
    private var server: Server? = null

    // ミラーリングで使う
    private val mediaProjectionManager by lazy { getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    private var mediaProjection: MediaProjection? = null

    /** 画面を録画してエンコードするクラス */
    private var screenVideoEncoder: ScreenVideoEncoder? = null

    /** 内部音声を収録してエンコードするクラス、Android 10未満では利用できないので使ってはいけない */
    private var internalAudioEncoder: InternalAudioEncoder? = null

    /** ミラーリング情報、ビットレートとか */
    private var mirroringSettingData: MirroringSettingData? = null

    // 画面サイズ
    private var displayHeight = 0
    private var displayWidth = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val resultCode = intent?.getIntExtra(KEY_INTENT_RESULT_CODE, -1)
        val resultData = intent?.getParcelableExtra<Intent>(KEY_INTENT_RESULT_INTENT)
        displayHeight = intent?.getIntExtra(KEY_INTENT_HEIGHT, 0) ?: 0
        displayWidth = intent?.getIntExtra(KEY_INTENT_WIDTH, 0) ?: 0

        coroutineScope.launch {
            // 通知発行
            notifyForegroundNotification()
            // 設定を読み出す
            mirroringSettingData = MirroringSettingData.loadDataStore(this@ScreenMirrorService).first()

            // 今までのファイルを消す
            captureVideoManager = CaptureVideoManager(
                parentFolder = getExternalFilesDir(null)!!,
                prefixName = VIDEO_FILE_NAME,
                isWebM = mirroringSettingData!!.isVP9
            )
            captureVideoManager?.deleteParentFolderChildren()

            // 一時ファイル
            val tempFile = captureVideoManager!!.generateTempFile(ContainerFileWriter.TEMP_VIDEO_FILENAME)
            // コンテナファイルに書き込むやつ
            containerFileWriter = ContainerFileWriter(
                includeAudio = mirroringSettingData!!.isRecordInternalAudio,
                isWebM = mirroringSettingData!!.isVP9,
                isMp4FastStart = true,
                tempFile = tempFile
            )

            // 起動できる場合は起動
            if (resultCode != null && resultData != null) {
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)
                // 画面ミラーリング
                screenVideoEncoder = ScreenVideoEncoder(resources.configuration.densityDpi, mediaProjection!!)

                // 解像度をカスタマイズしている場合
                val (height, width) = if (mirroringSettingData!!.isCustomResolution || mirroringSettingData!!.isVP9) {
                    // VP9 エンコーダーだと画面解像度を入れると失敗する？ 1280x720 / 1920x1080 だと成功する
                    mirroringSettingData!!.videoHeight to mirroringSettingData!!.videoWidth
                } else {
                    displayHeight to displayWidth
                }

                // エンコーダーの用意
                screenVideoEncoder!!.prepareEncoder(
                    videoWidth = width,
                    videoHeight = height,
                    bitRate = mirroringSettingData!!.videoBitRate,
                    frameRate = mirroringSettingData!!.videoFrameRate,
                    isVp9 = mirroringSettingData!!.isVP9,
                )
                // 内部音声を一緒にエンコードする場合
                if (availableInternalAudio()) {
                    internalAudioEncoder = InternalAudioEncoder(mediaProjection!!)
                    internalAudioEncoder!!.prepareEncoder(
                        bitRate = mirroringSettingData!!.audioBitRate,
                        isOpus = mirroringSettingData!!.isVP9,
                    )
                }
            } else {
                stopSelf()
            }

            // IPアドレスを通知として出す
            IpAddressTool.collectIpAddress(this@ScreenMirrorService).onEach { ipAddress ->
                val url = "http://$ipAddress:${mirroringSettingData!!.portNumber}"
                notifyForegroundNotification(url = url, contentText = "${getString(R.string.ip_address)}：$url")
                Log.d(TAG, url)
            }.launchIn(coroutineScope)

            // エンコーダーは別スレッドで
            launch { startEncode() }

            // サーバー開始
            server = Server(mirroringSettingData!!.portNumber, captureVideoManager!!.outputsFolder)
            server?.startServer()
        }

        return START_NOT_STICKY
    }

    @SuppressLint("NewApi")
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        screenVideoEncoder?.release()
        internalAudioEncoder?.release()
        mediaProjection?.stop()
        containerFileWriter?.release()
        server?.stopServer()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    /** 動画、内部音声エンコーダー（使うなら）を起動する */
    private suspend fun startEncode() = withContext(Dispatchers.Default) {
        // 初回用
        containerFileWriter?.createContainer(captureVideoManager!!.generateNewFile().path)

        // 画面録画エンコーダー
        launch {
            screenVideoEncoder!!.start(
                onOutputBufferAvailable = { byteBuffer, bufferInfo -> containerFileWriter?.writeVideo(byteBuffer, bufferInfo) },
                onOutputFormatAvailable = {
                    containerFileWriter?.setVideoTrack(it)
                    // 開始する
                    containerFileWriter?.start()
                }
            )
        }
        // 内部音声エンコーダー
        if (availableInternalAudio()) {
            launch {
                internalAudioEncoder!!.start(
                    onOutputBufferAvailable = { byteBuffer, bufferInfo -> containerFileWriter?.writeAudio(byteBuffer, bufferInfo) },
                    onOutputFormatAvailable = {
                        containerFileWriter?.setAudioTrack(it)
                        // ここでは start が呼べない、なぜなら音声が再生されてない場合は何もエンコードされないから
                    }
                )
            }
        }

        // コルーチンの中なので whileループ できます
        while (isActive) {
            // intervalMs 秒待機したら新しいファイルにする
            delay(mirroringSettingData!!.intervalMs)

            // クライアント側にWebSocketでファイルが出来たことを通知する
            // val publishFile = containerFileWriter!!.stopAndRelease()
            // server?.updateVideoFileName(publishFile.name)

            // エンコーダー内部で持っている時間をリセットする
            // screenVideoEncoder?.resetInternalTime()
            // if (availableInternalAudio()) {
            //     internalAudioEncoder?.resetInternalTime()
            // }

            // それぞれ格納するファイルを用意
            containerFileWriter?.createContainer(captureVideoManager!!.generateNewFile().path)
            // containerFileWriter?.start()
        }
    }

    /**
     * 内部音声を収録する場合はtrue、Android 10以降とか見てる
     *
     * @return 内部音声を取る場合はtrue
     */
    private fun availableInternalAudio() =
        PermissionTool.isAndroidQAndHigher()
                && PermissionTool.isGrantedRecordPermission(this)
                && mirroringSettingData?.isRecordInternalAudio == true

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

        /** 内部録画だけのファイルの名前、拡張子は [ScreenVideoEncoder.createContainer] とかで作る */
        private const val SCREEN_CAPTURE_FILE_NAME = "screen"

        /** 内部音声を記録するだけのファイルの名前 */
        private const val INTERNAL_RECORD_FILE_NAME = "internal"

        /** クライアントに見せる最終的な動画のファイル名、拡張子はあとから付ける */
        private const val VIDEO_FILE_NAME = "file_"

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