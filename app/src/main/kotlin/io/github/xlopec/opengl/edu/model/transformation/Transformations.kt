package io.github.xlopec.opengl.edu.model.transformation

data class Transformations(
    val scene: Scene,
    val grayscale: Grayscale = Grayscale.Disabled,
    val brightness: Brightness = Brightness.Disabled,
    val saturation: Saturation = Saturation.Disabled,
    val contrast: Contrast = Contrast.Disabled,
    val tint: Tint = Tint.Disabled,
    val blur: GaussianBlur = GaussianBlur.Disabled,
)