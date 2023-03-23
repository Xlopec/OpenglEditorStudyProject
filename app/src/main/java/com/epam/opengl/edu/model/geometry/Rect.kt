package com.epam.opengl.edu.model.geometry

import com.epam.opengl.edu.model.transformation.Scene
import kotlin.math.abs

data class Rect(
    val topLeft: Point,
    val bottomRight: Point,
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
    val coercedX = toX.coerceIn(topLeft.x + Scene.MinSize, width)
    return copy(bottomRight = Point(x = coercedX, y = bottomRight.y))
}

context (Size)
fun Rect.moveLeftEdgeWithinBounds(
    toX: Int,
): Rect {
    val coercedX = toX.coerceIn(0, bottomRight.x - Scene.MinSize)
    return copy(topLeft = Point(x = coercedX, y = topLeft.y))
}

context (Size)
fun Rect.moveTopEdgeWithinBounds(
    toY: Int,
): Rect {
    val coercedY = toY.coerceIn(0, bottomRight.y - Scene.MinSize)
    return copy(topLeft = Point(x = topLeft.x, y = coercedY))
}

context (Size)
fun Rect.moveBottomEdgeWithinBounds(
    toY: Int,
): Rect {
    val coercedY = toY.coerceIn(topLeft.y + Scene.MinSize, height)
    return copy(bottomRight = Point(x = bottomRight.x, y = coercedY))
}

fun Rect.offsetTo(
    offset: Offset,
): Rect = copy(
    topLeft = Point(offset.x, offset.y),
    bottomRight = Point(x = offset.x + (bottomRight.x - topLeft.x), y = offset.y + (bottomRight.y - topLeft.y))
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

context (Size)
fun Rect.offsetToWithinBounds(
    offset: Offset,
): Rect {
    val size = size
    val x = offset.x.coerceIn(0, width - size.width)
    val y = offset.y.coerceIn(0, height - size.height)

    return offsetTo(Offset(x, y))
}

fun Point.isOnRightEdgeOf(
    rect: Rect,
    tolerancePx: Int,
): Boolean = abs(x - rect.bottomRight.x) <= tolerancePx && y in rect.topLeft.y..rect.bottomRight.y

fun Point.isOnLeftEdgeOf(
    rect: Rect,
    tolerancePx: Int,
): Boolean = abs(x - rect.topLeft.x) <= tolerancePx && y in rect.topLeft.y..rect.bottomRight.y


fun Point.isOnTopEdgeOf(
    rect: Rect,
    tolerancePx: Int,
): Boolean = abs(y - rect.topLeft.y) <= tolerancePx && x in rect.topLeft.x..rect.bottomRight.x

fun Point.isOnBottomEdgeOf(
    rect: Rect,
    tolerancePx: Int,
): Boolean = abs(y - rect.bottomRight.y) <= tolerancePx && x in rect.topLeft.x..rect.bottomRight.x
