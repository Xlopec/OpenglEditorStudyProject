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

val EditMenu.displayTransformations: Transformations
    get() = when (state) {
        Displayed, Hidden -> current
        is EditTransformation -> state.edited
    }

fun EditMenu.updateGrayscale(
    value: Float,
) = copy(
    state = EditTransformation(
        which = Grayscale::class,
        edited = current + value.toGrayscale(),
    )
)

fun EditMenu.updateBrightness(
    value: Float,
) = copy(
    state = EditTransformation(
        which = Brightness::class,
        edited = current + value.toBrightness(),
    )
)

fun EditMenu.updateSaturation(
    value: Float,
) = copy(
    state = EditTransformation(
        which = Saturation::class,
        edited = current + value.toSaturation(),
    )
)

fun EditMenu.updateContrast(
    value: Float,
) = copy(
    state = EditTransformation(
        which = Contrast::class,
        edited = current + value.toContrast(),
    )
)

fun EditMenu.updateTint(
    value: Float,
) = copy(
    state = EditTransformation(
        which = Tint::class,
        edited = current + Tint(value),
    )
)

fun EditMenu.switchToEditTransformationMode(
    which: KClass<out Transformation>,
): EditMenu = copy(state = EditTransformation(which = which, edited = current))

fun EditMenu.applyEditedTransformation() = when (state) {
    Displayed, Hidden -> this
    is EditTransformation -> copy(
        state = Displayed, current = state.edited, previous = previous + state.edited
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