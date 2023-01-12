package com.epam.opengl.edu.model

data class Transformations(
    val grayscale: Grayscale = Grayscale(0f),
)

sealed interface Transformation

@JvmInline
value class Grayscale(val value: Float) : Transformation {

    init {
        require(value in 0f..1f) { "invalid grayscale: $value" }
    }
}

fun Float.toGrayscale() = Grayscale(this)

operator fun Transformations.plus(
    transformation: Transformation,
): Transformations =
    when (transformation) {
        is Grayscale -> copy(grayscale = transformation)
    }
