package com.epam.opengl.edu.ui.gl

import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

class TouchHelper(
    /**
     * Viewport size
     */
    val viewport: Size = Size(0, 0),
    /**
     * Current texture size, can't exceed [viewport] size
     */
    val texture: Size = viewport,
    /**
     * Texture offset accumulated relative to the left side of crop rect.
     * This is needed to account invisible offset accumulated after each crop because
     * we need to calculate offset in coordinate system of the original texture
     */
    val cropOriginOffset: Offset = Offset(0, 0),
) {

    init {
        require(texture.width <= viewport.width && texture.height <= viewport.height) {
            "texture size can't be larger than viewport $texture > $viewport"
        }
    }

    companion object {
        const val TolerancePx = 30
        const val MinSize = TolerancePx * 3
    }

    /**
     * Texture offset in viewport coordinate system
     */
    var textureOffset = Offset()
        private set

    /**
     * User input in texture coordinate system
     */
    var userInput = Px()
        private set

    /**
     * Cropping rect coordinates in viewport coordinate system. The latter means none of the vertices can be located outside viewport
     */
    var rect = Rect(
        topLeft = Px(),
        bottomRight = Px(viewport.width, viewport.height)
    )
        private set

    // fixme problems with zoom
    private var currentSpan = 1f
    private var previousInput = Px()
    private var oldSpan = Float.NaN

    val zoom: Float
        get() = (currentSpan + viewport.width) / viewport.width.toFloat()

    fun onTouch(
        event: MotionEvent,
        isCropSelectionMode: Boolean,
    ) {
        userInput = Px(toTextureCoordinateX(event.x), toTextureCoordinateY(event.y))
        val offset = Offset(userInput.x - previousInput.x, userInput.y - previousInput.y)
        previousInput = userInput

        if (event.pointerCount > 1) {
            handleZoom(event)
        } else {
            handleMovement(event, isCropSelectionMode, offset)
        }
    }

    private fun handleZoom(event: MotionEvent) {
        var focalX = 0f
        var focalY = 0f

        for (i in 0 until event.pointerCount) {
            focalX += event.getX(i)
            focalY += event.getY(i)
        }

        focalX /= event.pointerCount
        focalY /= event.pointerCount

        var devSumX = 0f
        var devSumY = 0f

        for (i in 0 until event.pointerCount) {
            devSumX += abs(focalX - event.getX(i))
            devSumY += abs(focalY - event.getY(i))
        }

        devSumX /= event.pointerCount
        devSumY /= event.pointerCount

        val span = hypot(devSumX, devSumY)

        if (oldSpan.isNaN()) {
            oldSpan = span
        }

        if (event.action == MotionEvent.ACTION_MOVE) {
            currentSpan += span - oldSpan
            oldSpan = span
        }
    }

    private fun handleMovement(
        event: MotionEvent,
        isCropSelectionMode: Boolean,
        offset: Offset,
    ) = with(viewport) {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                when {
                    isCropSelectionMode && userInput.isOnRightEdgeOf(rect, TolerancePx) -> {
                        rect = rect.moveRightEdgeWithinBounds(userInput.x)
                    }

                    isCropSelectionMode && userInput.isOnLeftEdgeOf(rect, TolerancePx) -> {
                        rect = rect.moveLeftEdgeWithinBounds(userInput.x)
                    }

                    isCropSelectionMode && userInput.isOnTopEdgeOf(rect, TolerancePx) -> {
                        rect = rect.moveTopEdgeWithinBounds(userInput.y)
                    }

                    isCropSelectionMode && userInput.isOnBottomEdgeOf(rect, TolerancePx) -> {
                        rect = rect.moveBottomEdgeWithinBounds(userInput.y)
                    }

                    isCropSelectionMode && userInput in rect -> {
                        rect = rect.offsetByWithinBounds(offset)
                    }

                    else -> {
                        textureOffset += offset
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                oldSpan = Float.NaN
            }
        }
    }

}

fun TouchHelper.onReset() = TouchHelper(viewport = viewport)

fun TouchHelper.onCropped(): TouchHelper = TouchHelper(
    viewport = viewport,
    texture = croppedTextureSize,
    cropOriginOffset = cropOriginOffset + consumedTextureOffset
)

/**
 * Texture size given current [TouchHelper.cropRect] and [TouchHelper.viewport]
 */
inline val TouchHelper.croppedTextureSize: Size
    // new width = width * (selection.width / viewportWidth)
    get() = Size(
        (texture.width * (rect.size.width.toFloat() / viewport.width)).roundToInt(),
        (texture.height * (rect.size.height.toFloat() / viewport.height)).roundToInt()
    )

