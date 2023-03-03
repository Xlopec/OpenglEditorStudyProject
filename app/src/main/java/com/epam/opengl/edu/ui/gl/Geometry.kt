package com.epam.opengl.edu.ui.gl

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

@JvmInline
value class Px private constructor(
    val value: Long,
) {
    constructor(x: Int, y: Int) : this(packInts(x, y))
    constructor() : this(0)
}

inline val Px.x: Int
    get() = unpackInt1(value)

inline val Px.y: Int
    get() = unpackInt2(value)

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

// todo add sanity checks