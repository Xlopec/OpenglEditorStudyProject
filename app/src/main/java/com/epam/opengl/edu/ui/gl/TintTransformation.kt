package com.epam.opengl.edu.ui.gl

import android.content.Context
import android.opengl.GLES31
import com.epam.opengl.edu.R
import com.epam.opengl.edu.model.transformation.Transformations
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL

class TintTransformation(
    private val context: Context,
    private val verticesCoordinates: FloatBuffer,
    private val textureCoordinates: FloatBuffer,
) : OpenglTransformation {

    context (GL)
    private val program by lazy {
        context.loadProgram(R.raw.no_transform_vertex, R.raw.frag_tint)
    }

    context (GL)
    override fun draw(
        transformations: Transformations,
        fbo: Int,
        texture: Int,
    ) {
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, fbo)
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        GLES31.glUseProgram(program)
        val positionHandle = GLES31.glGetAttribLocation(program, "aPosition")
        val texturePositionHandle = GLES31.glGetAttribLocation(program, "aTexPosition")
        val tintHandle = GLES31.glGetUniformLocation(program, "tint")
        GLES31.glVertexAttribPointer(texturePositionHandle, 2, GLES31.GL_FLOAT, false, 0, textureCoordinates)
        GLES31.glEnableVertexAttribArray(texturePositionHandle)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
        GLES31.glUniform1f(tintHandle, transformations.tint.value)
        GLES31.glVertexAttribPointer(positionHandle, 2, GLES31.GL_FLOAT, false, 0, verticesCoordinates)
        GLES31.glEnableVertexAttribArray(positionHandle)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4)
        GLES31.glDisableVertexAttribArray(positionHandle)
        GLES31.glDisableVertexAttribArray(texturePositionHandle)
    }

}