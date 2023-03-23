package com.epam.opengl.edu.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.epam.opengl.edu.model.transformation.Transformation
import kotlin.reflect.KClass

@Immutable
sealed interface Command

@JvmInline
value class DoExportImage(
    val filename: String
) : Command

data class NotifyImageExported(
    val filename: String,
    val path: Uri
) : Command

@JvmInline
value class NotifyException(
    val th: Throwable
) : Command

@JvmInline
value class NotifyTransformationApplied(
    val which: KClass<out Transformation>,
) : Command

@JvmInline
value class StoreDebugMode(
    val isDebugModeEnabled: Boolean
) : Command