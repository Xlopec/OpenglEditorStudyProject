package io.github.xlopec.opengl.edu.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.xlopec.opengl.edu.MimeTypePng
import io.github.xlopec.opengl.edu.R
import io.github.xlopec.opengl.edu.model.*
import io.github.xlopec.opengl.edu.model.geometry.Size
import io.github.xlopec.opengl.edu.model.transformation.Scene
import io.github.xlopec.opengl.edu.saveBitmap
import io.github.xlopec.opengl.edu.ui.gl.GLView
import io.github.xlopec.opengl.edu.ui.gl.decodeImageSize
import io.github.xlopec.opengl.edu.ui.gl.rememberGlState
import io.github.xlopec.tea.core.Snapshot
import java.io.IOException
import kotlin.math.roundToInt

typealias MessageHandler = (Message) -> Unit

@Composable
fun App(
    snapshot: Snapshot<Message, AppState, Command>,
    handler: MessageHandler,
) {
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var viewportSize by remember { mutableStateOf<Size?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scaffoldState = rememberScaffoldState(drawerState = drawerState)
    val app = snapshot.currentState
    val editor = app.editor
    val context = LocalContext.current

    LaunchedEffect(selectedUri?.toString(), viewportSize) {
        val image = selectedUri
        val viewport = viewportSize

        if (image != null && viewport != null) {
            // fixme image might not always needs to be loaded
            val imageSize = with(context) { image.decodeImageSize() }
            handler(OnDataPrepared(image = image, imageSize = imageSize, windowSize = viewport))
        }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        topBar = {
            AppToolbar(
                editor = editor,
                drawer = drawerState,
                handler = handler
            )
        },
        drawerGesturesEnabled = editor == null || drawerState.isOpen,
        drawerContent = {
            AppDrawer(
                state = app,
                handler = handler
            )
        },
        floatingActionButton = {
            val chooserLauncher = rememberLauncherForActivityResult(GetContent()) { uri: Uri? ->
                selectedUri = uri
            }

            FloatingActionButton(
                onClick = {
                    if (editor?.isExportingImage != true) {
                        chooserLauncher.launch("image/*")
                    }
                }
            ) {
                Icon(
                    modifier = Modifier.animateRotationIfTrue(editor?.isExportingImage == true),
                    imageVector = if (editor?.isExportingImage == true) Icons.Filled.Cached else Icons.Filled.PermMedia,
                    contentDescription = null
                )
            }
        },
        bottomBar = {
            if (editor != null) {
                AppBottomBar(
                    modifier = Modifier.fillMaxWidth(),
                    editor = editor,
                    handler = handler
                )
            }
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
            BoxWithConstraints(
                contentAlignment = Alignment.TopEnd
            ) {
                val density = LocalDensity.current

                SideEffect {
                    with(density) {
                        viewportSize = Size(maxWidth.toPx().roundToInt(), maxHeight.toPx().roundToInt())
                    }
                }

                if (editor == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = stringResource(R.string.message_no_image))
                    }
                } else {
                    val glState = rememberGlState(
                        editor = editor,
                        onViewportUpdated = { viewport ->
                            viewportSize = viewport
                        }
                    )

                    val cropRequested = snapshot.commands.any { cmd -> cmd.cropRequested }

                    if (cropRequested) {
                        LaunchedEffect(Unit) {
                            glState.crop()
                            handler(OnCropped)
                        }
                    }

                    LaunchedEffect(editor) {
                        glState.editor = editor
                    }
                    LaunchedEffect(app.isDebugModeEnabled) {
                        glState.isDebugModeEnabled = app.isDebugModeEnabled
                    }

                    AppNotificationsHandler(snapshot, scaffoldState)

                    val exportCommand: DoExportImage? = snapshot.commands.firstInstanceOfOrNull()

                    if (exportCommand != null) {
                        LaunchedEffect(Unit) {
                            var bitmap: Bitmap? = null
                            try {
                                bitmap = glState.exportFrame()
                                handler(
                                    OnImageExported(
                                        filename = exportCommand.filename,
                                        path = context.saveBitmap(
                                            bitmap = bitmap,
                                            filename = exportCommand.filename,
                                            format = Bitmap.CompressFormat.PNG,
                                            mimeType = MimeTypePng,
                                        )
                                    )
                                )
                            } catch (io: IOException) {
                                handler(OnImageExportException(io))
                            } finally {
                                bitmap?.recycle()
                            }
                        }
                    }

                    GLView(
                        state = glState,
                    )

                    if (app.isDebugModeEnabled) {
                        Surface(
                            modifier = Modifier
                                .alpha(0.8f)
                                .padding(8.dp)
                        ) {
                            Text(text = stringResource(R.string.debug_message_fps_counter, glState.fps.toInt()))
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.animateRotationIfTrue(
    condition: Boolean,
) = composed {
    if (condition) {
        val transition = rememberInfiniteTransition()
        val alpha = transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 2000
                    0f at 0
                    360f at durationMillis
                },
                repeatMode = RepeatMode.Restart
            )
        )
        rotate(alpha.value)
    } else {
        Modifier
    }
}

private val Command.cropRequested: Boolean
    get() = this is NotifyTransformationApplied && which == Scene::class
