package com.epam.opengl.edu.ui.gl

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.opengl.*
import android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY
import android.view.MotionEvent
import android.view.View
import androidx.compose.ui.graphics.Color
import com.epam.opengl.edu.model.*
import com.epam.opengl.edu.model.geometry.*
import com.epam.opengl.edu.model.transformation.ratio
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AppGLRenderer(
    private val context: Context,
    private val view: GLSurfaceView,
    editor: Editor,
    private val onCropped: () -> Unit,
    private val onViewportSizeChange: (Size) -> Unit,
    isDebugModeEnabled: Boolean = false,
) : GLSurfaceView.Renderer, View.OnTouchListener {

    private companion object {
        const val INITIAL_TEXTURE_SIZE = 1
    }

    @Volatile
    var editor: Editor = editor
        set(value) {
            val old = field
            field = value

            val imageChanged = old.image != value.image
            val viewportChanged = old.displayTransformations.scene.windowSize != value.displayTransformations.scene.windowSize
            val transformationsChanged = value.displayTransformations != old.displayTransformations
            val cropSelectionChanged = value.displayCropSelection != old.displayCropSelection

            if (imageChanged) {
                view.queueEvent {
                    updateImage(value.image)
                }
            }

            if (imageChanged || transformationsChanged || viewportChanged || cropSelectionChanged) {
                view.requestRender()
            }
        }

    @Volatile
    var backgroundColor: Color = Color.White

    @Volatile
    var isDebugModeEnabled: Boolean = isDebugModeEnabled
        set(value) {
            val old = field
            field = value
            if (old != value) {
                view.requestRender()
            }
        }

    private val verticesBuffer = floatBufferOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )

    private val textureBuffer = floatBufferOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )

    init {
        view.setOnTouchListener(this)
    }

    private var colorTransformations = listOf(
        GrayscaleTransformation(context, verticesBuffer, textureBuffer),
        HsvTransformation(context, verticesBuffer, textureBuffer),
        ContrastTransformation(context, verticesBuffer, textureBuffer),
        TintTransformation(context, verticesBuffer, textureBuffer),
        GaussianBlurTransformation(context, verticesBuffer, textureBuffer),
    )

    private val frameBuffers = FrameBuffers(colorTransformations.size + 1)
    private val textures = Textures()
    private val projectionMatrix = FloatArray(16)
    private val vPMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private var viewTransformation = ViewTransformation(context, verticesBuffer, textureBuffer)
    private var cropTransformation = CropTransformation(context, verticesBuffer, textureBuffer)

    @Volatile
    private var cropRequested = false

    fun requestCrop() {
        if (cropRequested) {
            return
        }
        cropRequested = true
        view.requestRender()
    }

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        view.renderMode = RENDERMODE_WHEN_DIRTY
        // todo temp workaround to dispose captured gl context
        viewTransformation = ViewTransformation(context, verticesBuffer, textureBuffer)
        cropTransformation = CropTransformation(context, verticesBuffer, textureBuffer)
        colorTransformations = listOf(
            GrayscaleTransformation(context, verticesBuffer, textureBuffer),
            HsvTransformation(context, verticesBuffer, textureBuffer),
            ContrastTransformation(context, verticesBuffer, textureBuffer),
            TintTransformation(context, verticesBuffer, textureBuffer),
            GaussianBlurTransformation(context, verticesBuffer, textureBuffer),
        )

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

        updateImage(editor.image)
    }

    override fun onDrawFrame(
        gl: GL10,
    ) = with(gl) {
        GLES31.glClearColor(
            backgroundColor.red,
            backgroundColor.green,
            backgroundColor.blue,
            backgroundColor.alpha
        )
        val state = editor
        val transformations = state.displayTransformations
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

        val cropWasRequested = cropRequested
        cropRequested = false
        val readFromTexture = textures.readTextureFor(frameBuffers.size - 1)

        if (cropWasRequested) {
            cropTransformation.crop(
                transformations = transformations,
                fbo = frameBuffers.cropFrameBuffer,
                texture = readFromTexture,
                textures = textures,
                isDebugEnabled = isDebugModeEnabled,
            )
        } else if (state.displayCropSelection) {
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

            Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f)
            // Calculate the projection and view transformation
            Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
            viewTransformation.render(vPMatrix, 0, textures.readTextureFor(frameBuffers.size), imageSize, windowSize)
        }

        if (cropWasRequested) {
            onCropped()
        }
    }

    // fixme rework, also, it'll stuck forever if glThread is stopped before event is enqueued
    suspend fun bitmap(): Bitmap = suspendCoroutine { continuation ->
        view.queueEvent {
            val scene = editor.displayTransformations.scene
            GLES31.glViewport(0, 0, scene.imageSize.width, scene.imageSize.height)
            GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, frameBuffers.cropFrameBuffer)
            continuation.resume(readTextureToBitmap(scene.imageSize))
        }
    }

    override fun onSurfaceChanged(
        gl: GL10,
        width: Int,
        height: Int,
    ) {
        onViewportSizeChange(Size(width, height))
    }

    private fun updateImage(
        image: Uri,
    ) {
        val bitmap = with(context) { image.asBitmap() }
        textures.updateTextures(bitmap)
        bitmap.recycle()
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(
        v: View,
        event: MotionEvent,
    ): Boolean {
        val selectionMode = editor.displayCropSelection
        editor.displayTransformations.scene.onTouch(event, selectionMode)
        view.requestRender()
        return true
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
