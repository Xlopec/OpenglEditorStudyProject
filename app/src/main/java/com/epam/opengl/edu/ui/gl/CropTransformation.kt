package com.epam.opengl.edu.ui.gl

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES31
import android.opengl.GLUtils
import com.epam.opengl.edu.R
import com.epam.opengl.edu.model.geometry.component1
import com.epam.opengl.edu.model.geometry.component2
import com.epam.opengl.edu.model.geometry.height
import com.epam.opengl.edu.model.geometry.width
import com.epam.opengl.edu.model.geometry.x
import com.epam.opengl.edu.model.geometry.y
import com.epam.opengl.edu.model.transformation.Transformations
import com.epam.opengl.edu.model.transformation.toNormalized
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.roundToInt

class CropTransformation(
    private val context: Context,
    private val verticesCoordinates: FloatBuffer,
    private val textureCoordinates: FloatBuffer,
    private val textures: Textures,
) {

    private companion object {
        const val RectLineWidthPx = 2
    }

    context (GL)
            private val program by lazy {
        context.loadProgram(R.raw.no_transform_vertex, R.raw.frag_crop)
    }

    context (GL)
    fun crop(
        transformations: Transformations,
        fbo: Int,
        texture: Int,
    ) {
        val scene = transformations.scene
        render(fbo) { cropRegionHandle, _, _ ->

            // resize texture bound to this framebuffer
            /*GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textures.cropTexture)
            GLES31.glTexImage2D(
                GLES31.GL_TEXTURE_2D,
                0,
                GLES31.GL_RGBA,
                (window.width),// * (croppedTextureSize.width.toFloat() / scene.viewport.width.toFloat())).roundToInt(),
                window.height,
                0,
                GLES31.GL_RGBA,
                GLES31.GL_UNSIGNED_BYTE,
                null
            )*/

            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
            GLES31.glUniform4f(cropRegionHandle, 0f, 0f, 0f, 0f)
        }
        val rawOffsetLeft = scene.selection.topLeft.x
        val rawOffsetRight = scene.image.width - scene.selection.bottomRight.x

        val w2vRatio = scene.window.width / scene.image.width.toFloat()
        val h2vRatio = scene.window.height / scene.image.height.toFloat()

        val offsetLeftX = (rawOffsetLeft * w2vRatio).roundToInt()
        val offsetRightX = (rawOffsetRight * w2vRatio).roundToInt()
        val newWindowWidth = scene.window.width - offsetLeftX - offsetRightX

        val offsetTopY = (scene.selection.topLeft.y * h2vRatio).roundToInt()
        val offsetBottomY = ((scene.image.height - scene.selection.bottomRight.y) * h2vRatio).roundToInt()

        val newWindowHeight = scene.window.height - offsetTopY - offsetBottomY

        val buffer = ByteBuffer.allocateDirect(newWindowWidth * newWindowHeight * 4)
            .order(ByteOrder.nativeOrder())
            .position(0)

        GLES31.glReadPixels(
            offsetLeftX,
            offsetTopY,
            newWindowWidth,
            newWindowHeight,
            GLES31.GL_RGBA,
            GLES31.GL_UNSIGNED_BYTE,
            buffer
        )

        val bitmap = Bitmap.createBitmap(newWindowWidth, newWindowHeight, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)

        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textures.originalTexture)

        GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bitmap, 0)
        /*GLES31.glTexImage2D(
            GLES31.GL_TEXTURE_2D,
            0,
            GLES31.GL_RGBA,
            croppedTextureSize.width,
            croppedTextureSize.height,
            0,
            GLES31.GL_RGBA,
            GLES31.GL_UNSIGNED_BYTE,
            buffer
        )*/

        bitmap.recycle()
    }

    context (GL)
    fun drawSelection(
        transformations: Transformations,
        fbo: Int,
        texture: Int,
    ) {
        render(fbo) { cropRegionHandle, borderWidthHandle, pointerHandle ->
            val scene = transformations.scene
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
            GLES31.glUniform1f(borderWidthHandle, RectLineWidthPx.toFloat() / scene.image.width)

            with(scene) {
                val (left, top) = scene.selection.topLeft.toNormalized()
                val (right, bottom) = scene.selection.bottomRight.toNormalized()
                val pointer = scene.userInput.toNormalized()
                // Coordinate origin is bottom left!
                GLES31.glUniform4f(cropRegionHandle, left, 1f - top, right, 1f - bottom)
                GLES31.glUniform2f(pointerHandle, pointer.x, 1f - pointer.y)
            }
        }
    }

    context (GL)
    fun drawNormal(
        fbo: Int,
        texture: Int,
        transformations: Transformations,
    ) {

        render(fbo) { cropRegionHandle, borderWidthHandle, pointerHandle ->
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
            val pointer = with(transformations.scene) { userInput.toNormalized() }
            GLES31.glUniform2f(pointerHandle, pointer.x, 1f - pointer.y)
            GLES31.glUniform4f(cropRegionHandle, 0f, 0f, 0f, 0f)
            GLES31.glUniform1f(borderWidthHandle, RectLineWidthPx.toFloat() / transformations.scene.image.width)
        }
    }

    context (GL)
            @OptIn(ExperimentalContracts::class)
            private /*inline*/ fun render(
        fbo: Int,
        strategy: (cropRegionHandle: Int, borderWidthHandle: Int, pointerHandle: Int) -> Unit,
    ) {
        contract {
            callsInPlace(strategy, InvocationKind.EXACTLY_ONCE)
        }
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, fbo)
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        GLES31.glUseProgram(program)

        val positionHandle = GLES31.glGetAttribLocation(program, "aPosition")
        val texturePositionHandle = GLES31.glGetAttribLocation(program, "aTexPosition")
        val pointerHandle = GLES31.glGetUniformLocation(program, "pointer")
        val cropRegionHandle = GLES31.glGetUniformLocation(program, "cropRegion")
        val borderWidthHandle = GLES31.glGetUniformLocation(program, "borderWidth")

        GLES31.glVertexAttribPointer(texturePositionHandle, 2, GLES31.GL_FLOAT, false, 0, textureCoordinates)
        GLES31.glEnableVertexAttribArray(texturePositionHandle)

        strategy(cropRegionHandle, borderWidthHandle, pointerHandle)

        GLES31.glVertexAttribPointer(positionHandle, 2, GLES31.GL_FLOAT, false, 0, verticesCoordinates)
        GLES31.glEnableVertexAttribArray(positionHandle)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4)
        GLES31.glDisableVertexAttribArray(positionHandle)
        GLES31.glDisableVertexAttribArray(texturePositionHandle)
    }

}