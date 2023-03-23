package com.epam.opengl.edu.ui.gl

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.opengl.GLES31
import android.opengl.Matrix
import androidx.compose.ui.graphics.Color
import com.epam.opengl.edu.model.Editor
import com.epam.opengl.edu.model.displayCropSelection
import com.epam.opengl.edu.model.displayTransformations
import com.epam.opengl.edu.model.geometry.height
import com.epam.opengl.edu.model.geometry.width
import com.epam.opengl.edu.model.geometry.x
import com.epam.opengl.edu.model.geometry.y
import com.epam.opengl.edu.model.transformation.Scene
import com.epam.opengl.edu.model.transformation.ratio
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL

context (GL)
internal class GlRendererDelegate(
    context: Context,
    image: Uri,
) {

    private companion object {
        const val INITIAL_TEXTURE_SIZE = 1
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

    private val frameBuffers = FrameBuffers(colorTransformations.size + 1)
    private val textures = Textures()
    private val projectionMatrix = FloatArray(16)
    private val vPMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16).also { matrix ->
        Matrix.setLookAtM(matrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f)
    }
    private val viewTransformation = ViewTransformation(context, VerticesBuffer, TextureBuffer)
    private val cropTransformation = CropTransformation(context, VerticesBuffer, TextureBuffer)

    init {
        GLES31.glGenTextures(textures.size, textures.array, 0)
        GLES31.glGenFramebuffers(frameBuffers.size, frameBuffers.array, 0)
        // setup color attachments and bind them to corresponding frame buffers
        // note that textures are shifted by one!
        // buffers binding look like the following:
        // fbo 0 (screen bound) -> texture[0] (original texture)
        // frameBuffers[0] -> textures[1]
        // frameBuffers[1] -> textures[2]
        // frameBuffers[2] -> textures[1]
        // frameBuffers[3] -> textures[2]
        // ...
        for (frameBufferIndex in 0 until frameBuffers.size) {
            setupFrameBuffer(frameBuffers[frameBufferIndex], textures.bindTextureFor(frameBufferIndex))
        }

        with(context) {
            updateImage(image)
        }
    }

    context (Context)
    fun updateImage(
        image: Uri,
    ) {
        val bitmap = image.asBitmap()
        textures.updateTextures(bitmap)
        bitmap.recycle()
    }

    fun captureScene(
        scene: Scene
    ): Bitmap {
        GLES31.glViewport(0, 0, scene.imageSize.width, scene.imageSize.height)
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, frameBuffers.cropFrameBuffer)
        return readTextureToBitmap(scene.imageSize)
    }

    context (GL)
    fun onDrawFrame(
        backgroundColor: Color,
        editor: Editor,
        cropRequested: Boolean,
        isDebugModeEnabled: Boolean
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
                fbo = frameBuffers[index],
                // fbo index -> txt index mapping: 0 -> 0; 1 -> 1; 2 -> 2; 3 -> 1; 4 -> 2...
                texture = textures.readTextureFor(index),
            )
        }

        val readFromTexture = textures.readTextureFor(frameBuffers.size - 1)

        if (cropRequested) {
            cropTransformation.crop(
                transformations = transformations,
                fbo = frameBuffers.cropFrameBuffer,
                texture = readFromTexture,
                textures = textures,
                isDebugEnabled = isDebugModeEnabled,
            )
        } else if (editor.displayCropSelection) {
            cropTransformation.drawSelection(
                transformations = transformations,
                fbo = frameBuffers.cropFrameBuffer,
                texture = readFromTexture,
                isDebugEnabled = isDebugModeEnabled,
            )
        } else {
            cropTransformation.drawNormal(
                fbo = frameBuffers.cropFrameBuffer,
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
            viewTransformation.render(vPMatrix, 0, textures.readTextureFor(frameBuffers.size), imageSize, windowSize)
        }

    }

    private fun setupFrameBuffer(
        frameBuffer: Int,
        texture: Int,
    ) {
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, frameBuffer)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
        GLES31.glTexImage2D(
            /* target = */ GLES31.GL_TEXTURE_2D,
            /* level = */ 0,
            /* internalformat = */ GLES31.GL_RGBA,
            /* width = */ INITIAL_TEXTURE_SIZE,
            /* height = */ INITIAL_TEXTURE_SIZE,
            /* border = */ 0,
            /* format = */ GLES31.GL_RGBA,
            /* type = */ GLES31.GL_UNSIGNED_BYTE,
            /* pixels = */ null
        )
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE)

        GLES31.glFramebufferTexture2D(
            GLES31.GL_FRAMEBUFFER,
            GLES31.GL_COLOR_ATTACHMENT0,
            GLES31.GL_TEXTURE_2D,
            texture,
            0
        )
        check(GLES31.glCheckFramebufferStatus(GLES31.GL_FRAMEBUFFER) == GLES31.GL_FRAMEBUFFER_COMPLETE) {
            "incomplete buffer $frameBuffer, texture $texture"
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