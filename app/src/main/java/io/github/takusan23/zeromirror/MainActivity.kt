package io.github.takusan23.zeromirror

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.takusan23.hlsserver.HlsServer
import io.github.takusan23.zeromirror.tool.IpAddressTool
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private val PORT_NUMBER = 10_000

    private lateinit var server: HlsServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {

            IpAddressTool.collectIpAddress(this@MainActivity).onEach {
                println("http://$it:$PORT_NUMBER")
            }.launchIn(this)

            val file = getExternalFilesDir(null)
            val videoFolder = File(file, "hls_2")
            server = HlsServer(
                portNumber = PORT_NUMBER,
                hostingFolder = videoFolder
            )
            server.startServer()

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        server.stopServer()
    }
}