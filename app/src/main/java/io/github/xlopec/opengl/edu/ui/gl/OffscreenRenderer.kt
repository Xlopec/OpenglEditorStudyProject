package io.github.xlopec.opengl.edu.ui.gl

import android.graphics.Bitmap
import android.opengl.GLES31
import android.opengl.GLUtils
import io.github.xlopec.opengl.edu.model.geometry.Offset
import io.github.xlopec.opengl.edu.model.geometry.Size
import io.github.xlopec.opengl.edu.model.geometry.height
import io.github.xlopec.opengl.edu.model.geometry.width
import javax.microedition.khronos.opengles.GL

context (GL)
class OffscreenRenderer(
    fboCount: Int,
    bitmap: Bitmap,
) {
    // todo inline classes
    private val frameBuffers = FrameBuffers(fboCount)
    private val textures = Textures()

    init {
        GLES31.glGenTextures(textures.size, textures.array, 0)
        GLES31.glGenFramebuffers(frameBuffers.size, frameBuffers.array, 0)

        val imageSize = Size(bitmap.width, bitmap.height)

        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textures.originalTexture)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bitmap, 0)
        // setup color attachments and bind them to corresponding frame buffers
        // note that textures are shifted by one!
        // buffers binding look like the following:
        // fbo 0 (screen bound) -> texture[0] (original texture)
        // frameBuffers[0] -> textures[1]
        // frameBuffers[1] -> textures[2]
        // frameBuffers[2] -> textures[1]
        // frameBuffers[3] -> textures[2]
        // ...
        for (frameBufferIndex in 0 until frameBuffers.size) {
            setupFrameBuffer(frameBuffers[frameBufferIndex], textures.bindTextureFor(frameBufferIndex), imageSize)
        }
    }

    val cropFrameBuffer: Int
        get() = frameBuffers.cropFrameBuffer

    val fboCount: Int
        get() = frameBuffers.size

    fun fbo(
        index: Int,
    ) = frameBuffers[index]

    fun sourceTextureForFbo(
        index: Int,
    ) = textures.readTextureFor(index)

    fun updateTextures(
        bitmap: Bitmap,
    ) {
        textures.updateTextures(bitmap)
    }

    fun resizeTextures(
        offset: Offset,
        size: Size,
    ) {
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textures.originalTexture)
        val buffer = readTextureToBuffer(offset, size)
        textures.updateTextures(buffer, size)
    }

    private fun setupFrameBuffer(
        frameBuffer: Int,
        texture: Int,
        size: Size,
    ) {
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
        check(GLES31.glCheckFramebufferStatus(GLES31.GL_FRAMEBUFFER) == GLES31.GL_FRAMEBUFFER_COMPLETE) {
            "incomplete buffer $frameBuffer, texture $texture"
        }
    }


}