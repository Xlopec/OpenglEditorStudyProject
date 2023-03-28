package io.github.xlopec.opengl.edu.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import io.github.xlopec.opengl.edu.model.geometry.Size
import io.github.xlopec.opengl.edu.model.transformation.*
import kotlin.reflect.KClass

@Stable
data class Editor(
    val image: Uri,
    val current: Transformations,
    val state: EditorState = Hidden,
    val previous: List<Transformations> = listOf(),
    val isExportingImage: Boolean = false,
)

@Immutable
sealed interface EditorState

object Hidden : EditorState

object Displayed : EditorState

data class EditTransformation(
    val which: KClass<out Transformation>,
    val edited: Transformations,
) : EditorState

val Editor.isDisplayed: Boolean
    get() = state !== Hidden

val Editor.canUndoTransformations: Boolean
    get() = previous.isNotEmpty()

val Editor.displayTransformations: Transformations
    get() = when (state) {
        Displayed, Hidden -> current
        is EditTransformation -> state.edited
    }

val Editor.displayCropSelection: Boolean
    get() = state is EditTransformation && state.which == Scene::class

fun Editor.onImageExportStart() = copy(isExportingImage = true)

fun Editor.onImageExportedFinished() = copy(isExportingImage = false)

fun Editor.undoLastTransformation() =
    if (canUndoTransformations) {
        copy(
            current = previous.last(),
            previous = previous.subList(0, previous.size - 1),
        )
    } else {
        this
    }

fun Editor?.updateViewportAndImageOrCreate(
    newImage: Uri,
    newImageSize: Size,
    newWindowSize: Size,
): Editor = this?.updateViewportAndImage(newImage, newImageSize, newWindowSize) ?: Editor(
    image = newImage,
    current = Transformations(scene = Scene(imageSize = newImageSize, windowSize = newWindowSize))
)

fun Editor.updateViewportAndImage(
    newImage: Uri,
    newImageSize: Size,
    newWindowSize: Size,
): Editor {
    val updated = if (newImage != image) {
        copy(image = newImage).updateTransformation(Scene(imageSize = newImageSize, windowSize = newWindowSize))
    } else {
        this
    }

    return if (newImage == image && newWindowSize != displayTransformations.scene.windowSize) {
        updated.updateTransformation(Scene(imageSize = newImageSize, windowSize = newWindowSize))
    } else {
        updated
    }
}

fun Editor.updateCropped() = updateTransformation(displayTransformations.scene.onCropped())

fun Editor.updateTransformation(
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

fun Editor.switchToEditTransformationMode(
    which: KClass<out Transformation>,
): Editor = copy(state = EditTransformation(which = which, edited = current))

fun Editor.applyEditedTransformation() = when (state) {
    Displayed, Hidden -> this
    is EditTransformation -> copy(
        state = Displayed,
        current = state.edited,
        // todo add support for reverting Scene transformations
        previous = if (state.which == Scene::class) {
            previous
        } else {
            previous + current
        }
    )
}

fun Editor.discardEditedTransformation() = when (state) {
    Displayed, Hidden -> this
    is EditTransformation -> copy(state = Displayed)
}

fun Editor.toggleState() = copy(
    state = when (state) {
        Hidden -> Displayed
        is EditTransformation, Displayed -> Hidden
    }
)