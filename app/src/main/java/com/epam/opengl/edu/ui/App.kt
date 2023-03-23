package com.epam.opengl.edu.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult.ActionPerformed
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import com.epam.opengl.edu.MimeTypePng
import com.epam.opengl.edu.R
import com.epam.opengl.edu.model.AppState
import com.epam.opengl.edu.model.Command
import com.epam.opengl.edu.model.DoExportImage
import com.epam.opengl.edu.model.Message
import com.epam.opengl.edu.model.NotifyException
import com.epam.opengl.edu.model.NotifyImageExported
import com.epam.opengl.edu.model.NotifyTransformationApplied
import com.epam.opengl.edu.model.OnCropped
import com.epam.opengl.edu.model.OnDataPrepared
import com.epam.opengl.edu.model.OnImageExportException
import com.epam.opengl.edu.model.OnImageExported
import com.epam.opengl.edu.model.geometry.Size
import com.epam.opengl.edu.model.transformation.Scene
import com.epam.opengl.edu.saveBitmap
import com.epam.opengl.edu.ui.gl.GLView
import com.epam.opengl.edu.ui.gl.decodeImageSize
import com.epam.opengl.edu.ui.gl.rememberGlState
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
    val scaffoldState = rememberScaffoldState()
    val editor = snapshot.currentState.editor
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
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        topBar = {
            InsetsAwareToolbar(
                title = { Text(text = stringResource(R.string.app_name)) },
                actions = {
                    if (editor != null) {
                        AppToolbarActions(
                            editor = editor,
                            handler = handler,
                        )
                    }
                }
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
                contentAlignment = Alignment.Center
            ) {
                val density = LocalDensity.current

                SideEffect {
                    with(density) {
                        viewportSize = Size(maxWidth.toPx().roundToInt(), maxHeight.toPx().roundToInt())
                    }
                }

                if (editor == null) {
                    Text(text = stringResource(R.string.message_no_image))
                } else {
                    val glState = rememberGlState(
                        editor = editor,
                        onCropped = { handler(OnCropped) },
                        onViewportUpdated = { viewport ->
                            viewportSize = viewport
                        }
                    )

                    val cropRequested = snapshot.commands.any { cmd -> cmd.cropRequested }

                    if (cropRequested) {
                        LaunchedEffect(Unit) {
                            glState.requestCrop()
                        }
                    }

                    LaunchedEffect(editor) {
                        glState.editor = editor
                    }

                    NotificationsHandler(snapshot, scaffoldState)

                    val exportCommand: DoExportImage? = snapshot.commands.firstInstanceOfOrNull()

                    if (exportCommand != null) {
                        LaunchedEffect(Unit) {
                            var bitmap: Bitmap? = null
                            try {
                                bitmap = glState.bitmap()
                                handler(
                                    OnImageExported(
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
                }
            }
        }
    }
}


@Composable
private fun NotificationsHandler(
    snapshot: Snapshot<Message, AppState, Command>,
    scaffoldState: ScaffoldState,
) {
    val context = LocalContext.current
    val exceptionNotification: NotifyException? = snapshot.commands.firstInstanceOfOrNull()

    if (exceptionNotification != null) {
        LaunchedEffect(Unit) {
            scaffoldState.snackbarHostState.showSnackbar(
                message = "Couldn't save image ${exceptionNotification.th.message ?: ""}",
                duration = SnackbarDuration.Short
            )
        }
    }

    val exportNotification: NotifyImageExported? = snapshot.commands.firstInstanceOfOrNull()

    if (exportNotification != null) {
        LaunchedEffect(Unit) {
            val action = scaffoldState.snackbarHostState.showSnackbar(
                message = "Image was saved to ${exportNotification.filename}",
                actionLabel = "Open",
                duration = SnackbarDuration.Short
            )

            if (action == ActionPerformed) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    setDataAndType(exportNotification.path, MimeTypePng)
                }

                context.startActivity(intent)
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

private inline fun <reified C : A, A> Collection<A>.firstInstanceOfOrNull() = find { it is C } as? C