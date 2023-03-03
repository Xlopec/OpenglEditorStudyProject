package com.epam.opengl.edu.ui.gl.geometry

@JvmInline
value class Offset private constructor(
    val value: Long,
) {
    constructor(x: Int, y: Int) : this(packInts(x, y))
    constructor() : this(0)
}

inline operator fun Offset.plus(
    other: Offset,
) = Offset(x + other.x, y + other.y)

inline operator fun Offset.minus(
    other: Offset,
) = Offset(x - other.x, y - other.y)

inline val Offset.x: Int
    get() = unpackInt1(value)

inline val Offset.y: Int
    get() = unpackInt2(value)