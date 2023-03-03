package com.epam.opengl.edu.ui.gl

data class Size(
    val width: Int,
    val height: Int,
) {
    init {
        // require(width * height > 0) { "Invalid size: $this" }
    }
}

@JvmInline
value class Px private constructor(
    val value: Long,
) {
    constructor(x: Float, y: Float) : this(packFloats(x, y))
    constructor() : this(0)
}

inline val Px.x: Float
    get() = unpackFloat1(value)

inline val Px.y: Float
    get() = unpackFloat2(value)

@JvmInline
value class Offset private constructor(
    val value: Long,
) {
    constructor(x: Float, y: Float) : this(packFloats(x, y))
    constructor() : this(0)
}

inline operator fun Offset.plus(
    other: Offset,
) = Offset(x + other.x, y + other.y)

inline operator fun Offset.minus(
    other: Offset,
) = Offset(x - other.x, y - other.y)

inline val Offset.x: Float
    get() = unpackFloat1(value)

inline val Offset.y: Float
    get() = unpackFloat2(value)