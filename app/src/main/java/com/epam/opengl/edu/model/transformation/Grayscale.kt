package com.epam.opengl.edu.model.transformation

@JvmInline
value class Grayscale(
    val value: Float,
) : Transformation {

    init {
        require(value in 0f..1f) { "invalid grayscale: $value" }
    }

    companion object {
        val Min = Grayscale(0f)
        val Max = Grayscale(1f)
        val Disabled = Min
    }

}