package io.github.takusan23.zeromirror.ui.components

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.zeromirror.R

/**
 * 通知権限を取得するコンポーネント
 *
 * @param modifier [Modifier]
 * @param permissionResult 結果
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostNotificationPermissionCard(
    modifier: Modifier = Modifier,
    permissionResult: (Boolean) -> Unit,
) {
    /** 権限コールバック */
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGrant ->
        permissionResult(isGrant)
    }

    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                modifier = Modifier.padding(5.dp),
                text = stringResource(id = R.string.post_notification_permission_card_title),
                style = TextStyle(fontWeight = FontWeight.Bold),
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(10.dp))

            Text(
                modifier = Modifier.padding(5.dp),
                text = stringResource(id = R.string.post_notification_permission_card_description)
            )
            OutlinedButton(
                modifier = Modifier
                    .align(Alignment.End),
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionRequest.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_outline_settings_24), contentDescription = null)
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = stringResource(id = R.string.post_notification_permission_card_button))
            }
        }
    }
}