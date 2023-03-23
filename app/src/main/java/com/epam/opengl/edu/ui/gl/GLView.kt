package com.epam.opengl.edu.ui.gl

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onViewportUpdated: (Size) -> Unit,
) {
    internal val view = GLSurfaceView(context).apply {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        preserveEGLContextOnPause = true
        holder.setFormat(PixelFormat.TRANSLUCENT)
    }

    internal val renderer = AppGLRenderer(
        context = context,
        view = view,
        editor = editor,
        onViewportSizeChange = onViewportUpdated,
        onFpsUpdated = {
            fps = it
        }
    )

    var fps by mutableStateOf(0U)
        private set

    var editor by renderer::editor

    var isDebugModeEnabled by renderer::isDebugModeEnabled

    suspend fun bitmap() = renderer.bitmap()

    suspend fun crop() = renderer.crop()
}

@Composable
fun rememberGlState(
    editor: Editor,
    onViewportUpdated: (Size) -> Unit,
): GLViewState {
    val context = LocalContext.current
    return remember { GLViewState(editor, context, onViewportUpdated) }
}

@Composable
fun GLView(
    state: GLViewState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.surface,
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
    ) {
        state.renderer.backgroundColor = backgroundColor
    }
}
