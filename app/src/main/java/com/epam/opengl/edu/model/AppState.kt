package com.epam.opengl.edu.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class AppState(
    val editMenu: EditMenu = EditMenu(),
    val image: Uri? = null,
)

fun AppState.withEditMenu(
    menu: EditMenu,
) = copy(editMenu = menu)

fun AppState.withEditMenu(
    modify: EditMenu.() -> EditMenu
) = copy(editMenu = editMenu.run(modify))

fun AppState.withImage(
    image: Uri?,
) = copy(
    image = image,
    editMenu = EditMenu()
)


