package com.epam.opengl.edu.ui.gl

import kotlin.math.roundToInt

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

// todo add sanity checks

data class Rect(
    val topLeft: Px,
    val bottomRight: Px,
)

inline val Rect.size: Size
    get() = Size(
        width = (bottomRight.x - topLeft.x).roundToInt(),
        height = (bottomRight.y - topLeft.y).roundToInt()
    )

fun Rect.moveRightEdge(
    toX: Float,
) = copy(bottomRight = Px(x = toX, y = bottomRight.y))

fun Rect.moveLeftEdge(
    toX: Float,
) = copy(topLeft = Px(x = toX, y = topLeft.y))

fun Rect.moveTopEdge(
    toY: Float,
) = copy(topLeft = Px(x = topLeft.x, y = toY))

fun Rect.moveBottomEdge(
    toY: Float,
) = copy(bottomRight = Px(x = bottomRight.x, y = toY))

fun Rect.offsetTo(
    offset: Offset,
): Rect = copy(
    topLeft = Px(offset.x, offset.y),
    bottomRight = Px(x = offset.x + (bottomRight.x - topLeft.x), y = offset.y + (bottomRight.y - topLeft.y))
)

context (Size)
fun Rect.offsetToWithinBounds(
    offset: Offset,
): Rect {
    val size = size
    val x = offset.x.coerceIn(0f, (width - size.width).toFloat())
    val y = offset.y.coerceIn(0f, (height - size.height).toFloat())

    return offsetTo(Offset(x, y))
}