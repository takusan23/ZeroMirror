package io.github.takusan23.zeromirror

import android.content.Context
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import io.github.takusan23.zeromirror.databinding.ActivityMainBinding
import io.github.takusan23.zeromirror.ui.screen.MainScreen

class MainActivity : ComponentActivity() {

    private val mediaProjectionManager by lazy { getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    private val viewBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    /** 権限コールバック */
    private val mediaProjectionResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // ミラーリングサービス開始
        if (result.resultCode == RESULT_OK && result.data != null) {
            ScreenMirrorService.startService(this, result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, "開始が拒否されました、(ヽ´ω`)", Toast.LENGTH_SHORT).show()
        }
    }

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