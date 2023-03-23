package com.epam.opengl.edu.ui.gl

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.epam.opengl.edu.model.Editor
import com.epam.opengl.edu.model.geometry.Size
import kotlinx.coroutines.awaitCancellation

class GLViewState(
    editor: Editor,
    context: Context,
    onCropped: () -> Unit,
    onViewportUpdated: (Size) -> Unit,
) {
    internal val view = GLSurfaceView(context).apply {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        preserveEGLContextOnPause = true
        holder.setFormat(PixelFormat.TRANSLUCENT)
    }

    internal val renderer = AppGLRenderer(context, view, editor, onCropped, onViewportUpdated)

    var editor by renderer::editor

    suspend fun bitmap() = renderer.bitmap()

    fun requestCrop() = renderer.requestCrop()
}

@Composable
fun rememberGlState(
    editor: Editor,
    onCropped: () -> Unit,
    onViewportUpdated: (Size) -> Unit,
): GLViewState {
    val context = LocalContext.current
    return remember { GLViewState(editor, context, onCropped, onViewportUpdated) }
}

@Composable
fun GLView(
    state: GLViewState,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try {
                state.view.onResume()
                awaitCancellation()
            } finally {
                state.view.onPause()
            }
        }
    }

    AndroidView(
        modifier = modifier,
        // fixme once we set renderer gl thread starts
        factory = { state.view.apply { setRenderer(state.renderer) } }
    )
}
