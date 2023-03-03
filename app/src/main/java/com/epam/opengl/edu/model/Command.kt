package com.epam.opengl.edu.model

import com.epam.opengl.edu.model.transformation.Transformation
import kotlin.reflect.KClass

sealed interface Command

@JvmInline
value class TransformationApplied(
    val which: KClass<out Transformation>,
) : Command