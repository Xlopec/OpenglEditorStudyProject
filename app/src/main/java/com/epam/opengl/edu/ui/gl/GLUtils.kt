package com.epam.opengl.edu.ui.gl

import android.graphics.Bitmap
import android.opengl.GLES31
import com.epam.opengl.edu.model.geometry.Offset
import com.epam.opengl.edu.model.geometry.Size
import com.epam.opengl.edu.model.geometry.height
import com.epam.opengl.edu.model.geometry.width
import com.epam.opengl.edu.model.geometry.x
import com.epam.opengl.edu.model.geometry.y
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun readTextureToBuffer(
    offset: Offset,
    size: Size,
): Buffer {
    val buffer = ByteBuffer.allocateDirect(size.width * size.height * 4)
        .order(ByteOrder.nativeOrder())
        .position(0)

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
    size: Size,
    sceneOffset: Offset = Offset(0, 0),
): Bitmap {
    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(readTextureToBuffer(sceneOffset, size))
    return bitmap
}