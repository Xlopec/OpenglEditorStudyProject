package io.github.xlopec.opengl.edu.ui.gl

import io.github.xlopec.opengl.edu.model.transformation.Transformations
import javax.microedition.khronos.opengles.GL

interface OpenglTransformation {

    context (GL)
    fun draw(
        transformations: Transformations,
        frameBuffer: FrameBuffer,
        sourceTexture: Texture,
    )

}