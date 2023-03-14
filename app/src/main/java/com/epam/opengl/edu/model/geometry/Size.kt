package com.epam.opengl.edu.model.geometry

@JvmInline
value class Size private constructor(
    val value: Long,
) {

    constructor(width: Int, height: Int) : this(packInts(width, height))

    init {
        require(width * height > 0) { "Invalid size: $this" }
    }

    override fun toString(): String {
        return "Size(width=$width, height=$height)"
    }
}

inline val Size.width: Int
    get() = unpackInt1(value)

inline val Size.height: Int
    get() = unpackInt2(value)

operator fun Size.minus(
    offset: Offset
): Size = Size(width = width - offset.x, height = height - offset.y)

operator fun Size.plus(
    offset: Offset
): Size = Size(width = width + offset.x, height = height + offset.y)

