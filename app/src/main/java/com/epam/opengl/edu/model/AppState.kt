package com.epam.opengl.edu.model

import android.net.Uri
import androidx.compose.runtime.Stable
import com.epam.opengl.edu.model.geometry.Size

@Stable
@JvmInline
value class AppState(
    val editor: Editor? = null,
)

fun AppState.withEditor(
    modify: Editor.() -> Editor,
) = AppState(editor = requireNotNull(editor) { "Can't update app state, image wasn't loaded" }.run(modify))

fun AppState.onImageOrViewportUpdated(
    image: Uri,
    imageSize: Size,
    windowSize: Size,
): AppState = AppState(
    editor = editor.updateViewportAndImageOrCreate(
        newImage = image,
        newImageSize = imageSize,
        newWindowSize = windowSize
    )
)


