package com.epam.opengl.edu.ui.gl

import kotlin.math.abs

data class Rect(
    val topLeft: Px,
    val bottomRight: Px,
)

inline val Rect.size: Size
    get() = Size(
        width = bottomRight.x - topLeft.x,
        height = bottomRight.y - topLeft.y
    )

context (Size)
fun Rect.moveRightEdgeWithinBounds(
    toX: Int,
): Rect {
    val coercedX = toX.coerceIn(topLeft.x + TouchHelper.MinSize, width)
    return copy(bottomRight = Px(x = coercedX, y = bottomRight.y))
}

context (Size)
fun Rect.moveLeftEdgeWithinBounds(
    toX: Int,
): Rect {
    val coercedX = toX.coerceIn(0, bottomRight.x - TouchHelper.MinSize)
    return copy(topLeft = Px(x = coercedX, y = topLeft.y))
}

context (Size)
fun Rect.moveTopEdgeWithinBounds(
    toY: Int,
): Rect {
    val coercedY = toY.coerceIn(0, bottomRight.y - TouchHelper.MinSize)
    return copy(topLeft = Px(x = topLeft.x, y = coercedY))
}

context (Size)
fun Rect.moveBottomEdgeWithinBounds(
    toY: Int,
): Rect {
    val coercedY = toY.coerceIn(topLeft.y + TouchHelper.MinSize, height)
    return copy(bottomRight = Px(x = bottomRight.x, y = coercedY))
}

fun Rect.offsetTo(
    offset: Offset,
): Rect = copy(
    topLeft = Px(offset.x, offset.y),
    bottomRight = Px(x = offset.x + (bottomRight.x - topLeft.x), y = offset.y + (bottomRight.y - topLeft.y))
)

context (Size)
fun Rect.offsetByWithinBounds(
    offset: Offset,
): Rect {
    val size = size
    val x = (topLeft.x + offset.x).coerceIn(0, width - size.width)
    val y = (topLeft.y + offset.y).coerceIn(0, height - size.height)

    return offsetTo(Offset(x, y))
}

fun Px.isOnRightEdgeOf(
    rect: Rect,
    tolerancePx: Int,
): Boolean = abs(x - rect.bottomRight.x) <= tolerancePx && y in rect.topLeft.y..rect.bottomRight.y

fun Px.isOnLeftEdgeOf(
    rect: Rect,
    tolerancePx: Int,
): Boolean = abs(x - rect.topLeft.x) <= tolerancePx && y in rect.topLeft.y..rect.bottomRight.y


fun Px.isOnTopEdgeOf(
    rect: Rect,
    tolerancePx: Int,
): Boolean = abs(y - rect.topLeft.y) <= tolerancePx && x in rect.topLeft.x..rect.bottomRight.x

fun Px.isOnBottomEdgeOf(
    rect: Rect,
    tolerancePx: Int,
): Boolean = abs(y - rect.bottomRight.y) <= tolerancePx && x in rect.topLeft.x..rect.bottomRight.x
