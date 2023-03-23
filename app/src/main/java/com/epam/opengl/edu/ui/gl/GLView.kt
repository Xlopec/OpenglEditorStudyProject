package com.epam.opengl.edu.ui.gl

import android.content.Context
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
    internal val view = AppGLSurfaceView(
        context = context,
        editor = editor,
        onViewportUpdated = onViewportUpdated,
        onFpsUpdated = { fps = it }
    )

    var fps by mutableStateOf(0U)
        private set

    var editor by view::editor

    var isDebugModeEnabled by view::isDebugModeEnabled

    suspend fun bitmap() = view.bitmap()

    suspend fun crop() = view.crop()
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
        factory = { state.view }
    ) { view ->
        view.backgroundColor = backgroundColor
    }
}
