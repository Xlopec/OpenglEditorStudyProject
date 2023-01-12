package com.epam.opengl.edu.ui.gl

import android.content.Context
import android.net.Uri
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY
import android.opengl.GLUtils
import android.opengl.Matrix
import com.epam.opengl.edu.model.Transformations
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class AppGLRenderer(
    private val context: Context,
    image: Uri,
    transformations: Transformations,
    private val view: GLSurfaceView,
) : GLSurfaceView.Renderer {

    @Volatile
    var transformations: Transformations = transformations
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

    private val noTransformation = NoTransformation(context, verticesBuffer, textureBuffer)
    private val grayscaleProgram = GrayscaleTransformation(context, verticesBuffer, textureBuffer)
    private val hsvTransformation = HsvTransformation(context, verticesBuffer, textureBuffer)
    private val contrastTransformation = ContrastTransformation(context, verticesBuffer, textureBuffer)
    private val tintTransformation = TintTransformation(context, verticesBuffer, textureBuffer)

    /**
     * original texture is stored at index 0
     */
    private val textures = IntArray(5)
    private val frameBuffers = IntArray(4)
    private val projectionMatrix = FloatArray(16)
    private val vPMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        view.renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onDrawFrame(gl: GL10) = with(gl) {
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f)
        // Calculate the projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        // we're using previous texture as target for next transformations
        grayscaleProgram.render(transformations.grayscale, frameBuffers[0], textures[0])
        hsvTransformation.render(transformations.brightness, transformations.saturation, frameBuffers[1], textures[1])
        contrastTransformation.render(transformations.contrast, frameBuffers[2], textures[2])
        tintTransformation.render(transformations.tint, frameBuffers[3], textures[3])
        noTransformation.render(vPMatrix, 0, textures[4])
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES31.glViewport(0, 0, width, height)
        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES31.glEnable(GLES31.GL_BLEND)
        GLES31.glBlendFunc(GLES31.GL_SRC_ALPHA, GLES31.GL_ONE_MINUS_SRC_ALPHA)

        val ratio: Float = width.toFloat() / height.toFloat()
        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)

        val bitmap = with(context) { image.asBitmap() }

        GLES31.glGenTextures(textures.size, textures, 0)
        GLES31.glGenFramebuffers(frameBuffers.size, frameBuffers, 0)

        // bind and load original texture
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textures[0])
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bitmap, 0)

        bitmap.recycle()
        // setup color attachments and bind them to corresponding frame buffers
        for (index in frameBuffers.indices) {
            GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, frameBuffers[index])
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textures[index + 1])
            GLES31.glTexImage2D(GLES31.GL_TEXTURE_2D, 0, GLES31.GL_RGBA, width, height, 0, GLES31.GL_RGBA, GLES31.GL_UNSIGNED_INT, null)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE)

            GLES31.glFramebufferTexture2D(GLES31.GL_FRAMEBUFFER, GLES31.GL_COLOR_ATTACHMENT0, GLES31.GL_TEXTURE_2D, textures[index + 1], 0)
            check(GLES31.glCheckFramebufferStatus(GLES31.GL_FRAMEBUFFER) == GLES31.GL_FRAMEBUFFER_COMPLETE) {
                "non-complete buffer at index: $index"
            }
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