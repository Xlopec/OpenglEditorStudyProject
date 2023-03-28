package io.github.xlopec.opengl.edu.ui

import android.content.Context
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.xlopec.opengl.edu.R
import io.github.xlopec.opengl.edu.model.*
import kotlinx.coroutines.launch

private const val MaxDisplayUndoOperations = 10

@Composable
fun AppToolbar(
    editor: Editor?,
    drawer: DrawerState,
    handler: MessageHandler,
) {

    val scope = rememberCoroutineScope()

    Surface(
        elevation = 1.dp,
        color = MaterialTheme.colors.primarySurface,
    ) {
        TopAppBar(
            modifier = Modifier.statusBarsPadding(),
            backgroundColor = Color.Transparent,
            contentColor = contentColorFor(MaterialTheme.colors.primarySurface),
            navigationIcon = {
                IconButton(
                    onClick = {
                        scope.launch {
                            if (drawer.isOpen) {
                                drawer.close()
                            } else {
                                drawer.open()
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = null
                    )
                }
            },
            title = { Text(text = stringResource(R.string.app_name)) },
            elevation = 0.dp,
            actions = {
                if (editor != null) {
                    AppToolbarActions(
                        editor = editor,
                        handler = handler,
                    )
                }
            }
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