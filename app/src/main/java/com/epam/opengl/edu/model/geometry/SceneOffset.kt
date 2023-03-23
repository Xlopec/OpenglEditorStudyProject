package com.epam.opengl.edu.model.geometry

import kotlin.math.roundToInt

@JvmInline
value class SceneOffset private constructor(
    val value: Long,
) {
    constructor(x: Float, y: Float) : this(packFloats(x, y))

    override fun toString(): String {
        return "WindowOffset(x=$x, y=$y)"
    }
}

fun SceneOffset(
    first: ScenePoint,
    second: ScenePoint,
) = SceneOffset(first.x - second.x, first.y - second.y)

inline operator fun SceneOffset.plus(
    other: SceneOffset,
) = SceneOffset(x + other.x, y + other.y)

inline operator fun SceneOffset.minus(
    other: SceneOffset,
) = SceneOffset(x - other.x, y - other.y)

inline val SceneOffset.x: Float
    get() = unpackFloat1(value)

inline val SceneOffset.y: Float
    get() = unpackFloat2(value)

operator fun SceneOffset.component1(): Float = x

operator fun SceneOffset.component2(): Float = y

operator fun Size.minus(
    offset: SceneOffset,
): Size = Size(width - offset.x.roundToInt(), height - offset.y.roundToInt())

operator fun Size.plus(
    offset: SceneOffset,
): Size = Size(width + offset.x.roundToInt(), height + offset.y.roundToInt())
