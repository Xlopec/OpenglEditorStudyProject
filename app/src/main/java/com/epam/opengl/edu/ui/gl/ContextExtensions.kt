package com.epam.opengl.edu.ui.gl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.opengl.GLES31
import androidx.annotation.RawRes
import java.io.Reader

context (Context)
fun Uri.asBitmap(): Bitmap =
    (contentResolver.openInputStream(this) ?: error("can't open input stream for $this"))
        .use { stream -> BitmapFactory.decodeStream(stream) ?: error("cannot decode input stream for $this") }

fun Context.loadShader(type: Int, @RawRes res: Int): Int {
    val sources = resources.openRawResource(res).reader().use(Reader::readText)

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

fun Context.loadProgram(
    @RawRes vertexShader: Int,
    @RawRes colorShader: Int,
): Int {
    val program = GLES31.glCreateProgram()
    val vertexShader = loadShader(GLES31.GL_VERTEX_SHADER, vertexShader)
    GLES31.glAttachShader(program, vertexShader)

    val fragmentShader = loadShader(GLES31.GL_FRAGMENT_SHADER, colorShader)
    GLES31.glAttachShader(program, fragmentShader)
    GLES31.glLinkProgram(program)

    val linked = IntArray(1)
    GLES31.glGetProgramiv(program, GLES31.GL_LINK_STATUS, linked, 0)
    check(linked[0] == GLES31.GL_TRUE) {
        "Program linking failed, ${GLES31.glGetProgramInfoLog(program)}"
    }
    GLES31.glDeleteShader(fragmentShader)
    GLES31.glDeleteShader(vertexShader)

    return program
}