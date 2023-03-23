package io.github.xlopec.opengl.edu.model.geometry

@JvmInline
value class ScenePoint private constructor(
    val value: Long,
) {
    constructor(x: Float, y: Float) : this(packFloats(x, y))

    override fun toString(): String {
        return "WindowPoint(x=$x, y=$y)"
    }
}

inline val ScenePoint.x: Float
    get() = unpackFloat1(value)

inline val ScenePoint.y: Float
    get() = unpackFloat2(value)

operator fun ScenePoint.minus(
    offset: SceneOffset,
) = ScenePoint(x - offset.x, y - offset.y)