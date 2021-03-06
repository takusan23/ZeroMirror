package io.github.takusan23.zeromirror

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.takusan23.zeromirror.ui.screen.MainScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainScreen()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        ScreenMirrorService.stopService(this)
    }
}