package io.github.xlopec.opengl.edu.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import io.github.xlopec.opengl.edu.model.geometry.Size
import io.github.xlopec.opengl.edu.model.transformation.Transformation
import kotlin.reflect.KClass

@Immutable
sealed interface Message

object OnEditorMenuToggled : Message

object OnUndoTransformation : Message

@JvmInline
value class OnTransformationUpdated(
    val transformation: Transformation,
) : Message

@JvmInline
value class OnSwitchedToEditTransformation(
    val which: KClass<out Transformation>,
) : Message

object OnApplyChanges : Message

object OnDiscardChanges : Message

data class OnDataPrepared(
    val image: Uri,
    val imageSize: Size,
    val windowSize: Size,
) : Message

object OnCropped : Message

object OnExportImage : Message

data class OnImageExported(
    val path: Uri,
    val filename: String,
) : Message

@JvmInline
value class OnImageExportException(
    val th: Throwable,
) : Message

@JvmInline
value class OnDebugModeChanged(
    val isDebugModeEnabled: Boolean,
) : Message