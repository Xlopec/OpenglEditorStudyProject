package com.epam.opengl.edu.ui.gl

import android.content.Context
import android.opengl.GLES31
import com.epam.opengl.edu.R
import com.epam.opengl.edu.model.geometry.component1
import com.epam.opengl.edu.model.geometry.component2
import com.epam.opengl.edu.model.geometry.height
import com.epam.opengl.edu.model.geometry.minus
import com.epam.opengl.edu.model.geometry.width
import com.epam.opengl.edu.model.transformation.Scene
import com.epam.opengl.edu.model.transformation.Transformations
import com.epam.opengl.edu.model.transformation.leftTopImageOffset
import com.epam.opengl.edu.model.transformation.rightBottomImageOffset
import com.epam.opengl.edu.model.transformation.toGlPoint
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
        textures: Textures,
    ) {
        val scene = transformations.scene
        render(fbo, scene) { cropRegionHandle, borderWidthHandle, _ ->
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
            GLES31.glUniform4f(cropRegionHandle, 0f, 0f, 0f, 0f)
            GLES31.glUniform1f(borderWidthHandle, 0f)
        }
        with(scene) {
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textures.originalTexture)

            val topLeft = leftTopImageOffset
            val bottomRight = rightBottomImageOffset
            val croppedSize = imageSize - topLeft - bottomRight
            val buffer = readTextureToBuffer(topLeft, croppedSize)

            textures.updateTextures(buffer, croppedSize)
        }
    }

    context (GL)
    fun drawSelection(
        transformations: Transformations,
        fbo: Int,
        texture: Int,
    ) {
        render(fbo, transformations.scene) { cropRegionHandle, borderWidthHandle, pointerHandle ->
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
    ) {
        render(fbo, transformations.scene) { cropRegionHandle, borderWidthHandle, pointerHandle ->
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

        GLES31.glVertexAttribPointer(texturePositionHandle, 2, GLES31.GL_FLOAT, false, 0, textureCoordinates)
        GLES31.glEnableVertexAttribArray(texturePositionHandle)

        renderer(scene, cropRegionHandle, borderWidthHandle, pointerHandle)

        GLES31.glVertexAttribPointer(positionHandle, 2, GLES31.GL_FLOAT, false, 0, verticesCoordinates)
        GLES31.glEnableVertexAttribArray(positionHandle)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4)
        GLES31.glDisableVertexAttribArray(positionHandle)
        GLES31.glDisableVertexAttribArray(texturePositionHandle)
    }

}