package com.epam.opengl.edu.model

import io.github.xlopec.tea.core.Update
import io.github.xlopec.tea.core.command
import io.github.xlopec.tea.core.noCommand

fun update(
    message: Message,
    state: AppState,
): Update<AppState, Command> =
    when (message) {
        is OnEditorMenuToggled -> state.withEditMenu { toggleState() }.noCommand()
        is OnViewportAndImageUpdated -> state.onImageOrViewportUpdated(message.image, message.viewport).noCommand()
        is OnTransformationUpdated -> state.withEditMenu { updateTransformation(message.transformation) }.noCommand()
        is OnSwitchedToEditTransformation -> state.withEditMenu { switchToEditTransformationMode(message.which) }.noCommand()
        OnApplyChanges -> state.withEditMenu { applyEditedTransformation() }
            .command(setOfNotNull((state.editMenu!!.state as? EditTransformation)?.which?.let(::TransformationApplied)))

        OnDiscardChanges -> state.withEditMenu { discardEditedTransformation() }.noCommand()
        OnUndoTransformation -> state.withEditMenu { undoLastTransformation() }.noCommand()
        is OnSceneUpdated -> state.withEditMenu { updateTransformation(message.scene) }.noCommand()
        OnCropped -> state.withEditMenu { updateCropped() }.noCommand()
    }