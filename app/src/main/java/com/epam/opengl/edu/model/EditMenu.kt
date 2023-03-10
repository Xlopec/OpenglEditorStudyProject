package com.epam.opengl.edu.model

import kotlin.reflect.KClass

data class EditMenu(
    val state: EditorState = Hidden,
    val current: Transformations = Transformations(),
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

fun EditMenu.undoLastTransformation() =
    if (canUndoTransformations) {
        copy(
            current = previous.last(),
            previous = previous.subList(0, previous.size - 1),
        )
    } else {
        this
    }

fun EditMenu.updateTransformation(
    transformation: Transformation,
) = copy(
    state = EditTransformation(
        which = transformation::class,
        edited = current + transformation,
    )
)

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