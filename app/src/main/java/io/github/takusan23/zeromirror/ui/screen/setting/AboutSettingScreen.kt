package io.github.takusan23.zeromirror.ui.screen.setting

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.takusan23.zeromirror.R
import io.github.takusan23.zeromirror.ui.components.AboutInfoListCard
import io.github.takusan23.zeromirror.ui.components.AboutTopCard

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
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "このアプリについて") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(id = R.drawable.ic_outline_arrow_back_24), contentDescription = null)
                    }
                }
            )
        }
    ) {
        Column(modifier = Modifier.padding(it)) {
            AboutTopCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                onGitHubClick = { openBrowser(context, GitHubUrl) }
            )
            Spacer(modifier = Modifier.padding(10.dp))
            AboutInfoListCard(
                modifier = Modifier
                    .fillMaxWidth(),
                appVersion = version,
                twitterId = TwitterId,
                onTwitterClick = { openBrowser(context, TwitterUrl) }
            )
        }
    }
}

/** GitHubリンク */
private val GitHubUrl = "https://github.com/takusan23/ZeroMirror"

/** Twitter ID */
private val TwitterId = "@takusan__23"

/** Twitter Url */
private val TwitterUrl = "https://twitter.com/takusan__23"

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