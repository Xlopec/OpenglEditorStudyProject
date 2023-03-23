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

    constructor() : this(IntArray(3))
}

inline val Textures.originalTexture: Int
    get() = array[0]

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