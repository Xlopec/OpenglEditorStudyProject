package com.epam.opengl.edu.ui.gl.geometry

@JvmInline
value class NormalizedPoint private constructor(
    val value: Long,
) {
    constructor(x: Float, y: Float) : this(packFloats(x, y))

    init {
        require(x in 0f..1f) { "$x can't be larger than 1" }
        require(y in 0f..1f) { "$y can't be larger than 1" }
    }
}

inline val NormalizedPoint.x: Float
    get() = unpackFloat1(value)

inline val NormalizedPoint.y: Float
    get() = unpackFloat1(value)

operator fun NormalizedPoint.component1(): Float = x

operator fun NormalizedPoint.component2(): Float = y