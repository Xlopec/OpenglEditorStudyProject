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
import com.epam.opengl.edu.ui.MessageHandler
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt

class AppGLRenderer(
    private val context: Context,
    image: Uri,
    state: EditMenu,
    private val view: GLSurfaceView,
    private val handler: MessageHandler,
) : GLSurfaceView.Renderer, View.OnTouchListener {

    @Volatile
    var state: EditMenu = state
        set(value) {
            if (value != field) {
                view.queueEvent {
                    field = value
                    view.requestRender()
                }
            }
        }

    @Volatile
    var image: Uri = image
        set(value) {
            if (value != field) {
                view.queueEvent {
                    // value writes/updates should happen on GL thread
                    field = value
                    val bitmap = with(context) { image.asBitmap() }
                    GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textures[0])
                    GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bitmap, 0)
                    bitmap.recycle()
                    touchHelper.reset()
                    view.requestRender()
                }
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

    private var viewportWidth = 0
    private var viewportHeight = 0
    private val touchHelper = TouchHelper()

    init {
        view.setOnTouchListener(this)
    }

    private val matrixTransformation = MatrixTransformation(context, verticesBuffer, textureBuffer)

    companion object {
        const val OriginalTextureIdx = 0
        const val PingTextureIdx = 1
        const val PongTextureIdx = 2
    }

    /**
     * Holds ids of textures:
     * * textures[0] holds original texture
     * * textures[1] holds color attachment for ping-pong
     * * textures[2] holds color attachment for ping-pong
     */
    private val textures = IntArray(3)

    private val colorTransformations = listOf(
        //GrayscaleTransformation(context, verticesBuffer, textureBuffer),
        CropTransformation(context, verticesBuffer, textureBuffer, textures),
        // HsvTransformation(context, verticesBuffer, textureBuffer),
        // ContrastTransformation(context, verticesBuffer, textureBuffer),
        // TintTransformation(context, verticesBuffer, textureBuffer),
        // GaussianBlurTransformation(context, verticesBuffer, textureBuffer),
    )
    private val frameBuffers = IntArray(colorTransformations.size)
    private val projectionMatrix = FloatArray(16)
    private val vPMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        view.renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun requestCrop() {
        val t = colorTransformations.first()

        t.cropTextures = true
        t.selectionMode = false
        view.requestRender()
    }

    override fun onDrawFrame(gl: GL10) = with(gl) {
        val t = colorTransformations.first()

        t.selectionMode = state.displayCropSelection
        t.textureWidth = viewportWidth
        t.textureHeight = viewportHeight
        t.pointer = touchHelper.pointer

        t.draw(
            state.displayTransformations,
            frameBuffers[0],
            textures[0]
        )

        if (t.cropTextures) {
            // CropSelection(Point(x = 0, y = 100), Point(x = 1080 / 2, y = 1000))
            // viewport 1080 X 2054
            val selection = state.displayTransformations.crop.selection
            // upside down
            val newOriginY = viewportHeight - selection.height + 5
            val newSelection = selection.moveTo(5, newOriginY)
                .copy(bottomRight = Point(x = selection.width - 5, y = viewportHeight - 5))


            println("New selection $newSelection")

            touchHelper.cropDx = 0f
            touchHelper.cropDy = 0f

            handler(
                OnTransformationUpdated(
                    state.displayTransformations.crop.copy(
                        selection = newSelection
                    )
                )
            )

            view.requestRender()
        }

        t.cropTextures = false
        // we're using previous texture as target for next transformations, render pipeline looks like the following
        // original texture -> grayscale transformation -> texture[1];
        // texture[1] -> hsv transformation -> texture[2];
        // ....
        // texture[last modified texture + 1] -> matrix transformation -> screen
        /*colorTransformations.fastForEachIndexed { index, transformation ->
            transformation.draw(
                transformations,
                frameBuffers[index],
                // fbo index -> txt index mapping: 0 -> 0; 1 -> 1; 2 -> 2; 3 -> 1; 4 -> 2...
                textures[index.takeIf { it == 0 } ?: (1 + ((1 + index) % (textures.size - 1)))],
            )
        }*/
        val ratio = viewportWidth.toFloat() / viewportHeight.toFloat()
        val zoom = (touchHelper.currentSpan + viewportWidth.toFloat()) / viewportWidth.toFloat()

        Matrix.frustumM(
            projectionMatrix,
            0,
            (-ratio - touchHelper.textureDx / viewportWidth) / zoom,
            (ratio - touchHelper.textureDx / viewportWidth) / zoom,
            (1f - touchHelper.textureDy / viewportHeight) / zoom,
            (-1f - touchHelper.textureDy / viewportHeight) / zoom,
            3f,
            7f
        )
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f)
        // Calculate the projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        matrixTransformation.render(vPMatrix, 0, textures[1])
    }

    suspend fun bitmap(): Bitmap = suspendCoroutine { continuation ->
        view.queueEvent {
            GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, frameBuffers[0])
            continuation.resume(
                saveTextureToBitmap(
                    state.displayTransformations.crop.selection.width,
                    state.displayTransformations.crop.selection.height
                )
            )
        }
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES31.glViewport(0, 0, width, height)
        GLES31.glClearColor(1.0f, 1.0f, 1.0f, 1.0f)
        GLES31.glEnable(GLES31.GL_BLEND)
        GLES31.glBlendFunc(GLES31.GL_SRC_ALPHA, GLES31.GL_ONE_MINUS_SRC_ALPHA)

        val bitmap = with(context) { image.asBitmap() }

        GLES31.glGenTextures(2, textures, 0)
        GLES31.glGenFramebuffers(frameBuffers.size, frameBuffers, 0)

        println("viewport $width X $height")

        // bind and load original texture
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textures[0])
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
        for (index in frameBuffers.indices) {
            val texture = textures[1 + (index % (textures.size - 1))]
            GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, frameBuffers[index])
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
            GLES31.glTexImage2D(GLES31.GL_TEXTURE_2D, 0, GLES31.GL_RGBA, width, height, 0, GLES31.GL_RGBA, GLES31.GL_UNSIGNED_INT, null)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE)

            GLES31.glFramebufferTexture2D(GLES31.GL_FRAMEBUFFER, GLES31.GL_COLOR_ATTACHMENT0, GLES31.GL_TEXTURE_2D, texture, 0)
            check(GLES31.glCheckFramebufferStatus(GLES31.GL_FRAMEBUFFER) == GLES31.GL_FRAMEBUFFER_COMPLETE) {
                "non-complete buffer at index: $index"
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(
        v: View,
        event: MotionEvent,
    ): Boolean {
        val crop = state.displayTransformations.crop
        val selectionMode = state.displayCropSelection

        touchHelper.updateScene(event, crop.selection, viewportWidth, viewportHeight, selectionMode)

        if (selectionMode) {
            handler(OnTransformationUpdated(crop.moveTo(touchHelper.cropDx.roundToInt(), touchHelper.cropDy.roundToInt())))
        }
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