private inline val TouchHelper.consumedTextureOffset: Offset
    get() = Offset(
        x = (rect.topLeft.x * texture.width.toFloat() / viewport.width).roundToInt(),
        y = (rect.topLeft.y * texture.height.toFloat() / viewport.height).roundToInt()
    )

val TouchHelper.ratio: Float
    get() = viewport.width.toFloat() / viewport.height

/**
 * Puts [xOnTexture] in range [0f .. 1f]
 * */
fun TouchHelper.normalizedX(
    xOnTexture: Int,
): Float = xOnTexture * (texture.width.toFloat() / viewport.width) / viewport.width.toFloat()

/**
 * Puts [yOnTexture] in range [0 .. 1f]
 * */
fun TouchHelper.normalizedY(
    yOnTexture: Int,
): Float = yOnTexture * (texture.height.toFloat() / viewport.height) / viewport.height.toFloat()

inline val TouchHelper.maxOffsetDistanceXPointsBeforeEdgeVisible: Float
    get() = 1 - ratio + (2 * ratio - 2 * ratio / zoom) / 2

inline val TouchHelper.maxOffsetDistanceYPointsBeforeEdgeVisible: Float
    get() = (2f - 2f / zoom) / 2f

inline val TouchHelper.maxOffsetDistanceYBeforeEdgeVisiblePx: Float
    get() = maxOffsetDistanceYPointsBeforeEdgeVisible * viewport.height

inline val TouchHelper.maxOffsetDistanceBeforeEdgeVisiblePx: Float
    get() = maxOffsetDistanceXPointsBeforeEdgeVisible * viewport.width

inline val TouchHelper.textureOffsetXPoints: Float
    get() = maxOffsetDistanceXPointsBeforeEdgeVisible * -textureOffset.x / (maxOffsetDistanceBeforeEdgeVisiblePx.takeIf { it != 0f }
        ?: viewport.width.toFloat())

inline val TouchHelper.consumedOffsetXPoints: Float
    get() = maxOffsetDistanceXPointsBeforeEdgeVisible + textureOffsetXPoints

inline val TouchHelper.textureOffsetYPoints: Float
    get() = maxOffsetDistanceYPointsBeforeEdgeVisible * -textureOffset.y / (maxOffsetDistanceYBeforeEdgeVisiblePx.takeIf { it != 0f }
        ?: viewport.height.toFloat())

inline val TouchHelper.consumedOffsetYPoints: Float
    get() = maxOffsetDistanceYPointsBeforeEdgeVisible + textureOffsetYPoints

/**
 * Returns how many points were consumed by viewport when x coordinate is [viewportX]
 * Return value in range 0..2
 *
 * Formula - consumedPoints = ((x / screen_viewport_width) * 2 * ratio) / zoom
 */
private fun TouchHelper.consumedPointsX(viewportX: Float): Float = 2 * ratio * (viewportX / viewport.width) / zoom

/**
 * Returns how many points were consumed by viewport when y coordinate is [viewportY]
 * Return value in range 0..2
 *
 * Formula - consumedPoints = ((y / screen_viewport_width) * 2) / zoom
 *
 * viewport.height - viewportY - inverted y coordinate
 */
private fun TouchHelper.consumedPointsY(viewportY: Float): Float =
    2 * ((/*viewport.height - */viewportY) / viewport.height) / zoom

/**
 * Formula x = (viewport * (consumed_window_points + maxOffsetDistanceXPointsBeforeEdgeVisible + textureOffsetXPoints)) / 2
 */
private fun TouchHelper.toTextureCoordinateX(
    viewportX: Float,
): Int = toTextureCoordinatesPx(consumedOffsetXPoints + consumedPointsX(viewportX), viewport.width)

/**
 * Formula x = (viewport * (consumed_window_points + maxOffsetDistanceYPointsBeforeEdgeVisible + textureOffsetYPoints)) / 2
 */
private fun TouchHelper.toTextureCoordinateY(
    viewportY: Float,
): Int = toTextureCoordinatesPx(consumedOffsetYPoints + consumedPointsY(viewportY), viewport.height)

/**
 * Converts [point] in range -1..1 to texture coordinates in pixels in range [0..viewport]
 */
private fun toTextureCoordinatesPx(
    point: Float,
    viewport: Int,
): Int = (viewport * point / 2).roundToInt()

@Suppress("NOTHING_TO_INLINE")
private inline operator fun Rect.contains(
    point: Px,
): Boolean = point.x in topLeft.x..bottomRight.x && point.y in topLeft.y..bottomRight.y