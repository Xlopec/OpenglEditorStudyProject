package com.epam.opengl.edu.model.geometry

@JvmInline
value class Point private constructor(
    val value: Long,
) {
    constructor(x: Int, y: Int) : this(packInts(x, y))
    override fun toString(): String {
        return "Point(x=$x, y=$y)"
    }
}

inline val Point.x: Int
    get() = unpackInt1(value)

inline val Point.y: Int
    get() = unpackInt2(value)