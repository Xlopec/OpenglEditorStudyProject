package com.epam.opengl.edu.ui.gl

import android.content.Context
import android.opengl.GLES31
import com.epam.opengl.edu.R
import com.epam.opengl.edu.model.geometry.*
import com.epam.opengl.edu.model.transformation.*
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class CropTransformation(
    private val context: Context,
    private val verticesCoordinates: FloatBuffer,
    private val textureCoordinates: FloatBuffer,
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
        offscreenRenderer: OffscreenRenderer,
        isDebugEnabled: Boolean,
    ) {
        val scene = transformations.scene
        render(fbo, scene, isDebugEnabled) { cropRegionHandle, borderWidthHandle, _ ->
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
            GLES31.glUniform4f(cropRegionHandle, 0f, 0f, 0f, 0f)
            GLES31.glUniform1f(borderWidthHandle, 0f)
        }
        with(scene) {
            val topLeft = leftTopImageOffset
            val bottomRight = rightBottomImageOffset
            val croppedSize = imageSize - topLeft - bottomRight
            offscreenRenderer.resizeTextures(topLeft, croppedSize)
        }
    }

    context (GL)
    fun drawSelection(
        transformations: Transformations,
        fbo: Int,
        texture: Int,
        isDebugEnabled: Boolean,
    ) {
        render(fbo, transformations.scene, isDebugEnabled) { cropRegionHandle, borderWidthHandle, pointerHandle ->
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
            GLES31.glUniform1f(borderWidthHandle, RectLineWidthPx.toFloat() / maxOf(windowSize.width, windowSize.height))
            val (x, y) = imagePoint.toGlPoint()
            GLES31.glUniform2f(pointerHandle, x, y)

            val (left, top) = selection.topLeft.toGlPoint()
            val (right, bottom) = selection.bottomRight.toGlPoint()
            GLES31.glUniform4f(cropRegionHandle, left, top, right, bottom)
        }
    }

    context (GL)
    fun drawNormal(
        fbo: Int,
        texture: Int,
        transformations: Transformations,
        isDebugEnabled: Boolean,
    ) {
        render(fbo, transformations.scene, isDebugEnabled) { cropRegionHandle, borderWidthHandle, pointerHandle ->
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
            val (x, y) = imagePoint.toGlPoint()
            GLES31.glUniform2f(pointerHandle, x, y)
            GLES31.glUniform4f(cropRegionHandle, 0f, 0f, 0f, 0f)
            GLES31.glUniform1f(borderWidthHandle, RectLineWidthPx.toFloat() / maxOf(windowSize.width, windowSize.height))
        }
    }

    context (GL)
            @OptIn(ExperimentalContracts::class)
            private /*inline*/ fun render(
        fbo: Int,
        scene: Scene,
        isDebugEnabled: Boolean,
        renderer: Scene.(cropRegionHandle: Int, borderWidthHandle: Int, pointerHandle: Int) -> Unit,
    ) {
        contract {
            callsInPlace(renderer, InvocationKind.EXACTLY_ONCE)
        }
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, fbo)
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        GLES31.glUseProgram(program)

        val positionHandle = GLES31.glGetAttribLocation(program, "aPosition")
        val texturePositionHandle = GLES31.glGetAttribLocation(program, "aTexPosition")
        val pointerHandle = GLES31.glGetUniformLocation(program, "pointer")
        val cropRegionHandle = GLES31.glGetUniformLocation(program, "cropRegion")
        val borderWidthHandle = GLES31.glGetUniformLocation(program, "borderWidth")
        val debugHandle = GLES31.glGetUniformLocation(program, "debugEnabled")

        GLES31.glVertexAttribPointer(texturePositionHandle, 2, GLES31.GL_FLOAT, false, 0, textureCoordinates)
        GLES31.glEnableVertexAttribArray(texturePositionHandle)
        GLES31.glUniform1i(debugHandle, if (isDebugEnabled) 1 else 0)

        renderer(scene, cropRegionHandle, borderWidthHandle, pointerHandle)

        GLES31.glVertexAttribPointer(positionHandle, 2, GLES31.GL_FLOAT, false, 0, verticesCoordinates)
        GLES31.glEnableVertexAttribArray(positionHandle)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4)
        GLES31.glDisableVertexAttribArray(positionHandle)
        GLES31.glDisableVertexAttribArray(texturePositionHandle)
    }

}