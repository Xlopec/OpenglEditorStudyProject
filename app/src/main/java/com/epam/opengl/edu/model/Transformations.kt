package com.epam.opengl.edu.model

data class Transformations(
    val grayscale: Grayscale = Grayscale.Disabled,
    val brightness: Brightness = Brightness.Disabled,
    val saturation: Saturation = Saturation.Disabled,
    val contrast: Contrast = Contrast.Disabled,
    val tint: Tint = Tint.Disabled,
    val blur: GaussianBlur = GaussianBlur.Disabled,
)

sealed interface Transformation

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

operator fun Transformations.plus(
    transformation: Transformation,
): Transformations =
    when (transformation) {
        is Grayscale -> copy(grayscale = transformation)
        is Brightness -> copy(brightness = transformation)
        is Saturation -> copy(saturation = transformation)
        is Contrast -> copy(contrast = transformation)
        is Tint -> copy(tint = transformation)
        is GaussianBlur -> copy(blur = transformation)
    }
