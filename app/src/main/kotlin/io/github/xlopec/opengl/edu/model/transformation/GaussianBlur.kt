package io.github.xlopec.opengl.edu.model.transformation

data class GaussianBlur(
    val sigma: Int,
    val radius: Int,
) : Transformation {
    companion object {
        val Min = GaussianBlur(0, 0)
        val Max = GaussianBlur(20, 20)
        val Disabled = Min
    }

    init {
        require(sigma >= 0) { "sigma < 0, $sigma" }
        require(radius >= 0) { "radius < 0, $radius" }
    }
}