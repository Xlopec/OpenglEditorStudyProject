package com.epam.opengl.edu.model

import androidx.compose.ui.graphics.Color

data class Transformations(
    val grayscale: Grayscale = Grayscale(0f),
    val brightness: Brightness = Brightness(0f),
    val saturation: Saturation = Saturation(0f),
    val contrast: Contrast = Contrast(0f),
    val tint: Tint = Tint(Tint.InitialColor),
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
    }
}

@JvmInline
value class Tint(
    val color: Color,
) : Transformation {
    companion object {
        val InitialColor = Color.White.copy(alpha = 0f)
    }
}

fun Float.toGrayscale() = Grayscale(this)

fun Float.toBrightness() = Brightness(this)

fun Float.toSaturation() = Saturation(this)

fun Float.toContrast() = Contrast(this)

operator fun Transformations.plus(
    transformation: Transformation,
): Transformations =
    when (transformation) {
        is Grayscale -> copy(grayscale = transformation)
        is Brightness -> copy(brightness = transformation)
        is Saturation -> copy(saturation = transformation)
        is Contrast -> copy(contrast = transformation)
        is Tint -> copy(tint = transformation)
    }
