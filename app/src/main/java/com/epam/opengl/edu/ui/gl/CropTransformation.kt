package com.epam.opengl.edu.ui.gl

import android.content.Context
import android.opengl.GLES31
import com.epam.opengl.edu.R
import com.epam.opengl.edu.model.OnTouchHelperUpdated
import com.epam.opengl.edu.model.Transformations
import com.epam.opengl.edu.ui.MessageHandler
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL

class CropTransformation(
    private val context: Context,
    private val verticesCoordinates: FloatBuffer,
    private val textureCoordinates: FloatBuffer,
    private val textures: Textures,
    private val handler: MessageHandler,
) {

    context (GL)
            private val program by lazy {
        context.loadProgram(R.raw.no_transform_vertex, R.raw.frag_crop)
    }

    var cropTextures = false
    var selectionMode = false

    context (GL)
    fun draw(
        transformations: Transformations,
        fbo: Int,
        texture: Int,
        touchHelper: TouchHelper,
    ) {
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, fbo)
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        GLES31.glUseProgram(program)
        val positionHandle = GLES31.glGetAttribLocation(program, "aPosition")
        val pointerHandle = GLES31.glGetUniformLocation(program, "pointer")
        val texturePositionHandle = GLES31.glGetAttribLocation(program, "aTexPosition")
        val offsetHandle = GLES31.glGetUniformLocation(program, "offset")
        val cropRegionHandle = GLES31.glGetUniformLocation(program, "cropRegion")
        val borderWidthHandle = GLES31.glGetUniformLocation(program, "borderWidth")
        GLES31.glVertexAttribPointer(texturePositionHandle, 2, GLES31.GL_FLOAT, false, 0, textureCoordinates)
        GLES31.glEnableVertexAttribArray(texturePositionHandle)

        if (cropTextures) {
            cropTextures = false
            val croppedTextureSize = touchHelper.croppedTextureSize
            // resize texture bound to this framebuffer
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textures.cropTexture)
            GLES31.glTexImage2D(
                GLES31.GL_TEXTURE_2D,
                0,
                GLES31.GL_RGBA,
                croppedTextureSize.width,
                croppedTextureSize.height,
                0,
                GLES31.GL_RGBA,
                GLES31.GL_UNSIGNED_BYTE,
                null
            )

            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
            GLES31.glUniform2f(
                offsetHandle,
                // plus origin offset since we're working with original texture!
                (touchHelper.cropOriginOffset.x + touchHelper.selection.topLeft.x * (touchHelper.texture.width.toFloat() / touchHelper.viewport.width)) / touchHelper.viewport.width.toFloat(),
                // invert sign since origin for Y is bottom
                -(touchHelper.cropOriginOffset.y + touchHelper.selection.topLeft.y * (touchHelper.texture.height.toFloat() / touchHelper.viewport.height)) / touchHelper.viewport.height
            )
            handler(OnTouchHelperUpdated(touchHelper.onCropped()))
        } else if (selectionMode) {
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
            GLES31.glUniform1f(borderWidthHandle, transformations.crop.borderWidth.toFloat() / touchHelper.viewport.width)
            GLES31.glUniform4f(
                cropRegionHandle,
                touchHelper.normalizedX(touchHelper.selection.topLeft.x),
                // Coordinate origin is bottom left!
                1f - touchHelper.normalizedY(touchHelper.selection.topLeft.y),
                touchHelper.normalizedX(touchHelper.selection.bottomRight.x),
                // Coordinate origin is bottom left!
                1f - touchHelper.normalizedY(touchHelper.selection.bottomRight.y)
            )
        } else {
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
        }

        GLES31.glUniform2f(
            pointerHandle,
            touchHelper.normalizedX(touchHelper.userInput.x),
            // Coordinate origin is bottom left!
            1f - touchHelper.normalizedY(touchHelper.userInput.y)
        )
        GLES31.glVertexAttribPointer(positionHandle, 2, GLES31.GL_FLOAT, false, 0, verticesCoordinates)
        GLES31.glEnableVertexAttribArray(positionHandle)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4)
        GLES31.glDisableVertexAttribArray(positionHandle)
        GLES31.glDisableVertexAttribArray(texturePositionHandle)
    }

}