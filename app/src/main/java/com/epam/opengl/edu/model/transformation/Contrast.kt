package com.epam.opengl.edu.model.transformation

@JvmInline
value class Contrast(
    val delta: Float,
) : Transformation {
    init {
        require(delta in -1f..1f) { "invalid contrast delta: $delta" }
    }

    companion object {
        val Min = Contrast(-1f)
        val Max = Contrast(1f)
        val Disabled = Contrast(0f)
    }
}