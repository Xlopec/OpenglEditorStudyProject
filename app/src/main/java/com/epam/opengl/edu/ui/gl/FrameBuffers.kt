package com.epam.opengl.edu.ui.gl

@JvmInline
value class FrameBuffers private constructor(
    val array: IntArray,
) {

    constructor(
        size: Int,
    ) : this(IntArray(size))

    inline val cropFrameBuffer: Int
        get() = array.last()

    inline val size: Int
        get() = array.size

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun get(i: Int) = array[i]

}