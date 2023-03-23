package com.epam.opengl.edu.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import org.intellij.lang.annotations.Language

class AppGLRenderer(
    private val context: Context,
    image: Uri,
    private val view: GLSurfaceView
) : GLSurfaceView.Renderer {

    init {
        println("init renderer")
    }

    var image: Uri = image
        set(value) {
            if (value != field) {
                field = value
                view.requestRender()
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

    @Language("GLSL")
    private val vertexShaderCode = """
        attribute vec4 aPosition;  
        attribute vec2 aTexPosition;  
        varying vec2 vTexPosition; 
        void main() { 
          gl_Position = aPosition; 
          vTexPosition = aTexPosition; 
        }
    """.trimIndent()

    @Language("GLSL")
    private val fragmentShaderCode = """
        precision mediump float; 
        uniform sampler2D uTexture; 
        varying vec2 vTexPosition; 
        void main() { 
          gl_FragColor = texture2D(uTexture, vTexPosition); 
        }
    """.trimIndent()

    private val vertexShader by lazy { loadShader(GLES31.GL_VERTEX_SHADER, vertexShaderCode) }
    private val fragmentShader by lazy { loadShader(GLES31.GL_FRAGMENT_SHADER, fragmentShaderCode) }
    private val program by lazy {
        val program = GLES31.glCreateProgram()
        GLES31.glAttachShader(program, vertexShader)
        GLES31.glAttachShader(program, fragmentShader)
        GLES31.glLinkProgram(program)
        program
    }

    private val textures = IntArray(1)

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        view.renderMode = RENDERMODE_WHEN_DIRTY
        println("create surface")
    }

    override fun onDrawFrame(unused: GL10) {
        draw(textures[0])
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        println("Image ${image}")
        GLES31.glViewport(0, 0, width, height)
        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        val bitmap = (context.contentResolver.openInputStream(image) ?: error("can't open input stream for $image"))
            .use { stream -> BitmapFactory.decodeStream(stream) }

        GLES31.glGenTextures(1, textures, 0)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textures[0])
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bitmap, 0)

        bitmap.recycle()
    }

    private fun draw(texture: Int) {
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0)
        GLES31.glUseProgram(program)
        GLES31.glDisable(GLES31.GL_BLEND)
        val positionHandle = GLES31.glGetAttribLocation(program, "aPosition")
        val textureHandle = GLES31.glGetUniformLocation(program, "uTexture")
        val texturePositionHandle = GLES31.glGetAttribLocation(program, "aTexPosition")
        GLES31.glVertexAttribPointer(texturePositionHandle, 2, GLES31.GL_FLOAT, false, 0, textureBuffer)
        GLES31.glEnableVertexAttribArray(texturePositionHandle)
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
        GLES31.glUniform1i(textureHandle, 0)
        GLES31.glVertexAttribPointer(positionHandle, 2, GLES31.GL_FLOAT, false, 0, verticesBuffer)
        GLES31.glEnableVertexAttribArray(positionHandle)
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4)
    }

}

private fun loadShader(type: Int, sources: String): Int {
    val shader = GLES31.glCreateShader(type)
    GLES31.glShaderSource(shader, sources)
    GLES31.glCompileShader(shader)
    val compiled = IntArray(1)
    GLES31.glGetShaderiv(shader, GLES31.GL_COMPILE_STATUS, compiled, 0)

    if (compiled[0] == 0) {
        throw RuntimeException("Compilation failed : " + GLES31.glGetShaderInfoLog(shader))
    }

    return shader
}