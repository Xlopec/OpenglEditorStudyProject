package com.epam.opengl.edu.model

import android.net.Uri
import com.epam.opengl.edu.model.geometry.Size
import com.epam.opengl.edu.model.transformation.Scene
import com.epam.opengl.edu.model.transformation.Transformation
import com.epam.opengl.edu.model.transformation.Transformations
import com.epam.opengl.edu.model.transformation.onCropped
import com.epam.opengl.edu.model.transformation.plus
import kotlin.reflect.KClass

data class EditMenu(
    val image: Uri,
    val current: Transformations,
    val state: EditorState = Hidden,
    val previous: List<Transformations> = listOf(),
)

sealed interface EditorState

object Hidden : EditorState

object Displayed : EditorState

data class EditTransformation(
    val which: KClass<out Transformation>,
    val edited: Transformations,
) : EditorState

val EditMenu.isDisplayed: Boolean
    get() = state !== Hidden

val EditMenu.canUndoTransformations: Boolean
    get() = previous.isNotEmpty()

val EditMenu.displayTransformations: Transformations
    get() = when (state) {
        Displayed, Hidden -> current
        is EditTransformation -> state.edited
    }

val EditMenu.displayCropSelection: Boolean
    get() = state is EditTransformation && state.which == Scene::class

fun EditMenu.undoLastTransformation() =
    if (canUndoTransformations) {
        copy(
            current = previous.last(),
            previous = previous.subList(0, previous.size - 1),
        )
    } else {
        this
    }

fun EditMenu?.updateViewportAndImageOrCreate(
    newImage: Uri,
    newImageSize: Size,
    newWindowSize: Size,
): EditMenu = this?.updateViewportAndImage(newImage, newImageSize, newWindowSize) ?: EditMenu(
    image = newImage,
    current = Transformations(scene = Scene(image = newImageSize, window = newWindowSize))
)

fun EditMenu.updateViewportAndImage(
    newImage: Uri,
    newImageSize: Size,
    newWindowSize: Size,
): EditMenu {
    val updated = if (newImage != image) {
        copy(image = newImage).updateTransformation(Scene(image = newImageSize, window = newWindowSize))
    } else {
        this
    }

    return if (newImage == image && newWindowSize != displayTransformations.scene.window) {
        updated.updateTransformation(Scene(image = newImageSize, window = newWindowSize))
    } else {
        updated
    }
}

fun EditMenu.updateCropped() = updateTransformation(displayTransformations.scene.onCropped())

fun EditMenu.updateTransformation(
    transformation: Transformation,
) = when (state) {
    Displayed, Hidden -> copy(current = current + transformation)
    is EditTransformation -> copy(
        state = EditTransformation(
            which = transformation::class,
            edited = current + transformation,
        )
    )
}

fun EditMenu.switchToEditTransformationMode(
    which: KClass<out Transformation>,
): EditMenu = copy(state = EditTransformation(which = which, edited = current))

fun EditMenu.applyEditedTransformation() = when (state) {
    Displayed, Hidden -> this
    is EditTransformation -> copy(
        state = Displayed,
        current = state.edited,
        previous = previous + current
    )
}

fun EditMenu.discardEditedTransformation() = when (state) {
    Displayed, Hidden -> this
    is EditTransformation -> copy(state = Displayed)
}

fun EditMenu.toggleState() = copy(
    state = when (state) {
        Hidden -> Displayed
        is EditTransformation, Displayed -> Hidden
    }
)