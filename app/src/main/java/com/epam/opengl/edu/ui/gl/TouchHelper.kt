package com.epam.opengl.edu.ui.gl

import android.graphics.PointF
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
    val cropOriginOffset: Offset = Offset(0f, 0f),
) {

    init {
        require(texture.width <= viewport.width && texture.height <= viewport.height) {
            "texture size can't be larger than viewport $texture > $viewport"
        }
    }

    companion object {
        const val TolerancePx = 30f
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
        bottomRight = Px(viewport.width.toFloat(), viewport.height.toFloat())
    )
        private set

    // fixme problems with zoom
    private var currentSpan = 1f
    private val previousInput = PointF()
    private var oldSpan = Float.NaN

    val zoom: Float
        get() = (currentSpan + viewport.width) / viewport.width.toFloat()

    fun onTouch(
        event: MotionEvent,
        isCropSelectionMode: Boolean,
    ) {
        userInput = Px(toTextureCoordinateX(event.x), toTextureCoordinateY(event.y))
        val offset = Offset(userInput.x - previousInput.x, previousInput.y - userInput.y)

        println("User input ${userInput.x}, ${userInput.y}")
        println("Offset ${offset.x}, ${offset.y}")

        previousInput.set(userInput.x, userInput.y)

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
    ) {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                when {
                    isCropSelectionMode && abs(userInput.x - rect.bottomRight.x) <= TolerancePx && userInput.y in rect.topLeft.y..rect.bottomRight.y -> {
                        rect =
                            rect.moveRightEdge(userInput.x.coerceIn(rect.topLeft.x + MinSize, viewport.width.toFloat()))
                    }

                    isCropSelectionMode && abs(userInput.x - rect.topLeft.x) <= TolerancePx && userInput.y in rect.topLeft.y..rect.bottomRight.y -> {
                        rect = rect.moveLeftEdge(userInput.x.coerceIn(0f, rect.bottomRight.x - MinSize))
                    }

                    isCropSelectionMode && abs(userInput.y - rect.topLeft.y) <= TolerancePx && userInput.x in rect.topLeft.x..rect.bottomRight.x -> {
                        rect = rect.moveTopEdge(userInput.y.coerceIn(0f, rect.bottomRight.y - MinSize))
                    }

                    isCropSelectionMode && abs(userInput.y - rect.bottomRight.y) <= TolerancePx && userInput.x in rect.topLeft.x..rect.bottomRight.x -> {
                        rect = rect.moveBottomEdge(
                            userInput.y.coerceIn(
                                rect.topLeft.y + MinSize,
                                viewport.height.toFloat()
                            )
                        )
                    }

                    isCropSelectionMode && userInput in rect -> {
                        // check crop selection won't exceed texture bounds
                        with(viewport) {
                            rect = rect.offsetToWithinBounds(
                                Offset(
                                    x = rect.topLeft.x + offset.x,
                                    y = rect.topLeft.y - offset.y
                                )
                            )
                        }
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
        x = rect.topLeft.x * (texture.width.toFloat() / viewport.width),
        y = -(viewport.height - rect.bottomRight.y) * (texture.height.toFloat() / viewport.height)
    )

val TouchHelper.ratio: Float
    get() = viewport.width.toFloat() / viewport.height

/** range is [0f .. 1f] */
fun TouchHelper.normalizedX(
    xOnTexture: Float,
): Float = xOnTexture * (texture.width.toFloat() / viewport.width) / viewport.width.toFloat()

/** range is [1f - textureHeight / viewportHeight .. 1f] */
fun TouchHelper.normalizedY(
    yOnTexture: Float,
): Float =
    1f - texture.height.toFloat() / viewport.height + yOnTexture * (texture.height.toFloat() / viewport.height) / viewport.height.toFloat()


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
    get() = maxOffsetDistanceYPointsBeforeEdgeVisible * textureOffset.y / (maxOffsetDistanceYBeforeEdgeVisiblePx.takeIf { it != 0f }
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
    2 * ((viewport.height - viewportY) / viewport.height) / zoom

/**
 * Formula x = (viewport * (consumed_window_points + maxOffsetDistanceXPointsBeforeEdgeVisible + textureOffsetXPoints)) / 2
 */
private fun TouchHelper.toTextureCoordinateX(
    viewportX: Float,
): Float = toTextureCoordinatesPx(consumedOffsetXPoints + consumedPointsX(viewportX), viewport.width)

/**
 * Formula x = (viewport * (consumed_window_points + maxOffsetDistanceYPointsBeforeEdgeVisible + textureOffsetYPoints)) / 2
 */
private fun TouchHelper.toTextureCoordinateY(
    viewportY: Float,
): Float = toTextureCoordinatesPx(consumedOffsetYPoints + consumedPointsY(viewportY), viewport.height)

/**
 * Converts [point] in range -1..1 to texture coordinates in pixels in range [0..viewport]
 */
private fun toTextureCoordinatesPx(
    point: Float,
    viewport: Int,
): Float = (viewport * point) / 2

@Suppress("NOTHING_TO_INLINE")
private inline operator fun Rect.contains(
    point: Px,
): Boolean = point.x in topLeft.x..bottomRight.x && point.y in topLeft.y..bottomRight.y