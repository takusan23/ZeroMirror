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
 * @param fileIntervalSec ファイルの生成間隔 秒
 * @param manifestUpdateSec マニフェストファイルを更新する頻度 秒
 */
class Server(
    private val portNumber: Int = 10_000,
    private val hostingFolder: File,
    private val fileIntervalSec: Int = 5,
    private val manifestUpdateSec: Int = 60,
) {
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.JAPAN)

    /** MPEG-DASHのマニフェストファイル [updateManifest] で更新される */
    private var manifestMpd = ""

    /** サーバー */
    private val server = embeddedServer(Netty, port = portNumber) {
        install(WebSockets)

        routing {
            get("/") {
                call.respondText(MPEG_DASH_HTML, ContentType.parse("text/html"))
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
        val serverLaunchTime = System.currentTimeMillis()
        updateManifest(serverLaunchTime)
        server.start()
    }

    /** サーバーを終了する */
    fun stopServer() {
        server.stop()
    }

    /**
     * マニフェストを更新する
     *
     * @param availabilityStartTime WebMのセグメントファイルが用意でき、再生可能になる時間
     */
    fun updateManifest(availabilityStartTime: Long = System.currentTimeMillis()) {
        // コンテンツが利用可能になる時間（ISO-8601）
        // この値があることで、途中から再生した場合でも途中のセグメントから取得するようになる
        val formattedAvailabilityStartTime = isoDateFormat.format(availabilityStartTime)
        // minimumUpdatePeriod を指定すると、マニフェストファイルを指定した時間の間隔で更新してくれる
        manifestMpd = """
            <?xml version="1.0" encoding="utf-8"?>
            <MPD xmlns="urn:mpeg:dash:schema:mpd:2011" availabilityStartTime="$formattedAvailabilityStartTime" minimumUpdatePeriod="P${manifestUpdateSec}S" maxSegmentDuration="PT${fileIntervalSec}S" minBufferTime="PT${fileIntervalSec}S" type="dynamic" profiles="urn:mpeg:dash:profile:isoff-live:2011,http://dashif.org/guidelines/dash-if-simple">
              <BaseURL>/</BaseURL>
              <Period start="PT0S">
                <AdaptationSet mimeType="video/webm">
                  <Role schemeIdUri="urn:mpeg:dash:role:2011" value="main" />
                  <!-- duration が更新頻度っぽい -->
                  <SegmentTemplate duration="$fileIntervalSec" initialization="/init.webm" media="/file_${"$"}Number${'$'}.webm" startNumber="0"/>
                  <Representation id="default" codecs="vp9,opus"/>
                </AdaptationSet>
              </Period>
            </MPD>
        """.trimIndent()
    }

    companion object {

        /** MPEG-DASH版 視聴ページ */
        private val MPEG_DASH_HTML = """
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
    }
}