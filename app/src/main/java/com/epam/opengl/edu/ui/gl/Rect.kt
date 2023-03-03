package com.epam.opengl.edu.ui.gl

import kotlin.math.abs
import kotlin.math.roundToInt

data class Rect(
    val topLeft: Px,
    val bottomRight: Px,
)

inline val Rect.size: Size
    get() = Size(
        width = (bottomRight.x - topLeft.x).roundToInt(),
        height = (bottomRight.y - topLeft.y).roundToInt()
    )

context (Size)
fun Rect.moveRightEdgeWithinBounds(
    toX: Float,
): Rect {
    val coercedX = toX.coerceIn(topLeft.x + TouchHelper.MinSize, width.toFloat())
    return copy(bottomRight = Px(x = coercedX, y = bottomRight.y))
}

context (Size)
fun Rect.moveLeftEdgeWithinBounds(
    toX: Float,
): Rect {
    val coercedX = toX.coerceIn(0f, bottomRight.x - TouchHelper.MinSize)
    return copy(topLeft = Px(x = coercedX, y = topLeft.y))
}

context (Size)
fun Rect.moveTopEdgeWithinBounds(
    toY: Float,
): Rect {
    val coercedY = toY.coerceIn(0f, bottomRight.y - TouchHelper.MinSize)
    return copy(topLeft = Px(x = topLeft.x, y = coercedY))
}

context (Size)
fun Rect.moveBottomEdgeWithinBounds(
    toY: Float,
): Rect {
    val coercedY = toY.coerceIn(topLeft.y + TouchHelper.MinSize, height.toFloat())
    return copy(bottomRight = Px(x = bottomRight.x, y = coercedY))
}

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

fun Px.isOnRightEdgeOf(
    rect: Rect,
    tolerancePx: Float,
): Boolean = abs(x - rect.bottomRight.x) <= tolerancePx && y in rect.topLeft.y..rect.bottomRight.y

fun Px.isOnLeftEdgeOf(
    rect: Rect,
    tolerancePx: Float,
): Boolean = abs(x - rect.topLeft.x) <= tolerancePx && y in rect.topLeft.y..rect.bottomRight.y


fun Px.isOnTopEdgeOf(
    rect: Rect,
    tolerancePx: Float,
): Boolean = abs(y - rect.topLeft.y) <= tolerancePx && x in rect.topLeft.x..rect.bottomRight.x

fun Px.isOnBottomEdgeOf(
    rect: Rect,
    tolerancePx: Float,
): Boolean = abs(y - rect.bottomRight.y) <= tolerancePx && x in rect.topLeft.x..rect.bottomRight.x
