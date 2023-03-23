package com.epam.opengl.edu.ui.gl

import android.content.Context
import android.opengl.GLES31
import com.epam.opengl.edu.R
import com.epam.opengl.edu.model.Brightness
import com.epam.opengl.edu.model.Saturation
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL

class HsvTransformation(
    private val context: Context,
    private val verticesCoordinates: FloatBuffer,
    private val textureCoordinates: FloatBuffer,
) {

    context (GL)
    private val program by lazy {
        context.loadProgram(R.raw.no_transform_vertex, R.raw.frag_hsv)
    }

    context (GL)
    fun render(
        brightness: Brightness,
        saturation: Saturation,
        targetFbo: Int,
        sourceTexture: Int,
    ) {
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, targetFbo)
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        GLES31.glUseProgram(program)
        val positionHandle = GLES31.glGetAttribLocation(program, "aPosition")
        val texturePositionHandle = GLES31.glGetAttribLocation(program, "aTexPosition")
        val brightnessHandle = GLES31.glGetUniformLocation(program, "brightness")
        val saturationHandle = GLES31.glGetUniformLocation(program, "saturation")
        GLES31.glVertexAttribPointer(texturePositionHandle, 2, GLES31.GL_FLOAT, false, 0, textureCoordinates)
        GLES31.glEnableVertexAttribArray(texturePositionHandle)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, sourceTexture)
        GLES31.glUniform1f(brightnessHandle, brightness.delta)
        GLES31.glUniform1f(saturationHandle, saturation.delta)
        GLES31.glVertexAttribPointer(positionHandle, 2, GLES31.GL_FLOAT, false, 0, verticesCoordinates)
        GLES31.glEnableVertexAttribArray(positionHandle)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4)
        GLES31.glDisableVertexAttribArray(positionHandle)
        GLES31.glDisableVertexAttribArray(texturePositionHandle)
    }

}