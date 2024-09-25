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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * ミラーリングした映像を配信するサーバー
 *
 * 視聴画面のHTMLを返す鯖と、映像が出来たことを通知するWebSocket鯖がある。
 *
 * TODO もうサーバーだけ別モジュールにする旨みが無いよ；；、もうエンコード部分をライブラリに切り出したほうが良いのかな
 *
 * @param portNumber ポート番号
 * @param hostingFolder 公開するフォルダ
 * @param indexHtml index.html
 * */
class Server(
    private val portNumber: Int = 10_000,
    private val hostingFolder: File,
    private val indexHtml: String,
) {

    /** 新しい動画データが出来たら動画ファイルを入れるFlow、staticで公開しているので相対パスで良いはず */
    private val _updateVideoFileName = MutableStateFlow<String?>(null)

    /** サーバー */
    private val server = embeddedServer(Netty, port = portNumber) {
        install(WebSockets)

        routing {
            // WebSocketと動画プレイヤーを持った簡単なHTMLを返す
            get("/") {
                call.respondText(indexHtml, ContentType.parse("text/html"))
            }
            // 静的ファイル公開するように。動画を配信する
            static {
                staticRootFolder = hostingFolder
                filesWithWait(hostingFolder)
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

    private fun Route.filesWithWait(folder: File) {
        static {
            files(folder)
            default("index.html")

            intercept(ApplicationCallPipeline.Features) {
                val path = call.parameters["path"] ?: return@intercept
                val requestedFile = File(folder, path)

                // Wait for the file if it doesn't exist yet
                if (!requestedFile.exists()) {
                    val fileExists = waitForFile(requestedFile)
                    if (!fileExists) {
                        call.respond(HttpStatusCode.NotFound, "File not found or creation timed out.")
                        return@intercept finish()
                    }
                }

                // Serve the file if it exists now
                call.respondFile(requestedFile)
            }
        }
    }

    suspend fun waitForFile(file: File, timeoutMillis: Long = 30_000): Boolean {
        return withTimeoutOrNull(timeoutMillis) {
            while (!file.exists()) {
                delay(500) // Check every 500ms
            }
            true
        } ?: false
    }
}