package io.github.takusan23.zeromirror

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.window.layout.WindowMetricsCalculator
import io.github.takusan23.hlsserver.Server
import io.github.takusan23.zeromirror.data.MirroringSettingData
import io.github.takusan23.zeromirror.media.InternalAudioEncoder
import io.github.takusan23.zeromirror.media.ScreenVideoEncoder
import io.github.takusan23.zeromirror.tool.PermissionTool
import io.github.takusan23.zeromirror.tool.TrackMixer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * ミラーリングサービス
 */
class ScreenMirrorService : Service() {

    private val TAG = ScreenMirrorService::class.simpleName

    /** コルーチンスコープ */
    private val coroutineScope = CoroutineScope(Job())

    /** 生成したファイルを管理する */
    private val captureVideoManager by lazy { CaptureVideoManager(getExternalFilesDir(null)!!, VIDEO_FILE_NAME) }

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
            // 今までのファイルを消す
            captureVideoManager.deleteParentFolderChildren()
            // 通知発行
            notifyForegroundNotification()
            // 設定を読み出す
            mirroringSettingData = MirroringSettingData.loadDataStore(this@ScreenMirrorService).first()

            // 起動できる場合は起動
            if (resultCode != null && resultData != null) {
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)
                // 画面ミラーリング
                screenVideoEncoder = ScreenVideoEncoder(resources.configuration.densityDpi, mediaProjection!!)
                screenVideoEncoder!!.prepareEncoder(
                    videoWidth = displayWidth,
                    videoHeight = displayHeight,
                    bitRate = mirroringSettingData!!.videoBitRate,
                    frameRate = mirroringSettingData!!.videoFrameRate,
                    iFrameInterval = 1
                )
                // 内部音声を一緒にエンコードする場合
                if (availableInternalAudio()) {
                    internalAudioEncoder = InternalAudioEncoder(mediaProjection!!)
                    internalAudioEncoder!!.prepareEncoder(
                        bitRate = mirroringSettingData!!.audioBitRate
                    )
                }
            } else {
                stopSelf()
            }

            // エンコーダーは別スレッドで
            launch { startEncode() }

            // サーバー開始
            server = Server(mirroringSettingData!!.portNumber, getExternalFilesDir(null)!!)
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
        server?.stopServer()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    /** 動画、内部音声エンコーダー（使うなら）を起動する */
    private suspend fun startEncode() = withContext(Dispatchers.Default) {
        // 内部録画エンコーダー
        launch { screenVideoEncoder!!.start() }
        // 内部音声エンコーダー
        if (availableInternalAudio()) {
            launch { internalAudioEncoder!!.start() }
        }

        // コルーチンの中なので whileループ できます
        while (isActive) {

            // それぞれ格納するファイルを用意
            if (availableInternalAudio()) {
                // 後で映像と音声を合成するので同じファイルに書き込む
                screenVideoEncoder!!.createContainer(File(getExternalFilesDir(null), SCREEN_CAPTURE_FILE_NAME))
                internalAudioEncoder!!.createContainer(File(getExternalFilesDir(null), INTERNAL_RECORD_FILE_NAME))
            } else {
                // ここで書き込んだファイルを直接クライアントに返すので重要
                screenVideoEncoder!!.createContainer(captureVideoManager.generateFile())
            }

            // intervalMs秒待機したら新しいファイルにする
            delay(mirroringSettingData?.intervalMs!!)

            // 内部音声を合成するか、合成せずに映像だけ返すか
            // 内部音声はAndroid10以降のみなので実装は必須
            val publishFile = if (availableInternalAudio()) {
                // ファイルの書き込みをやめる
                val videoFilePath = screenVideoEncoder!!.stopWriteContainer().path
                val audioFilePath = internalAudioEncoder!!.stopWriteContainer().path
                // 一つのファイルにする
                captureVideoManager.generateFile().also { mixedFile ->
                    TrackMixer.startMix(
                        mergeFileList = listOf(videoFilePath, audioFilePath),
                        resultFile = mixedFile
                    )
                }
            } else {
                screenVideoEncoder!!.stopWriteContainer()
            }
            // クライアント側にWebSocketでファイルが出来たことを通知する
            server?.updateVideoFileName(publishFile.name)
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

        /** 内部録画だけのファイルの名前 */
        private const val SCREEN_CAPTURE_FILE_NAME = "screen.mp4"

        /** 内部音声を記録するだけのファイルの名前 */
        private const val INTERNAL_RECORD_FILE_NAME = "internal.aac"

        /** クライアントに見せる最終的な動画のファイル名、拡張子はあとから付ける */
        private const val VIDEO_FILE_NAME = "publish"

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