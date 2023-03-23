package io.github.xlopec.opengl.edu.ui.gl

import android.graphics.Bitmap
import android.opengl.GLES31
import android.opengl.GLUtils
import io.github.xlopec.opengl.edu.model.geometry.Size
import io.github.xlopec.opengl.edu.model.geometry.height
import io.github.xlopec.opengl.edu.model.geometry.width
import java.nio.Buffer
import javax.microedition.khronos.opengles.GL

/**
 * Holds textures:
 * * textures[0] holds original texture
 * * textures[1] holds color attachment for ping-pong
 * * textures[2] holds color attachment for ping-pong
 */
@JvmInline
value class Textures private constructor(
    val array: IntArray,
) {

    companion object {
        const val OriginalTextureIdx = 0
        const val PingTextureIdx = 1
        const val PongTextureIdx = 2
    }

    constructor() : this(IntArray(3))
}

inline val Textures.originalTexture: Int
    get() = array[Textures.OriginalTextureIdx]

inline val Textures.pingTexture: Int
    get() = array[Textures.PingTextureIdx]

inline val Textures.pongTexture: Int
    get() = array[Textures.PongTextureIdx]

inline val Textures.size: Int
    get() = array.size

@Suppress("NOTHING_TO_INLINE")
inline operator fun Textures.get(i: Int) = array[i]

@Suppress("NOTHING_TO_INLINE")
inline fun Textures.readTextureFor(
    fboIdx: Int,
): Int = this[fboIdx.takeIf { it == 0 } ?: (1 + ((1 + fboIdx) % (size - 1)))]

@Suppress("NOTHING_TO_INLINE")
inline fun Textures.bindTextureFor(
    fboIdx: Int,
): Int = this[1 + fboIdx % (size - 1)]

// context (GL)
fun Textures.resizePingPongTextures(
    size: Size,
) {
    for (index in Textures.PingTextureIdx..Textures.PongTextureIdx) {
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, this[index])
        GLES31.glTexImage2D(
            GLES31.GL_TEXTURE_2D,
            0,
            GLES31.GL_RGBA,
            size.width,
            size.height,
            0,
            GLES31.GL_RGBA,
            GLES31.GL_UNSIGNED_BYTE,
            null
        )
    }
}

// context (GL)
fun Textures.updateTextures(
    bitmap: Bitmap,
) {
    val imageSize = Size(bitmap.width, bitmap.height)

    GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, originalTexture)
    GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR)
    GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
    GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE)
    GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE)
    GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bitmap, 0)

    bitmap.recycle()

    resizePingPongTextures(imageSize)
}

context (GL)
fun Textures.updateTextures(
    buffer: Buffer,
    size: Size,
) {
    GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, originalTexture)
    GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR)
    GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
    GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE)
    GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE)
    GLES31.glTexImage2D(
        GLES31.GL_TEXTURE_2D,
        0,
        GLES31.GL_RGBA,
        size.width,
        size.height,
        0,
        GLES31.GL_RGBA,
        GLES31.GL_UNSIGNED_BYTE,
        buffer
    )

    resizePingPongTextures(size)
}