package io.github.xlopec.opengl.edu.ui.gl

import android.content.Context
import android.opengl.GLES31
import io.github.xlopec.opengl.edu.R
import io.github.xlopec.opengl.edu.model.geometry.Size
import io.github.xlopec.opengl.edu.model.geometry.height
import io.github.xlopec.opengl.edu.model.geometry.width
import io.github.xlopec.opengl.edu.model.transformation.isPortrait
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL

class ViewTransformation(
    private val context: Context,
    private val verticesCoordinates: FloatBuffer,
    private val textureCoordinates: FloatBuffer,
) {

    context (GL)
            private val program by lazy {
        context.loadProgram(R.raw.projection_vertex, R.raw.frag_normal)
    }

    context (GL)
    fun render(
        vPMatrix: FloatArray,
        targetFbo: Int,
        sourceTexture: Int,
        textureSize: Size,
        windowSize: Size,
    ) {
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, targetFbo)
        GLES31.glViewport(0, 0, windowSize.width, windowSize.height)
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        GLES31.glUseProgram(program)
        val vPMatrixHandle = GLES31.glGetUniformLocation(program, "uMVPMatrix")
        val positionHandle = GLES31.glGetAttribLocation(program, "aPosition")
        val textureRatioHandle = GLES31.glGetUniformLocation(program, "textureRatio")
        val textureHandle = GLES31.glGetUniformLocation(program, "uTexture")
        val texturePositionHandle = GLES31.glGetAttribLocation(program, "aTexPosition")
        GLES31.glVertexAttribPointer(texturePositionHandle, 2, GLES31.GL_FLOAT, false, 0, textureCoordinates)
        GLES31.glEnableVertexAttribArray(texturePositionHandle)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, sourceTexture)
        GLES31.glUniform1i(textureHandle, 0)

        if (textureSize.isPortrait) {
            GLES31.glUniform2f(textureRatioHandle, 1f / (textureSize.width.toFloat() / textureSize.height), 1f)
        } else {
            GLES31.glUniform2f(textureRatioHandle, 1f, 1f / (textureSize.height.toFloat() / textureSize.width))
        }

        GLES31.glUniformMatrix4fv(vPMatrixHandle, 1, false, vPMatrix, 0)
        GLES31.glVertexAttribPointer(positionHandle, 2, GLES31.GL_FLOAT, false, 0, verticesCoordinates)
        GLES31.glEnableVertexAttribArray(positionHandle)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4)
        GLES31.glDisableVertexAttribArray(positionHandle)
        GLES31.glDisableVertexAttribArray(texturePositionHandle)
    }

}