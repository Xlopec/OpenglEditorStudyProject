package com.epam.opengl.edu.model

import android.net.Uri
import kotlin.reflect.KClass

sealed interface Message

object OnEditorMenuToggled : Message

@JvmInline
value class OnGrayscaleUpdated(
    val value: Float,
) : Message

@JvmInline
value class OnBrightnessUpdated(
    val value: Float,
) : Message

@JvmInline
value class OnSaturationUpdated(
    val value: Float,
) : Message

@JvmInline
value class OnContrastUpdated(
    val value: Float,
) : Message

@JvmInline
value class OnTintUpdated(
    val value: Float,
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