package io.github.xlopec.opengl.edu.ui.gl

import android.graphics.Bitmap
import android.opengl.GLES31
import android.opengl.GLES31.GL_TEXTURE_HEIGHT
import android.opengl.GLES31.GL_TEXTURE_WIDTH
import android.opengl.GLUtils
import io.github.xlopec.opengl.edu.model.geometry.Size
import io.github.xlopec.opengl.edu.model.geometry.height
import io.github.xlopec.opengl.edu.model.geometry.width
import javax.microedition.khronos.opengles.GL

@JvmInline
value class Texture(
    val value: Int,
)

private val TempTextureSizeHolder = IntArray(2)

context (GL)
var Texture.size: Size
    set(value) {
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, this.value)
        GLES31.glTexImage2D(
            /* target = */ GLES31.GL_TEXTURE_2D,
            /* level = */ 0,
            /* internalformat = */ GLES31.GL_RGBA,
            /* width = */ value.width,
            /* height = */ value.height,
            /* border = */ 0,
            /* format = */ GLES31.GL_RGBA,
            /* type = */ GLES31.GL_UNSIGNED_BYTE,
            /* pixels = */ null
        )
    }
    get() {
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, value)
        GLES31.glGetTexLevelParameteriv(GLES31.GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH, TempTextureSizeHolder, 0)
        GLES31.glGetTexLevelParameteriv(GLES31.GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT, TempTextureSizeHolder, 1)

        return Size(TempTextureSizeHolder[0], TempTextureSizeHolder[1])
    }

fun Texture.updateImage(
    bitmap: Bitmap,
) {
    GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, value)
    GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR)
    GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
    GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE)
    GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE)
    GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bitmap, 0)
}