package com.epam.opengl.edu.ui.gl

import android.content.Context
import android.graphics.PointF
import android.opengl.GLES31
import com.epam.opengl.edu.R
import com.epam.opengl.edu.model.Transformations
import com.epam.opengl.edu.model.height
import com.epam.opengl.edu.model.width
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL
import kotlin.math.roundToInt

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
    var cropTextures = false
    var selectionMode = false
    var pointer = PointF()
    var viewportWidth = 0f
    var viewportHeight = 0f

    private var cropOriginOffsetX = 0f
    private var cropOriginOffsetY = 0f

    context (GL)
    override fun draw(
        transformations: Transformations,
        fbo: Int,
        texture: Int,
    ) {
        val cropRegion = transformations.crop.selection

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
            // recalculates texture dimension. CroppedWidth = width * (selection.width / viewportWidth)
            val croppedTextureWidth = (textureWidth * (cropRegion.width.toFloat() / viewportWidth)).roundToInt()
            val croppedTextureHeight = (textureHeight * (cropRegion.height.toFloat() / viewportHeight)).roundToInt()

            println("texture after crop $croppedTextureWidth,$croppedTextureHeight")

            for (i in AppGLRenderer.PingTextureIdx until textures.size) {
                // resize all textures except for original one
                GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textures[i])
                GLES31.glTexImage2D(
                    GLES31.GL_TEXTURE_2D,
                    0,
                    GLES31.GL_RGBA,
                    croppedTextureWidth,
                    croppedTextureHeight,
                    0,
                    GLES31.GL_RGBA,
                    GLES31.GL_UNSIGNED_BYTE,
                    null
                )
            }

            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
            // Coordinate origin is bottom left!
            val normDeltaOffsetX = scaleX(cropRegion.topLeft.x.toFloat())
            val normDeltaOffsetY =
                -((viewportHeight - cropRegion.bottomRight.y.toFloat()) * textureHeight / viewportHeight) / viewportHeight

            GLES31.glUniform2f(
                offsetHandle,
                // plus origin offset since we're working with original texture!
                cropOriginOffsetX + normDeltaOffsetX,
                cropOriginOffsetY + normDeltaOffsetY
            )
            // each time we crop texture we remove a texture part that is located to the left from cropRegion or viewportWidth - cropRegion.bottomRight.x.
            // for x this part is going to be cropRegion.left.x, for y it'll be cropRegion.top.x or viewportHeight - cropRegion.bottomRight.y.
            // we should accumulate this cropped deltas so that we can adjust crop origin.
            // this must be done since we're working with original texture!
            cropOriginOffsetX += normDeltaOffsetX
            cropOriginOffsetY += normDeltaOffsetY
            this.textureWidth = croppedTextureWidth
            this.textureHeight = croppedTextureHeight
        } else if (selectionMode) {
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
            GLES31.glUniform1f(borderWidthHandle, transformations.crop.borderWidth.toFloat() / textureWidth.toFloat())
            GLES31.glUniform4f(
                cropRegionHandle,
                scaleX(cropRegion.topLeft.x.toFloat()),
                scaleY(cropRegion.topLeft.y.toFloat()),
                scaleX(cropRegion.bottomRight.x.toFloat()),
                scaleY(cropRegion.bottomRight.y.toFloat())
            )
        } else {
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
        }

        GLES31.glUniform2f(pointerHandle, pointer.x, pointer.y)
        GLES31.glVertexAttribPointer(positionHandle, 2, GLES31.GL_FLOAT, false, 0, verticesCoordinates)
        GLES31.glEnableVertexAttribArray(positionHandle)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4)
        GLES31.glDisableVertexAttribArray(positionHandle)
        GLES31.glDisableVertexAttribArray(texturePositionHandle)
    }

    @Deprecated("remove")
    private fun scaleX(x: Float) = x * (textureWidth / viewportWidth) / viewportWidth

    @Deprecated("remove")
    private fun scaleY(y: Float) =
        1f - textureHeight / viewportHeight + y * (textureHeight / viewportHeight) / viewportHeight

}