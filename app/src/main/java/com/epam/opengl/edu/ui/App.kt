package com.epam.opengl.edu.ui

import android.content.Context
import android.graphics.PixelFormat
import android.net.Uri
import android.opengl.GLSurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.epam.opengl.edu.R
import com.epam.opengl.edu.model.*
import com.epam.opengl.edu.ui.gl.AppGLRenderer
import com.epam.opengl.edu.ui.theme.AppTheme

typealias MessageHandler = (Message) -> Unit

private const val MaxDisplayUndoOperations = 10

@Composable
fun App(
    state: AppState,
    handler: MessageHandler,
) {

    val export = remember { mutableStateOf(0) }

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
                        IconButton(
                            onClick = {
                                export.value += 1
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.UploadFile,
                                contentDescription = null
                            )
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
            val chooserLauncher = rememberLauncherForActivityResult(GetContent()) { uri: Uri? ->
                if (uri != null) {
                    handler(OnImageSelected(uri))
                }
            }

            FloatingActionButton(
                onClick = { chooserLauncher.launch("image/*") }
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
                    val context = LocalContext.current
                    val glView = remember {
                        GLSurfaceView(context).apply {
                            setEGLContextClientVersion(3)
                            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                            holder.setFormat(PixelFormat.TRANSLUCENT)
                        }
                    }

                    val renderer = remember { AppGLRenderer(context, state.image, state.editMenu.current, glView, handler) }

                    AndroidView({ glView.apply { setRenderer(renderer) } })

                    LaunchedEffect(state.editMenu.displayTransformations) {
                        renderer.transformations = state.editMenu.displayTransformations
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
        )
    }
}