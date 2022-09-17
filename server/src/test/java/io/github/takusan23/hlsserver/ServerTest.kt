package io.github.takusan23.hlsserver

import org.junit.Test
import java.io.File

/** サーバー実行テスト */
class ServerTest {

    /** ポート番号 */
    private val portNumber = 10_000

    /** 公開するファイル */
    private val hostingFileList = File("""C:\Users\takusan23\Desktop\hls_2""")

    /** サーバー */
    private val server = Server(
        portNumber = portNumber,
        hostingFolder = hostingFileList,
        indexHtml = """<h1>Hello World</h1>"""
    )

    /** サーバーを開始できること */
    @Test
    fun startServer() {
        println("http://localhost:$portNumber/")
        server.startServer()
        while (true) {
            // 待機...
        }
    }

}