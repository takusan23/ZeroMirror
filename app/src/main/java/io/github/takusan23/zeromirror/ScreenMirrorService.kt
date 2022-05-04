package io.github.takusan23.zeromirror

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.takusan23.zeromirror.tool.UniqueFileTool
import kotlinx.coroutines.*

/**
 * ミラーリングサービス
 */
class ScreenMirrorService : Service() {

    // 通知

    // ファイル関係
    private val uniqueFileTool by lazy { UniqueFileTool(getExternalFilesDir(null)!!, "videofile", "mp4") }

    // ミラーリングで使う
    private val mediaProjectionManager by lazy { getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    /** 録画のクラス */
    private var mediaRecorder: MediaRecorder? = null

    /** 定期実行 */
    private val coroutineScope = CoroutineScope(Job())

    var height = 2800
    var width = 1400
    var dpi = 1000

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val resultCode = intent?.getIntExtra(INTENT_RESULT_CODE, -1)
        val resultData = intent?.getParcelableExtra<Intent>(INTENT_RESULT_INTENT)

        // 通知発行
        notifyForegroundNotification()

        // 起動できる場合は起動
        if (resultCode != null && resultData != null) {
            startScreenMirrorService(resultCode, resultData)
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        mediaRecorder?.stop()
        mediaRecorder?.release()
        mediaProjection?.stop()
        virtualDisplay?.release()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    /**
     * ミラーリング開始
     *
     * @param resultCode onActivityResult のときのCode
     * @param resultData onActivityResult のときのIntent
     */
    private fun startScreenMirrorService(resultCode: Int, resultData: Intent) {
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)
        // 画面録画
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }
        // 初期化
        setupMediaRecorder()
        virtualDisplay = createVirtualDisplay()
        mediaRecorder?.start()

        // 録画開始、定期実行する
        coroutineScope.launch {
            while (isActive) {
                delay(5_000)
                mediaRecorder?.stop()
                mediaRecorder?.reset()
                // 次の用意
                setupMediaRecorder()
                virtualDisplay?.release()
                virtualDisplay = createVirtualDisplay()
                mediaRecorder?.start()
            }
        }
    }

    /** [mediaRecorder]の初期化をする */
    private fun setupMediaRecorder() {
        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoSize(width, height)
            setOutputFile(uniqueFileTool.generateFile().path)
            prepare()
        }
    }

    /** [VirtualDisplay]を作る、各初期化後に呼ぶ */
    private fun createVirtualDisplay(): VirtualDisplay {
        return mediaProjection!!.createVirtualDisplay(
            "io.github.takusan23.zeromirror",
            width,
            height,
            dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder!!.surface,
            null,
            null
        )
    }

    /** フォアグラウンドサービスの通知を発行する */
    private fun notifyForegroundNotification() {
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
            setContentTitle("ブラウザから見れます")
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
        private const val INTENT_RESULT_CODE = "intent_result_code"

        /** onActivityResult でもらえるIntent */
        private const val INTENT_RESULT_INTENT = "intent_result_intent"

        /**
         * ミラーリングサービスを開始させる
         *
         * @param context [Context]
         * @param resultCode onActivityResult でもらえるCode
         * @param data onActivityResult でもらえるIntent
         */
        fun startService(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, ScreenMirrorService::class.java).apply {
                putExtra(INTENT_RESULT_CODE, resultCode)
                putExtra(INTENT_RESULT_INTENT, data)
            }
            ContextCompat.startForegroundService(context, intent)
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