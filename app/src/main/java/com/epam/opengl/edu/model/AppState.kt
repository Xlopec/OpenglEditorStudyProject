package com.epam.opengl.edu.model

import android.net.Uri
import androidx.compose.runtime.Stable
import com.epam.opengl.edu.model.geometry.Size

@Stable
data class AppState(
    val editor: Editor? = null,
    val isDebugModeEnabled: Boolean = true,
)

fun AppState.onDebugModeUpdated(
    isDebugModeEnabled: Boolean
) = copy(isDebugModeEnabled = isDebugModeEnabled)

fun AppState.withEditor(
    modify: Editor.() -> Editor,
) = copy(editor = requireNotNull(editor) { "Can't update app state, image wasn't loaded" }.run(modify))

fun AppState.onImageOrViewportUpdated(
    image: Uri,
    imageSize: Size,
    windowSize: Size,
): AppState = copy(
    editor = editor.updateViewportAndImageOrCreate(
        newImage = image,
        newImageSize = imageSize,
        newWindowSize = windowSize
    )
)


