package io.github.takusan23.hlsserver

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

/**
 * HLSサーバー
 *
 * ローカルのサーバーを立てて、ここで映像を配信する。
 *
 * Ktorを内部的に利用している。
 *
 * Ktor、いつの間にか Android 5 以降で動くようになっていた。
 *
 * @param portNumber ポート番号
 * @param hostingFolder 公開するフォルダ
 * */
class HlsServer(
    private val portNumber: Int = 10_000,
    private val hostingFolder: File,
) {

    /** サーバー */
    private val server = embeddedServer(Netty, port = portNumber) {
        routing {
            get("/") {
                call.respondText("こちら、ぜろみらー の 配信サーバー(Webサーバー) です。私は生きています。")
            }
            static {
                // 静的ファイル、動画などを配信する。
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