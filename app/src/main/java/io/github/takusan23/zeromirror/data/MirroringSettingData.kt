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
 * @param isRecordInternalAudio 内部音声を入れる場合はtrue、権限があるかどうかまでは見ていません
 * @param isVP9 H.264ではなく、VP9を利用する場合はtrue。なんかかっこいい
 * @param isCustomResolution 動画の解像度をカスタマイズした場合はtrue、falseなら画面の解像度を利用する
 * @param videoHeight 動画の高さ、0の場合は画面の大きさを利用すること
 * @param videoWidth 動画の幅、0の場合は画面の大きさを利用すること
 */
data class MirroringSettingData(
    val portNumber: Int,
    val intervalMs: Long,
    val videoBitRate: Int,
    val videoFrameRate: Int,
    val audioBitRate: Int,
    val isRecordInternalAudio: Boolean,
    val isVP9: Boolean,
    val isCustomResolution: Boolean,
    val videoHeight: Int,
    val videoWidth: Int,
) {

    companion object {

        /** デフォルトポート番号 */
        const val DEFAULT_PORT_NUMBER = 2828

        /** デフォルトファイル生成間隔 */
        const val DEFAULT_INTERVAL_MS = 5_000L

        /** デフォルト映像ビットレート */
        const val DEFAULT_VIDEO_BIT_RATE = 1_000_000

        /** デフォルト音声ビットレート */
        const val DEFAULT_AUDIO_BIT_RATE = 128_000

        /** デフォルト映像フレームレート */
        const val DEFAULT_VIDEO_FRAME_RATE = 30

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
                    isVP9 = data[SettingKeyObject.IS_VP9] ?: false,
                    isCustomResolution = data[SettingKeyObject.IS_CUSTOM_RESOLUTION] ?: false,
                    videoWidth = data[SettingKeyObject.VIDEO_WIDTH] ?: 0,
                    videoHeight = data[SettingKeyObject.VIDEO_HEIGHT] ?: 0
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
                it[SettingKeyObject.IS_VP9] = mirroringSettingData.isVP9
                it[SettingKeyObject.IS_CUSTOM_RESOLUTION] = mirroringSettingData.isCustomResolution
                it[SettingKeyObject.VIDEO_WIDTH] = mirroringSettingData.videoWidth
                it[SettingKeyObject.VIDEO_HEIGHT] = mirroringSettingData.videoHeight
            }
        }

    }

}