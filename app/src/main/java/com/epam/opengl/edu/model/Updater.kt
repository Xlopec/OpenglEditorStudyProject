package com.epam.opengl.edu.model

fun update(
    message: Message,
    state: AppState,
): AppState =
    when (message) {
        is OnEditorMenuToggled -> state.withEditMenu { toggleState() }
        is OnImageSelected -> state.withImage(message.image)
        is OnGrayscaleUpdated -> state.withEditMenu { updateGrayscale(message.value) }
        is OnSwitchedToEditTransformation -> state.withEditMenu { switchToEditTransformationMode(message.which) }
        OnApplyChanges -> state.withEditMenu { applyEditedTransformation() }
        OnDiscardChanges -> state.withEditMenu { discardEditedTransformation() }
    }