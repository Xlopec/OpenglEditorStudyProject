package io.github.xlopec.opengl.edu.model.geometry

import io.github.xlopec.opengl.edu.model.transformation.Scene
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
fun Rect.moveTopLeftCornerWithinBounds(
    to: Point,
): Rect {
    val coercedX = to.x.coerceIn(0, bottomRight.x - Scene.MinSize)
    val coercedY = to.y.coerceIn(0, bottomRight.y - Scene.MinSize)
    return copy(topLeft = Point(x = coercedX, y = coercedY))
}

context (Size)
fun Rect.moveBottomLeftCornerWithinBounds(
    to: Point,
): Rect {
    val coercedX = to.x.coerceIn(0, bottomRight.x - Scene.MinSize)
    val coercedY = to.y.coerceIn(topLeft.y + Scene.MinSize, height)
    return copy(topLeft = Point(x = coercedX, y = topLeft.y), bottomRight = Point(x = bottomRight.x, y = coercedY))
}

context (Size)
fun Rect.moveTopRightCornerWithinBounds(
    to: Point,
): Rect {
    val coercedX = to.x.coerceIn(topLeft.x + Scene.MinSize, width)
    val coercedY = to.y.coerceIn(0, bottomRight.y - Scene.MinSize)
    return copy(topLeft = Point(x = topLeft.x, y = coercedY), bottomRight = Point(x = coercedX, y = bottomRight.y))
}

context (Size)
fun Rect.moveBottomRightCornerWithinBounds(
    to: Point,
): Rect {
    val coercedX = to.x.coerceIn(topLeft.x + Scene.MinSize, width)
    val coercedY = to.y.coerceIn(topLeft.y + Scene.MinSize, height)
    return copy(bottomRight = Point(x = coercedX, y = coercedY))
}

context (Size)
fun Rect.moveRightEdgeWithinBounds(
    toX: Int,
): Rect = moveBottomRightCornerWithinBounds(to = Point(x = toX, y = bottomRight.y))

context (Size)
fun Rect.moveLeftEdgeWithinBounds(
    toX: Int,
): Rect = moveTopLeftCornerWithinBounds(to = Point(x = toX, y = topLeft.y))

context (Size)
fun Rect.moveTopEdgeWithinBounds(
    toY: Int,
): Rect = moveTopLeftCornerWithinBounds(to = Point(x = topLeft.x, y = toY))

context (Size)
fun Rect.moveBottomEdgeWithinBounds(
    toY: Int,
): Rect = moveBottomRightCornerWithinBounds(to = Point(x = bottomRight.x, y = toY))

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

fun Point.isOnTopLeftCornerOf(
    rect: Rect,
    tolerancePx: Int,
): Boolean = abs(x - rect.topLeft.x) <= tolerancePx && abs(y - rect.topLeft.y) <= tolerancePx

fun Point.isOnBottomLeftCornerOf(
    rect: Rect,
    tolerancePx: Int,
): Boolean = abs(x - rect.topLeft.x) <= tolerancePx && abs(y - rect.bottomRight.y) <= tolerancePx

fun Point.isOnTopRightCornerOf(
    rect: Rect,
    tolerancePx: Int,
): Boolean = abs(x - rect.bottomRight.x) <= tolerancePx && abs(y - rect.topLeft.y) <= tolerancePx

fun Point.isOnBottomRightCornerOf(
    rect: Rect,
    tolerancePx: Int,
): Boolean = abs(x - rect.bottomRight.x) <= tolerancePx && abs(y - rect.bottomRight.y) <= tolerancePx

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
