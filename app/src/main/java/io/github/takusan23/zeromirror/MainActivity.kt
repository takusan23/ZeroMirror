package io.github.takusan23.zeromirror

import android.content.Context
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import io.github.takusan23.zeromirror.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

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
        setContentView(viewBinding.root)

        viewBinding.activityMainStart.setOnClickListener {
            mediaProjectionResult.launch(mediaProjectionManager.createScreenCaptureIntent())
        }
        viewBinding.activityMainStop.setOnClickListener {
            ScreenMirrorService.stopService(this)
        }

        // val server = Server(hostingFolder = getExternalFilesDir(null)!!)
        // server.startServer()

    }

    override fun onDestroy() {
        super.onDestroy()
        ScreenMirrorService.stopService(this)
    }
}