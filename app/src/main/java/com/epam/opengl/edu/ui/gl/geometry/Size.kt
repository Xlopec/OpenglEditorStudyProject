package com.epam.opengl.edu.ui.gl.geometry

@JvmInline
value class Size private constructor(
    val value: Long,
) {

    constructor(width: Int, height: Int) : this(packInts(width, height))

    init {
        // require(width * height > 0) { "Invalid size: $this" }
    }
}

inline val Size.width: Int
    get() = unpackInt1(value)

inline val Size.height: Int
    get() = unpackInt2(value)
