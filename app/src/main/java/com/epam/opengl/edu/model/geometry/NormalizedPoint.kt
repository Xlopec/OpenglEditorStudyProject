package com.epam.opengl.edu.model.geometry

@JvmInline
value class NormalizedPoint private constructor(
    val value: Long,
) {
    constructor(x: Float, y: Float) : this(packFloats(x, y))

    companion object {
        const val Min = 0f
        const val Max = 1f

        fun safeOf(
            x: Float,
            y: Float,
        ): NormalizedPoint = NormalizedPoint(x.coerceIn(Min, Max), y.coerceIn(Min, Max))
    }

    init {
        require(x in Min..Max) { "$x can't be larger than 1" }
        require(y in Min..Max) { "$y can't be larger than 1" }
    }

    override fun toString(): String {
        return "NormalizedPoint(x=$x, y=$y)"
    }
}

inline val NormalizedPoint.x: Float
    get() = unpackFloat1(value)

inline val NormalizedPoint.y: Float
    get() = unpackFloat2(value)

operator fun NormalizedPoint.component1(): Float = x

operator fun NormalizedPoint.component2(): Float = y