package io.github.xlopec.opengl.edu.ui.gl

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES31
import android.opengl.Matrix
import androidx.compose.ui.graphics.Color
import io.github.xlopec.opengl.edu.model.Editor
import io.github.xlopec.opengl.edu.model.displayCropSelection
import io.github.xlopec.opengl.edu.model.displayTransformations
import io.github.xlopec.opengl.edu.model.geometry.height
import io.github.xlopec.opengl.edu.model.geometry.width
import io.github.xlopec.opengl.edu.model.geometry.x
import io.github.xlopec.opengl.edu.model.geometry.y
import io.github.xlopec.opengl.edu.model.transformation.Scene
import io.github.xlopec.opengl.edu.model.transformation.ratio
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL

context (GL)
internal class GlRendererDelegate(
    context: Context,
    bitmap: Bitmap,
) {

    private companion object {
        val VerticesBuffer = floatBufferOf(
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f
        )
        val TextureBuffer = floatBufferOf(
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
        )
    }

    private val colorTransformations = listOf(
        GrayscaleTransformation(context, VerticesBuffer, TextureBuffer),
        HsvTransformation(context, VerticesBuffer, TextureBuffer),
        ContrastTransformation(context, VerticesBuffer, TextureBuffer),
        TintTransformation(context, VerticesBuffer, TextureBuffer),
        GaussianBlurTransformation(context, VerticesBuffer, TextureBuffer),
    )

    private val projectionMatrix = FloatArray(16)
    private val vPMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16).also { matrix ->
        Matrix.setLookAtM(matrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f)
    }
    private val viewTransformation = ViewTransformation(context, VerticesBuffer, TextureBuffer)
    private val cropTransformation = CropTransformation(context, VerticesBuffer, TextureBuffer)
    private val offscreenRenderer = OffscreenRenderer(colorTransformations.size + 1, bitmap)

    fun updateImage(
        bitmap: Bitmap,
    ) = offscreenRenderer.updateTextures(bitmap)

    fun captureScene(
        scene: Scene,
    ): Bitmap {
        GLES31.glViewport(0, 0, scene.imageSize.width, scene.imageSize.height)
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, offscreenRenderer.cropFrameBuffer)
        return readTextureToBitmap(scene.imageSize)
    }

    fun onDrawFrame(
        backgroundColor: Color,
        editor: Editor,
        cropRequested: Boolean,
        isDebugModeEnabled: Boolean,
    ) {
        GLES31.glClearColor(
            backgroundColor.red,
            backgroundColor.green,
            backgroundColor.blue,
            backgroundColor.alpha
        )
        val transformations = editor.displayTransformations
        val scene = transformations.scene

        GLES31.glViewport(0, 0, scene.imageSize.width, scene.imageSize.height)
        // we're using previous texture as target for next transformations, render pipeline looks like the following
        // original texture -> grayscale transformation -> texture[1];
        // texture[1] -> hsv transformation -> texture[2];
        // ....
        // texture[last modified texture + 1] -> matrix transformation -> screen
        colorTransformations.fastForEachIndexed { index, transformation ->
            transformation.draw(
                transformations = transformations,
                fbo = offscreenRenderer.fbo(index),
                // fbo index -> txt index mapping: 0 -> 0; 1 -> 1; 2 -> 2; 3 -> 1; 4 -> 2...
                texture = offscreenRenderer.sourceTextureForFbo(index),
            )
        }

        val readFromTexture = offscreenRenderer.sourceTextureForFbo(offscreenRenderer.fboCount - 1)

        if (cropRequested) {
            cropTransformation.crop(
                transformations = transformations,
                fbo = offscreenRenderer.cropFrameBuffer,
                texture = readFromTexture,
                offscreenRenderer = offscreenRenderer,
                isDebugEnabled = isDebugModeEnabled,
            )
        } else if (editor.displayCropSelection) {
            cropTransformation.drawSelection(
                transformations = transformations,
                fbo = offscreenRenderer.cropFrameBuffer,
                texture = readFromTexture,
                isDebugEnabled = isDebugModeEnabled,
            )
        } else {
            cropTransformation.drawNormal(
                fbo = offscreenRenderer.cropFrameBuffer,
                texture = readFromTexture,
                transformations = transformations,
                isDebugEnabled = isDebugModeEnabled,
            )
        }

        with(scene) {
            val frustumOffsetX = 2 * windowSize.ratio * sceneOffset.x / windowSize.width
            val frustumOffsetY = 2 * sceneOffset.y / windowSize.height

            Matrix.frustumM(
                /* m = */ projectionMatrix,
                /* offset = */ 0,
                /* left = */ -windowSize.ratio - frustumOffsetX,
                /* right = */ windowSize.ratio - frustumOffsetX,
                /* bottom = */ -1f + frustumOffsetY,
                /* top = */ 1f + frustumOffsetY,
                /* near = */ 3f,
                /* far = */ 7f
            )

            // Calculate the projection and view transformation
            Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
            viewTransformation.render(
                vPMatrix,
                0,
                offscreenRenderer.sourceTextureForFbo(offscreenRenderer.fboCount),
                imageSize,
                windowSize
            )
        }

    }

}

private fun floatBufferOf(
    vararg data: Float,
): FloatBuffer {
    val buff: ByteBuffer = ByteBuffer.allocateDirect(data.size * Float.SIZE_BYTES)

    buff.order(ByteOrder.nativeOrder())
    val floatBuffer = buff.asFloatBuffer()
    floatBuffer.put(data)
    floatBuffer.position(0)
    return floatBuffer
}