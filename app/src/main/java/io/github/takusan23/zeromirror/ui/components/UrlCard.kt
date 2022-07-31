package io.github.takusan23.zeromirror.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.zeromirror.R
import io.github.takusan23.zeromirror.tool.QrCodeGeneratorTool


/**
 * ミラーリング視聴用URLを表示する
 *
 * @param modifier [Modifier]
 * @param url 視聴用URL
 * @param onShareClick 共有押したとき
 * @param onOpenBrowserClick ブラウザーで開く押したとき
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlCard(
    modifier: Modifier = Modifier,
    url: String,
    onShareClick: () -> Unit,
    onOpenBrowserClick: () -> Unit,
) {
    // QRコードを出すか
    val isShowQrCode = remember { mutableStateOf(false) }
    // QRコードのBitmap
    val qrCodeBitmap = remember(url) { QrCodeGeneratorTool.generateQrCode(url) }

    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                modifier = Modifier.padding(5.dp),
                text = stringResource(id = R.string.url_card_title),
                style = TextStyle(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 20.sp
            )
            Text(
                modifier = Modifier.padding(5.dp),
                text = url,
                fontSize = 20.sp
            )

            // QRコード
            if (isShowQrCode.value) {
                Surface(
                    modifier = Modifier
                        .padding(5.dp)
                        .align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Image(
                        bitmap = qrCodeBitmap.asImageBitmap(),
                        contentDescription = null
                    )
                }
            }

            Text(
                modifier = Modifier.padding(5.dp),
                text = stringResource(id = R.string.url_card_description),
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                Button(onClick = onShareClick) {
                    Icon(painter = painterResource(id = R.drawable.ic_outline_share_24), contentDescription = null)
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(id = R.string.url_card_share))
                }
                Spacer(modifier = Modifier.size(10.dp))
                OutlinedButton(onClick = { isShowQrCode.value = !isShowQrCode.value }) {
                    Icon(painter = painterResource(id = R.drawable.ic_outline_qr_code_24), contentDescription = null)
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(id = R.string.url_card_qr_code))
                }
                Spacer(modifier = Modifier.size(10.dp))
                OutlinedButton(onClick = onOpenBrowserClick) {
                    Icon(painter = painterResource(id = R.drawable.ic_outline_open_in_browser_24), contentDescription = null)
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(id = R.string.url_card_open_browser))
                }
            }
        }
    }
}