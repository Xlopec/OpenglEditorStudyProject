package io.github.xlopec.opengl.edu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.xlopec.opengl.edu.R
import io.github.xlopec.opengl.edu.model.AppState
import io.github.xlopec.opengl.edu.model.OnDebugModeChanged

@Composable
fun AppDrawer(
    state: AppState,
    handler: MessageHandler,
) {
    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
        SwitchItem(
            icon = Icons.Filled.DeveloperMode,
            imageDescription = stringResource(R.string.drawer_title_developer_mode),
            title = stringResource(R.string.drawer_title_developer_mode),
            description = stringResource(R.string.drawer_description_developer_mode),
            checked = state.isDebugModeEnabled,
            onCheckedChange = { checked -> handler(OnDebugModeChanged(checked)) }
        )
    }
}

@Composable
private fun SwitchItem(
    icon: ImageVector,
    imageDescription: String?,
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        Icon(
            contentDescription = imageDescription,
            tint = MaterialTheme.colors.onSurface,
            imageVector = icon
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.body1
            )
        }

        Switch(
            enabled = enabled,
            checked = checked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = contentColorFor(MaterialTheme.colors.primarySurface),
                uncheckedThumbColor = contentColorFor(MaterialTheme.colors.primarySurface),
            ),
            onCheckedChange = onCheckedChange
        )
    }
    Divider()
}