package io.github.takusan23.zeromirror.ui.screen.setting

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.net.toUri
import androidx.lifecycle.compose.dropUnlessResumed
import io.github.takusan23.zeromirror.R
import io.github.takusan23.zeromirror.ui.components.AdvMain
import io.github.takusan23.zeromirror.ui.components.AdvMenu

/**
 * このアプリについて画面
 *
 * @param onBack 戻る押したときに呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingScreen(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val isPortRate = remember { context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT }
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName }

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(it)
        ) {
            IconButton(
                modifier = Modifier.align(alignment = Alignment.Start),
                onClick = dropUnlessResumed(block = onBack)
            ) { Icon(painter = painterResource(id = R.drawable.ic_outline_arrow_back_24), contentDescription = null) }

            AdvMain(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                version = version ?: ""
            )

            AdvMenu(
                isScrollable = isPortRate,
                onTwitterClick = { openBrowser(context, TwitterUrl) },
                onGitHubClick = { openBrowser(context, GitHubUrl) },
            )
        }
    }
}

/** GitHubリンク */
private const val GitHubUrl = "https://github.com/takusan23/ZeroMirror"

/** Twitter Url */
private const val TwitterUrl = "https://twitter.com/takusan__23"

/**
 * ブラウザを開く
 *
 * @param context [Context]
 * @param url URL
 * */
private fun openBrowser(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    context.startActivity(intent)
}