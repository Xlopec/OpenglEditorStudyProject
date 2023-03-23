package com.epam.opengl.edu.model.transformation

@JvmInline
value class Tint(
    val value: Float,
) : Transformation {
    companion object {
        val Min = Tint(-1f)
        val Max = Tint(1f)
        val Disabled = Tint(0f)
    }
}