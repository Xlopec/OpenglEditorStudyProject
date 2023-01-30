package com.epam.opengl.edu.ui.gl

import android.content.Context
import android.opengl.GLES31
import com.epam.opengl.edu.R
import com.epam.opengl.edu.model.CropSelection
import com.epam.opengl.edu.model.Transformations
import com.epam.opengl.edu.model.croppedWidth
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL

class CropTransformation(
    private val context: Context,
    private val verticesCoordinates: FloatBuffer,
    private val textureCoordinates: FloatBuffer,
    private val textures: IntArray,
) : OpenglTransformation {

    context (GL)
    private val program by lazy {
        context.loadProgram(R.raw.no_transform_vertex, R.raw.frag_crop)
    }

    var textureWidth = 0
    var textureHeight = 0
    var cropSelection: CropSelection? = null

    context (GL)
    override fun draw(
        transformations: Transformations,
        fbo: Int,
        texture: Int,
    ) {
        val cropRegion = cropSelection

        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, fbo)
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        GLES31.glUseProgram(program)
        val positionHandle = GLES31.glGetAttribLocation(program, "aPosition")
        val texturePositionHandle = GLES31.glGetAttribLocation(program, "aTexPosition")
        val offsetHandle = GLES31.glGetUniformLocation(program, "offset")
        val cropRegionHandle = GLES31.glGetUniformLocation(program, "cropRegion")
        val borderWidthHandle = GLES31.glGetUniformLocation(program, "borderWidth")
        GLES31.glVertexAttribPointer(texturePositionHandle, 2, GLES31.GL_FLOAT, false, 0, textureCoordinates)
        GLES31.glEnableVertexAttribArray(texturePositionHandle)

        GLES31.glUniform1f(borderWidthHandle, 3f / textureWidth.toFloat())

        if (cropRegion == null) {

        } else {
            val croppedWidth = cropRegion.croppedWidth(textureWidth)

            /*for (i in AppGLRenderer.PingTextureIdx until textures.size) {
                // resize all textures except for original one
                GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textures[i])
                GLES31.glTexImage2D(
                    GLES31.GL_TEXTURE_2D,
                    0,
                    GLES31.GL_RGBA,
                    croppedWidth,
                    textureHeight,
                    0,
                    GLES31.GL_RGBA,
                    GLES31.GL_UNSIGNED_INT,
                    null
                )
            }

            */

            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)

            val normOffsetX = cropRegion.topLeft.x.toFloat() / textureWidth.toFloat()

            println("crop region $cropRegion")

            GLES31.glUniform2f(offsetHandle, normOffsetX, 0f)
            GLES31.glUniform2f(offsetHandle, 0f, 0f)

            GLES31.glUniform4f(
                cropRegionHandle,
                cropRegion.topLeft.x.toFloat() / textureWidth.toFloat(),
                cropRegion.topLeft.y.toFloat() / textureHeight.toFloat(),
                cropRegion.bottomRight.x.toFloat() / textureWidth.toFloat(),
                cropRegion.bottomRight.y.toFloat() / textureHeight.toFloat()
            )
        }
        GLES31.glVertexAttribPointer(positionHandle, 2, GLES31.GL_FLOAT, false, 0, verticesCoordinates)
        GLES31.glEnableVertexAttribArray(positionHandle)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4)
        GLES31.glDisableVertexAttribArray(positionHandle)
        GLES31.glDisableVertexAttribArray(texturePositionHandle)
    }

}