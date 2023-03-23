package io.github.xlopec.opengl.edu.ui.gl

import android.graphics.Bitmap
import android.opengl.GLES31
import io.github.xlopec.opengl.edu.model.geometry.Offset
import io.github.xlopec.opengl.edu.model.geometry.Size
import io.github.xlopec.opengl.edu.model.geometry.height
import io.github.xlopec.opengl.edu.model.geometry.width
import io.github.xlopec.opengl.edu.model.geometry.x
import io.github.xlopec.opengl.edu.model.geometry.y
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun readTextureToBuffer(
    frameBuffer: FrameBuffer,
    offset: Offset,
    size: Size,
): Buffer {
    val buffer = ByteBuffer.allocateDirect(size.width * size.height * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .position(0)

    GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, frameBuffer.value)
    GLES31.glReadPixels(
        /* x = */ offset.x,
        /* y = */ offset.y,
        /* width = */ size.width,
        /* height = */ size.height,
        /* format = */ GLES31.GL_RGBA,
        /* type = */ GLES31.GL_UNSIGNED_BYTE,
        /* pixels = */ buffer
    )

    return buffer
}

fun readTextureToBitmap(
    frameBuffer: FrameBuffer,
    size: Size,
    sceneOffset: Offset = Offset(0, 0),
): Bitmap {
    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(readTextureToBuffer(frameBuffer, sceneOffset, size))
    return bitmap
}