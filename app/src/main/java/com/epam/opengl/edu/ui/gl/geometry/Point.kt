package com.epam.opengl.edu.ui.gl.geometry

@JvmInline
value class Point private constructor(
    val value: Long,
) {
    constructor(x: Int, y: Int) : this(packInts(x, y))
    constructor() : this(0)
}

inline val Point.x: Int
    get() = unpackInt1(value)

inline val Point.y: Int
    get() = unpackInt2(value)