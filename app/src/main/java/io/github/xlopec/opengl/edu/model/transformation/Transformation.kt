package io.github.xlopec.opengl.edu.model.transformation

sealed interface Transformation

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
        is Scene -> copy(scene = transformation)
    }
