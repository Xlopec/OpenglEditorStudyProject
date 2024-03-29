package io.github.xlopec.opengl.edu.ui.gl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.net.Uri
import android.opengl.GLES31
import androidx.annotation.RawRes
import io.github.xlopec.opengl.edu.model.geometry.Rect
import io.github.xlopec.opengl.edu.model.geometry.Size
import io.github.xlopec.opengl.edu.model.geometry.x
import io.github.xlopec.opengl.edu.model.geometry.y
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.Reader

context (Context)
fun Uri.asBitmap(
    options: BitmapFactory.Options? = null,
): Bitmap = inputStream { stream ->
    BitmapFactory.decodeStream(stream, null, options) ?: error("cannot decode input stream for $this")
}

context (Context)
fun Uri.asBitmap(
    cropRect: Rect,
    options: BitmapFactory.Options? = null,
): Bitmap = inputStream { stream ->
    (BitmapRegionDecoder.newInstance(stream, false) ?: error("can't create decoder for $this"))
        .decodeRegion(
            android.graphics.Rect(
                cropRect.topLeft.x,
                cropRect.topLeft.y,
                cropRect.bottomRight.x,
                cropRect.bottomRight.y,
            ),
            options
        )
}

context (Context)
suspend fun Uri.decodeImageSize(): Size = withContext(Dispatchers.IO) {
    val options = BitmapFactory.Options()
        .apply { inJustDecodeBounds = true }
        .also { options -> inputStream { stream -> BitmapFactory.decodeStream(stream, null, options) } }
    Size(options.outWidth, options.outHeight)
}

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

context (Context)
private inline fun <T> Uri.inputStream(
    crossinline mapper: (InputStream) -> T,
): T = (contentResolver.openInputStream(this@inputStream) ?: error("can't open input stream for $this")).use(mapper)