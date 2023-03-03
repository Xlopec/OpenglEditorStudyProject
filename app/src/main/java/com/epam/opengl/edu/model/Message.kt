package com.epam.opengl.edu.model

import android.net.Uri
import com.epam.opengl.edu.ui.gl.TouchHelper
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

@JvmInline
value class OnImageSelected(
    val image: Uri,
) : Message

@JvmInline
value class OnTouchHelperUpdated(
    val helper: TouchHelper,
) : Message