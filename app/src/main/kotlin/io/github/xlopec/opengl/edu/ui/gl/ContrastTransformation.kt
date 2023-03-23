package io.github.xlopec.opengl.edu.ui.gl

import android.content.Context
import android.opengl.GLES31
import io.github.xlopec.opengl.edu.R
import io.github.xlopec.opengl.edu.model.transformation.Transformations
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL

class ContrastTransformation(
    private val context: Context,
    private val verticesCoordinates: FloatBuffer,
    private val textureCoordinates: FloatBuffer,
) : OpenglTransformation {

    context (GL)
    private val program by lazy {
        context.loadProgram(R.raw.no_transform_vertex, R.raw.frag_contrast)
    }

    context (GL)
    override fun draw(
        transformations: Transformations,
        frameBuffer: FrameBuffer,
        sourceTexture: Texture,
    ) {
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, frameBuffer.value)
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        GLES31.glUseProgram(program)
        val positionHandle = GLES31.glGetAttribLocation(program, "aPosition")
        val texturePositionHandle = GLES31.glGetAttribLocation(program, "aTexPosition")
        val contrastHandle = GLES31.glGetUniformLocation(program, "contrast")
        GLES31.glVertexAttribPointer(texturePositionHandle, 2, GLES31.GL_FLOAT, false, 0, textureCoordinates)
        GLES31.glEnableVertexAttribArray(texturePositionHandle)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, sourceTexture.value)
        GLES31.glUniform1f(contrastHandle, transformations.contrast.delta)
        GLES31.glVertexAttribPointer(positionHandle, 2, GLES31.GL_FLOAT, false, 0, verticesCoordinates)
        GLES31.glEnableVertexAttribArray(positionHandle)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4)
        GLES31.glDisableVertexAttribArray(positionHandle)
        GLES31.glDisableVertexAttribArray(texturePositionHandle)
    }

}