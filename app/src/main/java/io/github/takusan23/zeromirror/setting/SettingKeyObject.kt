package io.github.takusan23.zeromirror.setting

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey

/**
 * 設定のキー一覧
 */
object SettingKeyObject {

    /** ポート番号 */
    val PORT_NUMBER = intPreferencesKey("port_number")

    /** 更新間隔ダイナモ感覚YOYOYO！ ミリ秒 */
    val INTERVAL_MS = longPreferencesKey("interval_ms")

    /** 映像のビットレート、単位はビット */
    val VIDEO_BIT_RATE = intPreferencesKey("video_bit_rate")

    /** 映像のフレームレート、fps */
    val VIDEO_FRAME_RATE = intPreferencesKey("video_frame_rate")

    /** 音声のビットレート、単位はビット */
    val AUDIO_BIT_RATE = intPreferencesKey("audio_bit_rate")

    /** 内部音声を収録するか */
    val IS_RECORD_INTERNAL_AUDIO = booleanPreferencesKey("is_record_internal_audio")

    /** はじめまして画面への誘導を消すかどうか */
    val IS_HIDE_HELLO_CARD = booleanPreferencesKey("is_hide_hello_card")

    /** H.264/avc の代わりに VP9 コーデックを利用する場合はtrue */
    val IS_VP9 = booleanPreferencesKey("is_vp9")

    /** 解像度を指定する場合はtrue */
    val IS_CUSTOM_RESOLUTION = booleanPreferencesKey("is_custom_resolution")

    /** 動画の高さ */
    val VIDEO_HEIGHT = intPreferencesKey("video_height")

    /** 動画の幅 */
    val VIDEO_WIDTH = intPreferencesKey("video_width")
}