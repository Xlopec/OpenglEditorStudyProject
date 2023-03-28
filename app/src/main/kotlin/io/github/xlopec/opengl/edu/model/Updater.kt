package io.github.xlopec.opengl.edu.model

import io.github.xlopec.tea.core.Update
import io.github.xlopec.tea.core.command
import io.github.xlopec.tea.core.noCommand

fun update(
    message: Message,
    state: AppState,
): Update<AppState, Command> =
    when (message) {
        is OnEditorMenuToggled -> state.withEditor { toggleState() }.noCommand()
        is OnDataPrepared -> state.onImageOrViewportUpdated(message.image, message.imageSize, message.windowSize)
            .noCommand()

        is OnTransformationUpdated -> state.withEditor { updateTransformation(message.transformation) }.noCommand()
        is OnSwitchedToEditTransformation -> state.withEditor { switchToEditTransformationMode(message.which) }
            .noCommand()

        OnApplyChanges -> state.withEditor { applyEditedTransformation() }.noCommand()
        OnDiscardChanges -> state.withEditor { discardEditedTransformation() }.noCommand()
        OnUndoTransformation -> state.withEditor { undoLastTransformation() }.noCommand()
        OnExportImage -> state.withEditor { onImageExportStart() } command DoExportImage()
        is OnImageExported -> state.withEditor { onImageExportedFinished() } command NotifyImageExported(message.filename, message.path)
        is OnImageExportException -> state.withEditor { onImageExportedFinished() } command NotifyException(message.th)
        is OnDebugModeChanged -> state.onDebugModeUpdated(message.isDebugModeEnabled) command StoreDebugMode(message.isDebugModeEnabled)
    }