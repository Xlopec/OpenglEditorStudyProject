package io.github.xlopec.opengl.edu.ui.gl

import android.content.Context
import android.opengl.GLES31
import io.github.xlopec.opengl.edu.R
import io.github.xlopec.opengl.edu.model.geometry.*
import io.github.xlopec.opengl.edu.model.transformation.*
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
    fun clearMarks(
        transformations: Transformations,
        frameBuffer: FrameBuffer,
        sourceTexture: Texture,
        isDebugEnabled: Boolean,
    ) {
        val scene = transformations.scene
        render(frameBuffer, scene, isDebugEnabled) { cropRegionHandle, borderWidthHandle, _ ->
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, sourceTexture.value)
            GLES31.glUniform4f(cropRegionHandle, 0f, 0f, 0f, 0f)
            GLES31.glUniform1f(borderWidthHandle, 0f)
        }
    }

    context (GL)
    fun drawSelection(
        transformations: Transformations,
        frameBuffer: FrameBuffer,
        texture: Texture,
        isDebugEnabled: Boolean,
    ) {
        render(
            frameBuffer,
            transformations.scene,
            isDebugEnabled
        ) { cropRegionHandle, borderWidthHandle, pointerHandle ->
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture.value)
            GLES31.glUniform1f(
                borderWidthHandle,
                RectLineWidthPx.toFloat() / maxOf(windowSize.width, windowSize.height)
            )
            val (x, y) = imagePoint.toGlPoint()
            GLES31.glUniform2f(pointerHandle, x, y)

            val (left, top) = selection.topLeft.toGlPoint()
            val (right, bottom) = selection.bottomRight.toGlPoint()
            GLES31.glUniform4f(cropRegionHandle, left, top, right, bottom)
        }
    }

    context (GL)
    fun drawNormal(
        frameBuffer: FrameBuffer,
        texture: Texture,
        transformations: Transformations,
        isDebugEnabled: Boolean,
    ) {
        render(
            frameBuffer,
            transformations.scene,
            isDebugEnabled
        ) { cropRegionHandle, borderWidthHandle, pointerHandle ->
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture.value)
            val (x, y) = imagePoint.toGlPoint()
            GLES31.glUniform2f(pointerHandle, x, y)
            GLES31.glUniform4f(cropRegionHandle, 0f, 0f, 0f, 0f)
            GLES31.glUniform1f(
                borderWidthHandle,
                RectLineWidthPx.toFloat() / maxOf(windowSize.width, windowSize.height)
            )
        }
    }

    context (GL)
            @OptIn(ExperimentalContracts::class)
            private /*inline*/ fun render(
        frameBuffer: FrameBuffer,
        scene: Scene,
        isDebugEnabled: Boolean,
        renderer: Scene.(cropRegionHandle: Int, borderWidthHandle: Int, pointerHandle: Int) -> Unit,
    ) {
        contract {
            callsInPlace(renderer, InvocationKind.EXACTLY_ONCE)
        }
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, frameBuffer.value)
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