package com.epam.opengl.edu.ui.gl

/**
 * Holds textures:
 * * textures[0] holds original texture
 * * textures[1] holds color attachment for ping-pong
 * * textures[2] holds color attachment for ping-pong
 * * textures[3] holds texture for cropping
 */
@JvmInline
value class Textures private constructor(
    val array: IntArray,
) {

    companion object {
        const val PingTextureIdx = 1
        const val PongTextureIdx = 2
    }

    constructor() : this(IntArray(4))

    inline val originalTexture: Int
        get() = array[0]

    inline val pingTexture: Int
        get() = array[PingTextureIdx]

    inline val pongTexture: Int
        get() = array[PongTextureIdx]

    inline val cropTexture: Int
        get() = array[3]

    inline val size: Int
        get() = array.size

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun get(i: Int) = array[i]

}