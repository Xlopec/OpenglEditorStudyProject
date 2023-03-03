package com.epam.opengl.edu.model

import android.net.Uri
import androidx.compose.runtime.Stable
import com.epam.opengl.edu.model.geometry.Size

@Stable
@JvmInline
value class AppState(
    val editMenu: EditMenu? = null,
)

fun AppState.withEditMenu(
    modify: EditMenu.() -> EditMenu,
) = AppState(editMenu = requireNotNull(editMenu) { "Can't update app state, image wasn't loaded" }.run(modify))

fun AppState.onImageOrViewportUpdated(
    image: Uri,
    viewport: Size,
): AppState = AppState(editMenu = editMenu.updateViewportAndImageOrCreate(image, viewport))


