package com.epam.opengl.edu.model

import io.github.xlopec.tea.core.Update
import io.github.xlopec.tea.core.noCommand

fun update(
    message: Message,
    state: AppState,
): Update<AppState, Command> =
    when (message) {
        is OnEditorMenuToggled -> state.withEditMenu { toggleState() }.noCommand()
        is OnImageSelected -> state.withImage(message.image).noCommand()
        is OnTransformationUpdated -> state.withEditMenu { updateTransformation(message.transformation) }.noCommand()
        is OnSwitchedToEditTransformation -> state.withEditMenu { switchToEditTransformationMode(message.which) }.noCommand()
        OnApplyChanges -> state.withEditMenu { applyEditedTransformation() }.noCommand()
        OnDiscardChanges -> state.withEditMenu { discardEditedTransformation() }.noCommand()
        OnUndoTransformation -> state.withEditMenu { undoLastTransformation() }.noCommand()
    }