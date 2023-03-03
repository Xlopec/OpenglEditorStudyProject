package com.epam.opengl.edu.model.transformation

@JvmInline
value class Saturation(
    val delta: Float,
) : Transformation {
    init {
        require(delta in -1f..1f) { "invalid saturation delta: $delta" }
    }

    companion object {
        val Min = Saturation(-1f)
        val Max = Saturation(1f)
        val Disabled = Saturation(0f)
    }
}