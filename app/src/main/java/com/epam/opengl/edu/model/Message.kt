package com.epam.opengl.edu.model

import android.net.Uri
import com.epam.opengl.edu.model.geometry.Size
import com.epam.opengl.edu.model.transformation.Transformation
import kotlin.reflect.KClass

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

@JvmInline
value class OnImageExported(
    val path: Uri,
) : Message

@JvmInline
value class OnImageExportException(
    val th: Throwable,
) : Message