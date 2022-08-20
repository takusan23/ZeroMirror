package io.github.takusan23.zeromirror

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import io.github.takusan23.hlsserver.Server
import io.github.takusan23.zeromirror.ui.screen.MainScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainScreen()
        }

        lifecycleScope.launch {
            return@launch
            val captureVideoManager = CaptureVideoManager(
                parentFolder = getExternalFilesDir(null)!!,
                prefixName = "file_",
                isWebM = true
            )
            val server = Server(2828, captureVideoManager.outputsFolder, fileIntervalMs = 5_000)
            server.startServer()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        ScreenMirrorService.stopService(this)
    }
}