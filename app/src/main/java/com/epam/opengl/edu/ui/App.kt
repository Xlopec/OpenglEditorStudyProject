package com.epam.opengl.edu.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.AutoFixOff
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Undo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.epam.opengl.edu.R
import com.epam.opengl.edu.model.*
import com.epam.opengl.edu.ui.gl.AppGLSurfaceView
import com.epam.opengl.edu.ui.theme.AppTheme

typealias MessageHandler = (Message) -> Unit

private const val MaxDisplayUndoOperations = 10

@Composable
fun App(
    state: AppState,
    handler: MessageHandler,
    onChooseImage: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.app_name))
                },
                actions = {
                    if (state.image != null) {
                        if (state.editMenu.canUndoTransformations) {
                            IconButton(onClick = { handler(OnUndoTransformation) }) {
                                BadgedBox(badge = {
                                    Badge {
                                        val context = LocalContext.current
                                        Text(
                                            text = with(context) { state.editMenu.undoBadgeText },
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
                        IconButton(onClick = { handler(OnEditorMenuToggled) }) {
                            Icon(
                                imageVector = if (state.editMenu.isDisplayed) Icons.Filled.AutoFixOff else Icons.Filled.AutoFixNormal,
                                contentDescription = null
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onChooseImage
            ) {
                Icon(
                    imageVector = Icons.Filled.PermMedia,
                    contentDescription = null
                )
            }
        },
        bottomBar = {
            EditMenu(
                modifier = Modifier.fillMaxWidth(),
                menu = state.editMenu,
                handler = handler
            )
        }
    ) {
        val layoutDirection = LocalLayoutDirection.current
        // ignore bottom padding, let bottom bar overlap the SurfaceView
        val padding = PaddingValues(
            start = it.calculateStartPadding(layoutDirection),
            end = it.calculateEndPadding(layoutDirection),
            top = it.calculateTopPadding()
        )
        Surface(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.image == null) {
                    Text(text = stringResource(R.string.message_no_image))
                } else {
                    AndroidView({ context ->
                        AppGLSurfaceView(
                            context,
                            state.image,
                            state.editMenu.current
                        )
                    }) { view ->
                        with(view.renderer) {
                            image = state.image
                            transformations = state.editMenu.displayTransformations
                        }
                    }
                }
            }
        }

    }
}

context (Context)
private val EditMenu.undoBadgeText: String
    get() = if (previous.size > MaxDisplayUndoOperations) getString(R.string.badge_operations_overflow) else previous.size.toString()

@Preview
@Composable
fun AppPreview() {
    AppTheme(darkTheme = false) {
        App(
            state = AppState(),
            handler = {},
            onChooseImage = {}
        )
    }
}