package io.github.xlopec.opengl.edu.model.transformation

@JvmInline
value class Brightness(
    val delta: Float,
) : Transformation {
    init {
        require(delta in -1f..1f) { "invalid brightness delta: $delta" }
    }

    companion object {
        val Min = Brightness(-1f)
        val Max = Brightness(1f)
        val Disabled = Brightness(0f)
    }
}