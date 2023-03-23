package com.epam.opengl.edu.model

fun update(
    message: Message,
    state: AppState,
): AppState =
    when (message) {
        is OnEditorMenuToggled -> state.withEditMenu { toggleState() }
        is OnImageSelected -> state.withImage(message.image)
        is OnTransformationUpdated -> state.withEditMenu { updateTransformation(message.transformation) }
        is OnSwitchedToEditTransformation -> state.withEditMenu { switchToEditTransformationMode(message.which) }
        OnApplyChanges -> state.withEditMenu { applyEditedTransformation() }
        OnDiscardChanges -> state.withEditMenu { discardEditedTransformation() }
        OnUndoTransformation -> state.withEditMenu { undoLastTransformation() }
    }