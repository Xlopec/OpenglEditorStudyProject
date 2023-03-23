package com.epam.opengl.edu.ui.gl

import com.epam.opengl.edu.model.transformation.Transformations
import javax.microedition.khronos.opengles.GL

interface OpenglTransformation {

    context (GL)
    fun draw(
        transformations: Transformations,
        fbo: Int,
        texture: Int,
    )

}