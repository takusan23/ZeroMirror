package io.github.takusan23.hlsserver

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import java.io.File

/**
 * ミラーリングした映像を配信するサーバー
 *
 * 視聴画面のHTMLを返す鯖と、映像が出来たことを通知するWebSocket鯖がある。
 *
 * @param portNumber ポート番号
 * @param hostingFolder 公開するフォルダ
 * */
class Server(
    private val portNumber: Int = 10_000,
    private val hostingFolder: File,
) {

    /** 新しい動画データが出来たら動画ファイルを入れるFlow、staticで公開しているので相対パスで良いはず */
    private val _updateVideoFileName = MutableStateFlow<String?>(null)

    /** サーバー */
    private val server = embeddedServer(Netty, port = portNumber) {
        install(WebSockets)

        // resources内のindex.htmlを取得。ブラウザ用投稿画面です
        val htmlFile = this@Server::class.java.classLoader.getResource("index.html")!!.readText()

        routing {
            // WebSocketと動画プレイヤーを持った簡単なHTMLを返す
            get("/") {
                call.respondText(htmlFile, ContentType.parse("text/html"))
            }
            // 静的ファイル、動画などを配信する。
            static {
                staticRootFolder = hostingFolder
                files(hostingFolder)
            }
            // WebSocket
            webSocket("/wsvideo") {
                // 新しいデータが来たらWebSocketを経由してクライアントへ通知する
                // JSON作るのちゃんとしろ
                _updateVideoFileName
                    .filterNotNull()
                    .collect { fileName -> send(Frame.Text("""{ "url":"$fileName" }""")) }
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

    /**
     * 新しい動画データが出来たことをWebSocket経由で通知する
     *
     * @param videoFileName ファイル名
     */
    fun updateVideoFileName(videoFileName: String) {
        _updateVideoFileName.value = videoFileName
    }
}