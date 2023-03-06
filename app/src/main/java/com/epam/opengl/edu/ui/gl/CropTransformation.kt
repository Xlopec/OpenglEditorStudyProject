package com.epam.opengl.edu.ui.gl

import android.content.Context
import android.opengl.GLES31
import com.epam.opengl.edu.R
import com.epam.opengl.edu.model.geometry.component1
import com.epam.opengl.edu.model.geometry.component2
import com.epam.opengl.edu.model.geometry.height
import com.epam.opengl.edu.model.geometry.width
import com.epam.opengl.edu.model.geometry.x
import com.epam.opengl.edu.model.geometry.y
import com.epam.opengl.edu.model.transformation.Transformations
import com.epam.opengl.edu.model.transformation.croppedTextureSize
import com.epam.opengl.edu.model.transformation.toNormalized
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class CropTransformation(
    private val context: Context,
    private val verticesCoordinates: FloatBuffer,
    private val textureCoordinates: FloatBuffer,
    private val textures: Textures,
) {

    private companion object {
        const val RectLineWidthPx = 1
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
        render(fbo) { offsetHandle, cropRegionHandle, _ ->
            val scene = transformations.scene
            val croppedTextureSize = scene.croppedTextureSize
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
                (scene.cropOriginOffset.x + scene.selection.topLeft.x * (scene.texture.width.toFloat() / scene.viewport.width)) / scene.viewport.width.toFloat(),
                // invert sign since origin for Y is bottom
                -(scene.cropOriginOffset.y + scene.selection.topLeft.y * (scene.texture.height.toFloat() / scene.viewport.height)) / scene.viewport.height
            )
            GLES31.glUniform4f(cropRegionHandle, 0f, 0f, 0f, 0f)
        }
    }

    context (GL)
    fun drawSelection(
        transformations: Transformations,
        fbo: Int,
        texture: Int,
    ) {
        render(fbo) { _, cropRegionHandle, borderWidthHandle ->
            val scene = transformations.scene
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
            GLES31.glUniform1f(borderWidthHandle, RectLineWidthPx.toFloat() / scene.viewport.width)

            with(scene) {
                val (left, top) = scene.selection.topLeft.toNormalized()
                val (right, bottom) = scene.selection.bottomRight.toNormalized()
                // Coordinate origin is bottom left!
                GLES31.glUniform4f(cropRegionHandle, left, 1f - top, right, 1f - bottom)
            }
        }
    }

    context (GL)
    fun drawNormal(
        fbo: Int,
        texture: Int,
    ) {

        render(fbo) { _, cropRegionHandle, _ ->
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
            GLES31.glUniform4f(cropRegionHandle, 0f, 0f, 0f, 0f)
        }
    }

    context (GL)
            @OptIn(ExperimentalContracts::class)
            private /*inline*/ fun render(
        fbo: Int,
        strategy: (offsetHandle: Int, cropRegionHandle: Int, borderWidthHandle: Int) -> Unit,
    ) {
        contract {
            callsInPlace(strategy, InvocationKind.EXACTLY_ONCE)
        }
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

        strategy(offsetHandle, cropRegionHandle, borderWidthHandle)

        GLES31.glVertexAttribPointer(positionHandle, 2, GLES31.GL_FLOAT, false, 0, verticesCoordinates)
        GLES31.glEnableVertexAttribArray(positionHandle)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4)
        GLES31.glDisableVertexAttribArray(positionHandle)
        GLES31.glDisableVertexAttribArray(texturePositionHandle)
    }

}