package io.github.takusan23.zeromirror.data

import io.github.takusan23.zeromirror.R

/**
 * モード一覧
 *
 * @param title タイトルのリソースID
 * @param message 説明のリソースID
 * @param icon アイコン
 */
enum class StreamingType(val title: Int, val message: Int, val icon: Int) {
    /** MPEG-DASH で配信する */
    MpegDash(
        title = R.string.streaming_setting_type_mpeg_dash_title,
        message = R.string.streaming_setting_type_mpeg_dash_description,
        icon = R.drawable.streaming_type_dash
    ),

    /** WebSocket で配信する */
    WebSocket(
        title = R.string.streaming_setting_type_websocket_title,
        message = R.string.streaming_setting_type_websocket_description,
        icon = R.drawable.streaming_type_ws
    )
}