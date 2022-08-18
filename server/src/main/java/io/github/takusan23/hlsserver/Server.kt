package io.github.takusan23.hlsserver

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * ミラーリングした映像を配信するサーバー
 *
 * @param portNumber ポート番号
 * @param hostingFolder 公開するフォルダ
 * @param serverLaunchDate サーバー起動日時
 * @param fileIntervalMs ファイルの生成間隔
 * */
class Server(
    private val portNumber: Int = 10_000,
    private val hostingFolder: File,
    private val serverLaunchDate: Long = System.currentTimeMillis(),
    private val fileIntervalMs: Long = 5_000,
) {
    /**
     * コンテンツが利用可能になる時間（ISO-8601）
     * この値があることで、途中から再生した場合でも途中のセグメントから取得するようになる
     * TODO Android 7 以降のみ対応
     */
    private val contentAvailableTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.JAPAN).format(serverLaunchDate + fileIntervalMs)

    /** MPEG-DASH版 視聴ページ */
    private val mpegDashHtml = """
        <!doctype html>
        <html>
        <head>
            <title>ぜろみらー MPEG-DASH</title>
            <style>
                video {
                    width: 640px;
                    height: 360px;
                }
            </style>
        </head>
        <body>
            <div>
                <video id="videoPlayer" controls muted autoplay></video>
            </div>
            <script src="https://cdn.dashjs.org/latest/dash.all.debug.js"></script>
            <script>
                (function () {
                    var url = "manifest.mpd";
                    var player = dashjs.MediaPlayer().create();
                    player.initialize(document.querySelector("#videoPlayer"), url, true);
                })();
            </script>
        </body>
        </html>
    """.trimIndent()

    /**
     * MPEG-DASHのマニフェストファイル
     * initialization に最初の WebM を渡してるが、多分良くない。初期化に必要なデータのみを持たせて、映像データ（Cluster）はそれ単体で吐き出すべき
     */
    private val manifestMpd = """
        <?xml version="1.0" encoding="utf-8"?>
        <MPD xmlns="urn:mpeg:dash:schema:mpd:2011" availabilityStartTime="$contentAvailableTime" maxSegmentDuration="PT5S" minBufferTime="PT5S" type="dynamic" profiles="urn:mpeg:dash:profile:isoff-live:2011,http://dashif.org/guidelines/dash-if-simple">
          <BaseURL>/</BaseURL>
          <Period start="PT0S">
            <AdaptationSet mimeType="video/webm">
              <Role schemeIdUri="urn:mpeg:dash:role:2011" value="main" />
              <!-- duration が更新頻度っぽい -->
              <SegmentTemplate duration="5" initialization="/file_0.webm" media="/file_${"$"}Number${'$'}.webm"/>
              <Representation id="default" codecs="vp9,opus"/>
            </AdaptationSet>
          </Period>
        </MPD>
    """.trimIndent()

    /** サーバー */
    private val server = embeddedServer(Netty, port = portNumber) {
        install(WebSockets)

        routing {
            get("/") {
                call.respondText(mpegDashHtml, ContentType.parse("text/html"))
            }
            get("manifest.mpd") {
                call.respondText(manifestMpd, ContentType.parse("text/plain"))
            }
            // 静的ファイル、動画などを配信する。
            static {
                staticRootFolder = hostingFolder
                files(hostingFolder)
            }
        }
    }

    /** サーバーを開始する */
    fun startServer() {
        server.start()
    }

    /** サーバーを終了する */
    fun stopServer() {
        server.stop()
    }

}