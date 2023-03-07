package com.epam.opengl.edu.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.AutoFixOff
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.epam.opengl.edu.R
import com.epam.opengl.edu.model.AppState
import com.epam.opengl.edu.model.Command
import com.epam.opengl.edu.model.EditMenu
import com.epam.opengl.edu.model.Message
import com.epam.opengl.edu.model.OnCropped
import com.epam.opengl.edu.model.OnEditorMenuToggled
import com.epam.opengl.edu.model.OnUndoTransformation
import com.epam.opengl.edu.model.OnViewportAndImageUpdated
import com.epam.opengl.edu.model.TransformationApplied
import com.epam.opengl.edu.model.canUndoTransformations
import com.epam.opengl.edu.model.geometry.Size
import com.epam.opengl.edu.model.isDisplayed
import com.epam.opengl.edu.model.transformation.Scene
import com.epam.opengl.edu.ui.gl.AppGLRenderer
import com.epam.opengl.edu.ui.gl.asBitmap
import com.epam.opengl.edu.ui.theme.AppTheme
import io.github.xlopec.tea.core.Initial
import io.github.xlopec.tea.core.Snapshot
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

typealias MessageHandler = (Message) -> Unit

private const val MaxDisplayUndoOperations = 10

@Composable
fun App(
    snapshot: Snapshot<Message, AppState, Command>,
    handler: MessageHandler,
) {
    val export = remember { mutableStateOf(0) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var viewportSize by remember { mutableStateOf<Size?>(null) }
    val state = snapshot.currentState
    val context = LocalContext.current

    LaunchedEffect(selectedUri?.toString(), viewportSize) {
        val image = selectedUri
        val viewport = viewportSize

        if (image != null && viewport != null) {
            // fixme later
            val b = with(context) { image.asBitmap() }
            val size = Size(b.width, b.height)
            b.recycle()

            handler(OnViewportAndImageUpdated(image, size))
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.app_name))
                },
                actions = {
                    if (state.editMenu != null) {
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
                selectedUri = uri
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
            if (state.editMenu != null) {
                EditMenu(
                    modifier = Modifier.fillMaxWidth(),
                    menu = state.editMenu,
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

        BoxWithConstraints {
            val density = LocalDensity.current

            SideEffect {
                with(density) {
                    viewportSize = Size(maxWidth.toPx().roundToInt(), maxHeight.toPx().roundToInt())
                }
            }

            EditorContent(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                state = state,
                snapshot = snapshot,
                export = export,
                onCropped = { handler(OnCropped) },
                onViewportUpdated = { viewport ->
                    viewportSize = viewport
                }
            )
        }
    }
}

@Composable
private fun EditorContent(
    modifier: Modifier,
    state: AppState,
    snapshot: Snapshot<Message, AppState, Command>,
    export: MutableState<Int>,
    onCropped: () -> Unit,
    onViewportUpdated: (Size) -> Unit,
) {
    Surface(
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.editMenu == null) {
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

                val renderer = remember { AppGLRenderer(context, glView, onCropped, onViewportUpdated) }

                AndroidView({ glView.apply { setRenderer(renderer) } })
                LaunchedEffect(state.editMenu) {
                    renderer.state = state.editMenu
                }
                SideEffect {
                    if (snapshot.commands.any { it.cropRequested }) {
                        renderer.requestCrop()
                    }
                }
                if (export.value > 0) {
                    LaunchedEffect(export.value) {
                        val f = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "output.png"
                        )

                        withContext(Dispatchers.IO) {
                            FileOutputStream(f).use { fos ->
                                val bitmap = renderer.bitmap()

                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                                bitmap.recycle()
                            }

                            println("done")
                        }
                    }
                }
            }
        }
    }
}

private val Command.cropRequested: Boolean
    get() = this is TransformationApplied && which == Scene::class

context (Context)
        private val EditMenu.undoBadgeText: String
    get() = if (previous.size > MaxDisplayUndoOperations) getString(R.string.badge_operations_overflow) else previous.size.toString()

@Preview
@Composable
fun AppPreview() {
    AppTheme(darkTheme = false) {
        App(
            snapshot = Initial(AppState()),
        ) {}
    }
}