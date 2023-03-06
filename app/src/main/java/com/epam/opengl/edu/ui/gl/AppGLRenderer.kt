package com.epam.opengl.edu.ui.gl

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.opengl.*
import android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY
import android.view.MotionEvent
import android.view.View
import com.epam.opengl.edu.model.*
import com.epam.opengl.edu.model.geometry.Size
import com.epam.opengl.edu.model.geometry.height
import com.epam.opengl.edu.model.geometry.size
import com.epam.opengl.edu.model.geometry.width
import com.epam.opengl.edu.model.transformation.ratio
import com.epam.opengl.edu.model.transformation.textureOffsetXPoints
import com.epam.opengl.edu.model.transformation.textureOffsetYPoints
import com.epam.opengl.edu.ui.gl.Textures.Companion.PingTextureIdx
import com.epam.opengl.edu.ui.gl.Textures.Companion.PongTextureIdx
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
    private val onCropped: () -> Unit,
    private val onViewportChange: (Size) -> Unit,
) : GLSurfaceView.Renderer, View.OnTouchListener {

    @Volatile
    var state: EditMenu? = null
        set(value) {
            val old = field
            field = value

            val imageChanged = value != null && old?.image != value.image
            val viewportChanged =
                value != null && value.displayTransformations.scene.viewport != old?.displayTransformations?.scene?.viewport
            val transformationsChanged = value?.displayTransformations != old?.displayTransformations
            val cropSelectionChanged = value?.displayCropSelection != old?.displayCropSelection

            if (viewportChanged) {
                view.queueEvent {
                    bindBuffers(value!!.displayTransformations.scene.viewport, value.image)
                }
            } else if (imageChanged) {
                view.queueEvent {
                    bindImage(value!!.image)
                }
            }

            if (imageChanged || transformationsChanged || viewportChanged || cropSelectionChanged) {
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

    private val textures = Textures()
    private val colorTransformations = listOf(
        GrayscaleTransformation(context, verticesBuffer, textureBuffer),
        HsvTransformation(context, verticesBuffer, textureBuffer),
        ContrastTransformation(context, verticesBuffer, textureBuffer),
        TintTransformation(context, verticesBuffer, textureBuffer),
        GaussianBlurTransformation(context, verticesBuffer, textureBuffer),
    )

    // 1 extra buffer for cropping
    private val frameBuffers = FrameBuffers(colorTransformations.size + 1)
    private val projectionMatrix = FloatArray(16)
    private val vPMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val viewTransformation = ViewTransformation(context, verticesBuffer, textureBuffer)
    private val cropTransformation = CropTransformation(context, verticesBuffer, textureBuffer, textures)
    @Volatile
    private var cropRequested = false

    fun requestCrop() {
        require(!cropRequested) { "Requesting crop during cropping might lead to state inconsistency bugs" }
        cropRequested = true
        view.requestRender()
    }

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        view.renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onDrawFrame(
        gl: GL10,
    ) = with(gl) {
        val state = state ?: return@with
        val transformations = state.displayTransformations
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
                texture = textures[index.takeIf { it == 0 } ?: (1 + ((1 + index) % (textures.size - 2)))],
            )
        }

        val cropWasRequested = cropRequested
        cropRequested = false
        val readFromTexture = textures[(frameBuffers.size - 1) % (textures.size - 2)]

        if (cropWasRequested) {
            cropTransformation.crop(
                transformations = transformations,
                fbo = frameBuffers.cropFrameBuffer,
                texture = readFromTexture,
            )
        } else if (state.displayCropSelection) {
            cropTransformation.drawSelection(
                transformations = transformations,
                fbo = frameBuffers.cropFrameBuffer,
                texture = readFromTexture,
            )
        } else {
            cropTransformation.drawNormal(
                fbo = frameBuffers.cropFrameBuffer,
                texture = readFromTexture
            )
        }

        val ratio = transformations.scene.ratio
        val zoom = transformations.scene.zoom
        val consumedOffsetX = transformations.scene.textureOffsetXPoints
        val consumedOffsetY = transformations.scene.textureOffsetYPoints

        Matrix.frustumM(
            /* m = */ projectionMatrix,
            /* offset = */ 0,
            /* left = */ -ratio / zoom + consumedOffsetX,
            /* right = */ ratio / zoom + consumedOffsetX,
            /* bottom = */ -1f / zoom - consumedOffsetY,
            /* top = */ 1f / zoom - consumedOffsetY,
            /* near = */ 3f,
            /* far = */ 7f
        )

        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f)
        // Calculate the projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        viewTransformation.render(vPMatrix, 0, textures.cropTexture)

        if (cropWasRequested) {
            onCropped()
        }
    }

    // fixme rework, also, it'll stuck forever if glThread is stopped before event is enqueued
    suspend fun bitmap(): Bitmap = suspendCoroutine { continuation ->
        view.queueEvent {
            val state = requireNotNull(state) { "can't export bitmap before state is initialized" }
            GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, frameBuffers[0])
            continuation.resume(
                saveTextureToBitmap(
                    state.displayTransformations.scene.selection.size.width,
                    state.displayTransformations.scene.selection.size.height
                )
            )
        }
    }

    override fun onSurfaceChanged(
        gl: GL10,
        width: Int,
        height: Int,
    ) {
        onViewportChange(Size(width, height))
    }

    private fun bindImage(
        image: Uri,
    ) {
        val bitmap = with(context) { image.asBitmap() }
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textures.originalTexture)
        GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()
    }

    private fun bindBuffers(
        viewport: Size,
        image: Uri,
    ) {
        GLES31.glViewport(0, 0, viewport.width, viewport.height)
        GLES31.glClearColor(1.0f, 1.0f, 1.0f, 1.0f)
        GLES31.glEnable(GLES31.GL_BLEND)
        GLES31.glBlendFunc(GLES31.GL_SRC_ALPHA, GLES31.GL_ONE_MINUS_SRC_ALPHA)

        val bitmap = with(context) { image.asBitmap() }

        GLES31.glGenTextures(textures.size, textures.array, 0)
        GLES31.glGenFramebuffers(frameBuffers.size, frameBuffers.array, 0)

        // bind and load original texture
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textures.originalTexture)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bitmap, 0)

        bitmap.recycle()
        // setup color attachments and bind them to corresponding frame buffers
        // note that textures are shifted by one!
        // buffers binding look like the following:
        // fbo 0 (screen bound) -> texture[0] (original texture)
        // frameBuffers[0] -> textures[1]
        // frameBuffers[1] -> textures[2]
        // frameBuffers[2] -> textures[1]
        // frameBuffers[3] -> textures[2]
        // ...
        for (frameBufferIndex in 0 until frameBuffers.size - 1) {
            val textureIndex = 1 + (frameBufferIndex % (textures.size - 2))
            require(textureIndex == PingTextureIdx || textureIndex == PongTextureIdx) {
                "incorrect texture index, was $textureIndex"
            }
            setupFrameBuffer(viewport.width, viewport.height, textures[textureIndex], frameBuffers[frameBufferIndex])
        }

        setupFrameBuffer(viewport.width, viewport.height, textures.cropTexture, frameBuffers.cropFrameBuffer)
    }

    private fun setupFrameBuffer(
        width: Int,
        height: Int,
        texture: Int,
        frameBuffer: Int,
    ) {
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, frameBuffer)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
        GLES31.glTexImage2D(
            GLES31.GL_TEXTURE_2D,
            0,
            GLES31.GL_RGBA,
            width,
            height,
            0,
            GLES31.GL_RGBA,
            GLES31.GL_UNSIGNED_BYTE,
            null
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
        val state = state ?: return true
        val selectionMode = state.displayCropSelection
        state.displayTransformations.scene.onTouch(event, selectionMode)
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

fun saveTextureToBitmap(w: Int, h: Int): Bitmap {
    val buffer = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder()).position(0)

    GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(buffer)

    return bitmap
}