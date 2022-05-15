package io.github.takusan23.zeromirror.tool

import android.content.Intent
import androidx.core.net.toUri

/** Intent 関係 */
object IntentTool {

    /**
     * 共有インテントを作る
     * @param url URL
     * @return [Intent]
     */
    fun createShareIntent(url: String): Intent {
        return Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, url)
            type = "text/plain"
        }, null)
    }

    /**
     * URLを開くインテントを作る
     *
     * @param url URL
     * @return [Intent]
     */
    fun createOpenBrowserIntent(url: String): Intent {
        return Intent(Intent.ACTION_VIEW, url.toUri())
    }

}