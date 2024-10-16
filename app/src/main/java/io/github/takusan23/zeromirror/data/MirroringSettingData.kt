package io.github.takusan23.zeromirror.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import io.github.takusan23.zeromirror.setting.SettingKeyObject
import io.github.takusan23.zeromirror.setting.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * ミラーリング情報データ
 *
 * @param portNumber ポート番号
 * @param videoBitRate 映像ビットレート、ビット
 * @param audioBitRate 音声ビットレート、ビット
 * @param videoFrameRate 映像フレームレート、fps
 * @param intervalMs 動画を切り出す間隔、ミリ秒
 * @param isRecordInternalAudio 内部音声を入れる場合は true、権限があるかどうかまでは見ていません
 * @param isCustomResolution 動画の解像度をカスタマイズした場合は true、false なら画面の解像度を利用する
 * @param videoHeight 動画の高さ
 * @param videoWidth 動画の幅
 * @param isMirroringExternalDisplay 外部出力ディスプレイをミラーリングする場合は true
 * @param streamingType ストリーミング方式。デフォルトは[StreamingType.MpegDash]
 * @param isVP8 [StreamingType.MpegDash]の場合、動画コーデックにVP8を利用する場合は true
 */
data class MirroringSettingData(
    val portNumber: Int,
    val intervalMs: Long,
    val videoBitRate: Int,
    val videoFrameRate: Int,
    val audioBitRate: Int,
    val isRecordInternalAudio: Boolean,
    val isCustomResolution: Boolean,
    val videoHeight: Int,
    val videoWidth: Int,
    val isMirroringExternalDisplay: Boolean,
    val streamingType: StreamingType,
    val isVP8: Boolean,
) {

    companion object {

        /** デフォルトポート番号 */
        private const val DEFAULT_PORT_NUMBER = 2828

        /** デフォルトファイル生成間隔 */
        private const val DEFAULT_INTERVAL_MS = 1_000L

        /** デフォルト映像ビットレート */
        private const val DEFAULT_VIDEO_BIT_RATE = 10_000_000 // 10M

        /** デフォルト音声ビットレート */
        private const val DEFAULT_AUDIO_BIT_RATE = 128_000

        /** デフォルト映像フレームレート */
        private const val DEFAULT_VIDEO_FRAME_RATE = 30

        /** デフォルトの動画の幅 */
        private const val DEFAULT_VIDEO_WIDTH = 1920

        /** デフォルトの動画の高さ */
        private const val DEFAULT_VIDEO_HEIGHT = 1080

        /**
         * データストアから読み出してデータクラスを返す
         *
         * @param context [context]
         */
        fun loadDataStore(context: Context): Flow<MirroringSettingData> {
            // FlowでDataStoreの変更を受け取って、データクラスに変換して返す
            return context.dataStore.data.map { data ->
                MirroringSettingData(
                    portNumber = data[SettingKeyObject.PORT_NUMBER] ?: DEFAULT_PORT_NUMBER,
                    intervalMs = data[SettingKeyObject.INTERVAL_MS] ?: DEFAULT_INTERVAL_MS,
                    videoBitRate = data[SettingKeyObject.VIDEO_BIT_RATE] ?: DEFAULT_VIDEO_BIT_RATE,
                    videoFrameRate = data[SettingKeyObject.VIDEO_FRAME_RATE] ?: DEFAULT_VIDEO_FRAME_RATE,
                    audioBitRate = data[SettingKeyObject.AUDIO_BIT_RATE] ?: DEFAULT_AUDIO_BIT_RATE,
                    isRecordInternalAudio = data[SettingKeyObject.IS_RECORD_INTERNAL_AUDIO] ?: false,
                    isCustomResolution = data[SettingKeyObject.IS_CUSTOM_RESOLUTION] ?: false,
                    videoWidth = data[SettingKeyObject.VIDEO_WIDTH] ?: DEFAULT_VIDEO_WIDTH,
                    videoHeight = data[SettingKeyObject.VIDEO_HEIGHT] ?: DEFAULT_VIDEO_HEIGHT,
                    isMirroringExternalDisplay = data[SettingKeyObject.IS_MIRRORING_EXTERNAL_DISPLAY] ?: false,
                    streamingType = data[SettingKeyObject.STREAMING_TYPE]?.let { StreamingType.valueOf(it) } ?: StreamingType.MpegDash,
                    isVP8 = data[SettingKeyObject.MPEG_DASH_CODEC_VP8] ?: false
                )
            }
        }

        /**
         * [MirroringSettingData]をデータストアへ格納する
         *
         * @param context [Context]
         * @param mirroringSettingData ミラーリング情報
         */
        suspend fun setDataStore(context: Context, mirroringSettingData: MirroringSettingData) {
            context.dataStore.edit {
                it[SettingKeyObject.PORT_NUMBER] = mirroringSettingData.portNumber
                it[SettingKeyObject.INTERVAL_MS] = mirroringSettingData.intervalMs
                it[SettingKeyObject.VIDEO_BIT_RATE] = mirroringSettingData.videoBitRate
                it[SettingKeyObject.VIDEO_FRAME_RATE] = mirroringSettingData.videoFrameRate
                it[SettingKeyObject.AUDIO_BIT_RATE] = mirroringSettingData.audioBitRate
                it[SettingKeyObject.IS_RECORD_INTERNAL_AUDIO] = mirroringSettingData.isRecordInternalAudio
                it[SettingKeyObject.IS_CUSTOM_RESOLUTION] = mirroringSettingData.isCustomResolution
                it[SettingKeyObject.VIDEO_WIDTH] = mirroringSettingData.videoWidth
                it[SettingKeyObject.VIDEO_HEIGHT] = mirroringSettingData.videoHeight
                it[SettingKeyObject.IS_MIRRORING_EXTERNAL_DISPLAY] = mirroringSettingData.isMirroringExternalDisplay
                it[SettingKeyObject.STREAMING_TYPE] = mirroringSettingData.streamingType.name
                it[SettingKeyObject.MPEG_DASH_CODEC_VP8] = mirroringSettingData.isVP8
            }
        }

        /**
         * 設定項目をリセットする
         * @param context [Context]
         */
        suspend fun resetDataStore(context: Context) {
            context.dataStore.edit {
                it -= SettingKeyObject.PORT_NUMBER
                it -= SettingKeyObject.INTERVAL_MS
                it -= SettingKeyObject.VIDEO_BIT_RATE
                it -= SettingKeyObject.VIDEO_FRAME_RATE
                it -= SettingKeyObject.AUDIO_BIT_RATE
                it -= SettingKeyObject.IS_RECORD_INTERNAL_AUDIO
                it -= SettingKeyObject.IS_CUSTOM_RESOLUTION
                it -= SettingKeyObject.VIDEO_WIDTH
                it -= SettingKeyObject.VIDEO_HEIGHT
                it -= SettingKeyObject.IS_MIRRORING_EXTERNAL_DISPLAY
                it -= SettingKeyObject.STREAMING_TYPE
                it -= SettingKeyObject.MPEG_DASH_CODEC_VP8
            }
        }
    }
}