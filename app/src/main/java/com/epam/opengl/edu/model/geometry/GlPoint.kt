package com.epam.opengl.edu.model.geometry

@JvmInline
value class GlPoint private constructor(
    val value: Long,
) {
    constructor(x: Float, y: Float) : this(packFloats(x, y))

    companion object {
        const val Min = 0f
        const val Max = 1f

        fun fromPoint(
            point: Point,
            imageSize: Size,
        ): GlPoint = GlPoint(
            x = (point.x / imageSize.width.toFloat()).coerceIn(Min, Max),
            // openGL y origin is bottom left corner
            y = 1f - (point.y / imageSize.height.toFloat()).coerceIn(Min, Max)
        )
    }

    init {
        require(x in Min..Max) { "$x can't be larger than 1" }
        require(y in Min..Max) { "$y can't be larger than 1" }
    }

    override fun toString(): String {
        return "GlPoint(x=$x, y=$y)"
    }
}

inline val GlPoint.x: Float
    get() = unpackFloat1(value)

inline val GlPoint.y: Float
    get() = unpackFloat2(value)

operator fun GlPoint.component1(): Float = x

operator fun GlPoint.component2(): Float = y
