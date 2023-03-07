package com.epam.opengl.edu.ui.gl

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES31
import android.opengl.GLUtils
import com.epam.opengl.edu.R
import com.epam.opengl.edu.model.geometry.Size
import com.epam.opengl.edu.model.geometry.component1
import com.epam.opengl.edu.model.geometry.component2
import com.epam.opengl.edu.model.geometry.height
import com.epam.opengl.edu.model.geometry.width
import com.epam.opengl.edu.model.geometry.x
import com.epam.opengl.edu.model.geometry.y
import com.epam.opengl.edu.model.transformation.Transformations
import com.epam.opengl.edu.model.transformation.toNormalized
import com.epam.opengl.edu.model.transformation.window
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
        println("Do crop")
        val scene = transformations.scene
        val croppedTextureSize =
            Size(scene.viewport.width, scene.viewport.height)// scene.croppedTextureSizeInViewportPerspective
        render(fbo) { offsetHandle, cropRegionHandle, _, _ ->

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
            GLES31.glUniform2f(
                offsetHandle,
                // plus origin offset since we're working with original texture!
                0f,//(scene.cropOriginOffset.x + scene.selection.topLeft.x * (scene.texture.width.toFloat() / scene.viewport.width)) / scene.viewport.width.toFloat(),
                // invert sign since origin for Y is bottom
                0f// -(scene.cropOriginOffset.y + scene.selection.topLeft.y * (scene.texture.height.toFloat() / scene.viewport.height)) / scene.viewport.height
            )
        }
        //


        val rawOffset = 307
        val offsetLeftX = (rawOffset * window.width / scene.viewport.width.toFloat()).roundToInt()

        val buffer = ByteBuffer.allocateDirect((window.width - offsetLeftX) * window.height * 4)
            .order(ByteOrder.nativeOrder()).position(0)


        GLES31.glReadPixels(
            offsetLeftX,
            0,
            window.width - offsetLeftX,
            window.height,
            GLES31.GL_RGBA,
            GLES31.GL_UNSIGNED_BYTE,
            buffer
        )
        val bitmap = Bitmap.createBitmap(window.width - offsetLeftX, window.height, Bitmap.Config.ARGB_8888)
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

        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textures.cropTexture)
        GLES31.glTexImage2D(
            GLES31.GL_TEXTURE_2D,
            0,
            GLES31.GL_RGBA,
            window.width,
            window.height,
            0,
            GLES31.GL_RGBA,
            GLES31.GL_UNSIGNED_BYTE,
            null
        )

        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
        val offsetHandle = GLES31.glGetUniformLocation(program, "offset")

        GLES31.glUniform2f(
            offsetHandle,
            // plus origin offset since we're working with original texture!
            0f,
            // invert sign since origin for Y is bottom
            0f
        )

        bitmap.recycle()
    }

    context (GL)
    fun drawSelection(
        transformations: Transformations,
        fbo: Int,
        texture: Int,
    ) {
        render(fbo) { _, cropRegionHandle, borderWidthHandle, pointerHandle ->
            val scene = transformations.scene
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
            GLES31.glUniform1f(borderWidthHandle, RectLineWidthPx.toFloat() / scene.viewport.width)

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

        render(fbo) { _, cropRegionHandle, borderWidthHandle, pointerHandle ->
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
            val pointer = with(transformations.scene) { userInput.toNormalized() }
            GLES31.glUniform2f(pointerHandle, pointer.x, 1f - pointer.y)
            GLES31.glUniform4f(cropRegionHandle, 0f, 0f, 0f, 0f)
            GLES31.glUniform1f(borderWidthHandle, RectLineWidthPx.toFloat() / transformations.scene.viewport.width)
        }
    }

    context (GL)
            @OptIn(ExperimentalContracts::class)
            private /*inline*/ fun render(
        fbo: Int,
        strategy: (offsetHandle: Int, cropRegionHandle: Int, borderWidthHandle: Int, pointerHandle: Int) -> Unit,
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
        val offsetHandle = GLES31.glGetUniformLocation(program, "offset")
        val cropRegionHandle = GLES31.glGetUniformLocation(program, "cropRegion")
        val borderWidthHandle = GLES31.glGetUniformLocation(program, "borderWidth")

        GLES31.glVertexAttribPointer(texturePositionHandle, 2, GLES31.GL_FLOAT, false, 0, textureCoordinates)
        GLES31.glEnableVertexAttribArray(texturePositionHandle)

        strategy(offsetHandle, cropRegionHandle, borderWidthHandle, pointerHandle)

        GLES31.glVertexAttribPointer(positionHandle, 2, GLES31.GL_FLOAT, false, 0, verticesCoordinates)
        GLES31.glEnableVertexAttribArray(positionHandle)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4)
        GLES31.glDisableVertexAttribArray(positionHandle)
        GLES31.glDisableVertexAttribArray(texturePositionHandle)
    }

}