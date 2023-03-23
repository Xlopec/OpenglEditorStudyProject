package io.github.xlopec.opengl.edu.ui.gl

import android.graphics.Bitmap
import android.opengl.GLES31
import android.opengl.GLUtils
import io.github.xlopec.opengl.edu.model.geometry.Offset
import io.github.xlopec.opengl.edu.model.geometry.Size
import io.github.xlopec.opengl.edu.model.geometry.height
import io.github.xlopec.opengl.edu.model.geometry.packInts
import io.github.xlopec.opengl.edu.model.geometry.unpackInt1
import io.github.xlopec.opengl.edu.model.geometry.unpackInt2
import io.github.xlopec.opengl.edu.model.geometry.width
import javax.microedition.khronos.opengles.GL

@JvmInline
value class Fbo private constructor(
    private val value: Long,
) {

    constructor(frameBuffer: Int, texture: Int) : this(packInts(frameBuffer, texture))

    companion object {
        const val PingPongTexturesSize = 2

        context (GL)
        fun forBitmap(
            bitmap: Bitmap,
        ): Fbo {
            val frameBuffer = createGlFrameBuffers(1)[0]
            val texture = createGlTextures(1)[0]

            GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, frameBuffer)
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE)

            GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bitmap, 0)

            GLES31.glFramebufferTexture2D(
                GLES31.GL_FRAMEBUFFER,
                GLES31.GL_COLOR_ATTACHMENT0,
                GLES31.GL_TEXTURE_2D,
                texture,
                0
            )

            checkBufferIsComplete(frameBuffer, texture)
            return Fbo(frameBuffer, texture)
        }

        context (GL)
        fun forPingPong(
            size: Size,
            count: Int,
        ): List<Fbo> {
            require(count > 0)
            val frameBuffers = createGlFrameBuffers(count)
            val textures = createGlTextures(PingPongTexturesSize)
            // setup color attachments and bind them to corresponding frame buffers
            // note that textures are shifted by one!
            // buffers binding look like the following:
            // fbo 0 (screen bound) -> texture[0] (original texture)
            // frameBuffers[0] -> textures[1]
            // frameBuffers[1] -> textures[2]
            // frameBuffers[2] -> textures[1]
            // frameBuffers[3] -> textures[2]
            // ...
            return List(count) { index ->
                val frameBuffer = frameBuffers[index]
                val texture = textures[index % textures.size]

                GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, frameBuffer)
                GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
                GLES31.glTexImage2D(
                    /* target = */ GLES31.GL_TEXTURE_2D,
                    /* level = */ 0,
                    /* internalformat = */ GLES31.GL_RGBA,
                    /* width = */ size.width,
                    /* height = */ size.height,
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

                checkBufferIsComplete(frameBuffer, texture)
                Fbo(frameBuffer, texture)
            }
        }

    }

    val frameBuffer: FrameBuffer
        get() = FrameBuffer(unpackInt1(value))

    val texture: Texture
        get() = Texture(unpackInt2(value))

    //context (GL)
    fun updateImage(
        bitmap: Bitmap,
    ) {
        texture.updateImage(bitmap)
    }

    //context (GL)
    fun resize(
        offset: Offset,
        size: Size,
    ) {
        // fixme should be a better way to resize texture
        val bitmap = readTextureToBitmap(frameBuffer, size, offset)
        texture.updateImage(bitmap)
        bitmap.recycle()
    }

    override fun toString(): String {
        return "Fbo(frameBuffer=$frameBuffer, texture=$texture)"
    }
}

context (GL)
        private fun createGlFrameBuffers(
    count: Int,
): IntArray {
    val fbo = IntArray(count)
    GLES31.glGenFramebuffers(fbo.size, fbo, 0)
    return fbo
}

context (GL)
        private fun createGlTextures(
    count: Int,
): IntArray {
    val textures = IntArray(count)
    GLES31.glGenTextures(textures.size, textures, 0)
    return textures
}

context (GL)
        private fun checkBufferIsComplete(
    frameBuffer: Int,
    texture: Int,
) = check(GLES31.glCheckFramebufferStatus(GLES31.GL_FRAMEBUFFER) == GLES31.GL_FRAMEBUFFER_COMPLETE) {
    "incomplete buffer $frameBuffer, texture $texture"
}