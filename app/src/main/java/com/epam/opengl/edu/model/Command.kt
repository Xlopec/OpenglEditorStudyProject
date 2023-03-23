package com.epam.opengl.edu.model

import android.net.Uri
import com.epam.opengl.edu.model.transformation.Transformation
import kotlin.reflect.KClass

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