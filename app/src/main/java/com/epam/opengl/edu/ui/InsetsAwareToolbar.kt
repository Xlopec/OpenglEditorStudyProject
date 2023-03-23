package com.epam.opengl.edu.ui

import android.content.Context
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.AutoFixOff
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.epam.opengl.edu.R
import com.epam.opengl.edu.model.Editor
import com.epam.opengl.edu.model.OnEditorMenuToggled
import com.epam.opengl.edu.model.OnExportImage
import com.epam.opengl.edu.model.OnUndoTransformation
import com.epam.opengl.edu.model.canUndoTransformations
import com.epam.opengl.edu.model.isDisplayed

private const val MaxDisplayUndoOperations = 10

@Composable
fun InsetsAwareToolbar(
    title: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(
        elevation = 1.dp,
        color = MaterialTheme.colors.primarySurface,
    ) {
        TopAppBar(
            modifier = Modifier.statusBarsPadding(),
            backgroundColor = Color.Transparent,
            contentColor = contentColorFor(MaterialTheme.colors.primarySurface),
            title = title,
            elevation = 0.dp,
            actions = actions
        )
    }
}

@Composable
fun AppToolbarActions(
    editor: Editor,
    handler: MessageHandler,
) {
    if (editor.canUndoTransformations) {
        IconButton(onClick = { handler(OnUndoTransformation) }) {
            BadgedBox(badge = {
                Badge {
                    val context = LocalContext.current
                    Text(
                        text = with(context) { editor.undoBadgeText },
                        style = MaterialTheme.typography.caption
                    )
                }
            }) {
                Icon(
                    imageVector = Icons.Filled.Undo,
                    contentDescription = null
                )
            }
        }
    }
    IconButton(
        enabled = !editor.isExportingImage,
        onClick = { handler(OnExportImage) }
    ) {
        Icon(
            imageVector = Icons.Filled.UploadFile,
            contentDescription = null
        )
    }
    IconButton(onClick = { handler(OnEditorMenuToggled) }) {
        Icon(
            imageVector = if (editor.isDisplayed) Icons.Filled.AutoFixOff else Icons.Filled.AutoFixNormal,
            contentDescription = null
        )
    }
}

context (Context)
private val Editor.undoBadgeText: String
    get() = if (previous.size > MaxDisplayUndoOperations) getString(R.string.badge_operations_overflow) else previous.size.toString()