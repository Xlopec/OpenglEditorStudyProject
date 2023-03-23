package com.epam.opengl.edu.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY
import android.opengl.GLUtils
import android.opengl.Matrix
import androidx.annotation.RawRes
import com.epam.opengl.edu.R
import com.epam.opengl.edu.model.Transformations
import java.io.Reader
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
                field = value
                view.requestRender()
            }
        }

    @Volatile
    var image: Uri = image
        set(value) {
            if (value != field) {
                view.queueEvent {
                    // value writes/updates should happen on GL thread
                    field = value
                    val bitmap = image.asBitmap()
                    GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textures[0])
                    GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bitmap, 0)
                    bitmap.recycle()
                    view.requestRender()
                }
            }
        }

    private val vertices = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )

    private val textureVertices = floatArrayOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )

    private val verticesBuffer: FloatBuffer = run {
        val buff: ByteBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
        buff.order(ByteOrder.nativeOrder())
        val verticesBuffer = buff.asFloatBuffer()
        verticesBuffer.put(vertices)
        verticesBuffer.position(0)
        verticesBuffer
    }
    private val textureBuffer: FloatBuffer = run {
        val buff: ByteBuffer = ByteBuffer.allocateDirect(textureVertices.size * 4)

        buff.order(ByteOrder.nativeOrder())
        val textureBuffer = buff.asFloatBuffer()
        textureBuffer.put(textureVertices)
        textureBuffer.position(0)
        textureBuffer
    }

    // must be init only inside current GL context
    private val vertexShader by lazy { loadShader(GLES31.GL_VERTEX_SHADER, R.raw.vertex) }
    private val fragmentShader by lazy { loadShader(GLES31.GL_FRAGMENT_SHADER, R.raw.fragment) }
    private val program by lazy {
        val program = GLES31.glCreateProgram()
        GLES31.glAttachShader(program, vertexShader)
        GLES31.glAttachShader(program, fragmentShader)
        GLES31.glLinkProgram(program)

        val linked = IntArray(1)
        GLES31.glGetProgramiv(program, GLES31.GL_LINK_STATUS, linked, 0)
        check(linked[0] == GLES31.GL_TRUE) {
            "Program linking failed, ${GLES31.glGetProgramInfoLog(program)}"
        }
        program
    }

    private val textures = IntArray(1)
    private val projectionMatrix = FloatArray(16)
    private val vPMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        view.renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onDrawFrame(unused: GL10) {
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f)
        // Calculate the projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0)
        GLES31.glUseProgram(program)
        GLES31.glDisable(GLES31.GL_BLEND)
        val vPMatrixHandle = GLES31.glGetUniformLocation(program, "uMVPMatrix")
        val positionHandle = GLES31.glGetAttribLocation(program, "aPosition")
        val textureHandle = GLES31.glGetUniformLocation(program, "uTexture")
        val texturePositionHandle = GLES31.glGetAttribLocation(program, "aTexPosition")
        val grayscaleHandle = GLES31.glGetUniformLocation(program, "grayscale")
        GLES31.glVertexAttribPointer(texturePositionHandle, 2, GLES31.GL_FLOAT, false, 0, textureBuffer)
        GLES31.glEnableVertexAttribArray(texturePositionHandle)
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textures[0])
        GLES31.glUniform1i(textureHandle, 0)
        GLES31.glUniform1f(grayscaleHandle, 1f - transformations.grayscale.value)
        GLES31.glUniformMatrix4fv(vPMatrixHandle, 1, false, vPMatrix, 0)
        GLES31.glVertexAttribPointer(positionHandle, 2, GLES31.GL_FLOAT, false, 0, verticesBuffer)
        GLES31.glEnableVertexAttribArray(positionHandle)
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4)
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES31.glViewport(0, 0, width, height)
        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        val ratio: Float = width.toFloat() / height.toFloat()
        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)

        val bitmap = image.asBitmap()

        GLES31.glGenTextures(1, textures, 0)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textures[0])
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bitmap, 0)

        bitmap.recycle()
    }

    private fun Uri.asBitmap() =
        (context.contentResolver.openInputStream(this) ?: error("can't open input stream for $this"))
            .use { stream -> BitmapFactory.decodeStream(stream) }

    private fun loadShader(type: Int, @RawRes res: Int): Int {
        val sources = context.resources.openRawResource(res).reader().use(Reader::readText)

        val shader = GLES31.glCreateShader(type)
        GLES31.glShaderSource(shader, sources)
        GLES31.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES31.glGetShaderiv(shader, GLES31.GL_COMPILE_STATUS, compiled, 0)
        check(compiled[0] == GLES31.GL_TRUE) {
            "Shader compilation failed ${GLES31.glGetShaderInfoLog(shader)}"
        }
        return shader
    }

}