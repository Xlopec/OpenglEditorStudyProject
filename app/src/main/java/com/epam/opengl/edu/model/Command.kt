package com.epam.opengl.edu.model

import kotlin.reflect.KClass

sealed interface Command

@JvmInline
value class TransformationApplied(
    val which: KClass<out Transformation>,
) : Command