package com.epam.opengl.edu.model.transformation

import android.view.MotionEvent
import com.epam.opengl.edu.model.geometry.NormalizedPoint
import com.epam.opengl.edu.model.geometry.Offset
import com.epam.opengl.edu.model.geometry.Point
import com.epam.opengl.edu.model.geometry.Rect
import com.epam.opengl.edu.model.geometry.Size
import com.epam.opengl.edu.model.geometry.height
import com.epam.opengl.edu.model.geometry.isOnBottomEdgeOf
import com.epam.opengl.edu.model.geometry.isOnLeftEdgeOf
import com.epam.opengl.edu.model.geometry.isOnRightEdgeOf
import com.epam.opengl.edu.model.geometry.isOnTopEdgeOf
import com.epam.opengl.edu.model.geometry.isPortrait
import com.epam.opengl.edu.model.geometry.moveBottomEdgeWithinBounds
import com.epam.opengl.edu.model.geometry.moveLeftEdgeWithinBounds
import com.epam.opengl.edu.model.geometry.moveRightEdgeWithinBounds
import com.epam.opengl.edu.model.geometry.moveTopEdgeWithinBounds
import com.epam.opengl.edu.model.geometry.offsetByWithinBounds
import com.epam.opengl.edu.model.geometry.plus
import com.epam.opengl.edu.model.geometry.width
import com.epam.opengl.edu.model.geometry.x
import com.epam.opengl.edu.model.geometry.y
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Represents editor view scene
 */
class Scene(
    /**
     * Current texture size
     */
    val texture: Size,
    /**
     * Texture offset accumulated relative to the left side of crop rect.
     * This is needed to account invisible offset accumulated after each crop because
     * we need to calculate offset in coordinate system of the original texture
     */
    val cropOriginOffset: Offset = Offset(0, 0),
) : Transformation {

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
    var userInput = Point(0, 0)
        private set

    /**
     * Cropping rect coordinates in viewport coordinate system. The latter means none of the vertices can be located outside viewport
     */
    var selection = Rect(
        topLeft = Point(0, 0),
        bottomRight = Point(texture.width, texture.height)
    )
        private set

    // fixme problems with zoom
    private var currentSpan = 1f
    private var previousInput = Point(0, 0)
    private var oldSpan = Float.NaN

    val zoom: Float
        get() = (currentSpan + texture.width) / texture.width.toFloat()

    fun onTouch(
        event: MotionEvent,
        isCropSelectionMode: Boolean,
    ) {
        userInput = event.toTexturePoint()
        println("Input $userInput")
        val offset = Offset(userInput.x - previousInput.x, userInput.y - previousInput.y)
        previousInput = userInput

        if (event.pointerCount > 1) {
            handleZoom(event)
        } else {
            handleMovement(event.action, isCropSelectionMode, offset)
        }
    }

    private fun handleZoom(
        event: MotionEvent,
    ) {
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
        action: Int,
        isCropSelectionMode: Boolean,
        offset: Offset,
    ) = with(texture) {
        when (action) {
            MotionEvent.ACTION_MOVE -> {
                when {
                    isCropSelectionMode && userInput.isOnRightEdgeOf(selection, TolerancePx) -> {
                        selection = selection.moveRightEdgeWithinBounds(userInput.x)
                    }

                    isCropSelectionMode && userInput.isOnLeftEdgeOf(selection, TolerancePx) -> {
                        selection = selection.moveLeftEdgeWithinBounds(userInput.x)
                    }

                    isCropSelectionMode && userInput.isOnTopEdgeOf(selection, TolerancePx) -> {
                        selection = selection.moveTopEdgeWithinBounds(userInput.y)
                    }

                    isCropSelectionMode && userInput.isOnBottomEdgeOf(selection, TolerancePx) -> {
                        selection = selection.moveBottomEdgeWithinBounds(userInput.y)
                    }

                    isCropSelectionMode && userInput in selection -> {
                        selection = selection.offsetByWithinBounds(offset)
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

fun Scene.onCropped(): Scene {
    return Scene(
        texture = croppedTextureSize,
    )
}

/**
 * Texture size given current [Scene.selection] and [Scene.texture]
 */
inline val Scene.croppedTextureSize: Size
    get() = Size(
        texture.width - (selection.topLeft.x + texture.width - selection.bottomRight.x),
        texture.height - (selection.topLeft.y + texture.height - selection.bottomRight.y)
    )

private inline val Scene.consumedTextureOffset: Offset
    get() = Offset(
        x = (selection.topLeft.x * texture.width.toFloat() / texture.width).roundToInt(),
        y = (selection.topLeft.y * texture.height.toFloat() / texture.height).roundToInt()
    )

val Scene.textureRatio: Float
    get() = texture.width.toFloat() / texture.height

val Scene.viewportRatio: Float
    get() = texture.width.toFloat() / texture.height

context (Scene)
        @Suppress("NOTHING_TO_INLINE")
        inline fun Point.toNormalized(): NormalizedPoint =
    NormalizedPoint.safeOf(
        // Puts x in range [0 .. 1f]
        x = x / texture.width.toFloat(),
        // Puts y in range [0 .. 1f]
        y = y / texture.height.toFloat()
    )


inline val Scene.maxOffsetDistanceXPointsBeforeEdgeVisible: Float
    get() = 1 - viewportRatio + (2 * viewportRatio - 2 * viewportRatio / zoom) / 2

inline val Scene.maxOffsetDistanceYPointsBeforeEdgeVisible: Float
    get() = (2f - 2f / zoom) / 2f

inline val Scene.maxOffsetDistanceYBeforeEdgeVisiblePx: Float
    get() = maxOffsetDistanceYPointsBeforeEdgeVisible * texture.height

inline val Scene.maxOffsetDistanceBeforeEdgeVisiblePx: Float
    get() = maxOffsetDistanceXPointsBeforeEdgeVisible * texture.width

inline val Scene.textureOffsetXPoints: Float
    get() = maxOffsetDistanceXPointsBeforeEdgeVisible * -textureOffset.x / (maxOffsetDistanceBeforeEdgeVisiblePx.takeIf { it != 0f }
        ?: texture.width.toFloat())

inline val Scene.consumedOffsetXPoints: Float
    get() = maxOffsetDistanceXPointsBeforeEdgeVisible + textureOffsetXPoints

inline val Scene.textureOffsetYPoints: Float
    get() = maxOffsetDistanceYPointsBeforeEdgeVisible * -textureOffset.y / (maxOffsetDistanceYBeforeEdgeVisiblePx.takeIf { it != 0f }
        ?: texture.height.toFloat())

inline val Scene.consumedOffsetYPoints: Float
    get() = maxOffsetDistanceYPointsBeforeEdgeVisible + textureOffsetYPoints

/**
 * Formula x = (viewport * (consumed_window_points + maxOffsetDistanceXPointsBeforeEdgeVisible + textureOffsetXPoints)) / 2
 * Formula y = (viewport * (consumed_window_points + maxOffsetDistanceYPointsBeforeEdgeVisible + textureOffsetYPoints)) / 2
 */
context (Scene)
        private fun MotionEvent.toTexturePoint(): Point {

    return if (texture.isPortrait) {
        val viewport2WindowHeight = texture.height.toFloat() / window.height
        // how many times texture was stretched to fit the window height
        val scaleFactor = 1f / viewport2WindowHeight
        // convert texture width to width that it occupies inside window,
        // it might be bigger than window width itself
        val scaledWidthInWindow = scaleFactor * texture.width
        val offsetX = window.width - scaledWidthInWindow
        val halfOffsetPx = offsetX * 0.5f
        val xWindow = x - halfOffsetPx
        val xTexture = (viewport2WindowHeight * xWindow).roundToInt()
        val yTexture = (y * viewport2WindowHeight).roundToInt()

        Point(xTexture, yTexture)
    } else {
        // in this case the texture occupies window.ratio pixels,
        // (1f - window.ratio) won't be drawn and go to offset.
        // 1f == texture.width
        // window.ratio == visible texture on screen px
        val onscreenTextureWidthPx = windowRatio * texture.width
        val offscreenTextureWidthPx = texture.width - onscreenTextureWidthPx
        val visible2WindowWidth = onscreenTextureWidthPx / window.width
        val textureX = (x * visible2WindowWidth + 0.5f * offscreenTextureWidthPx).roundToInt()

        val scaleFactor = 1f / visible2WindowWidth
        val scaledHeightInWindow = scaleFactor * texture.height
        val offsetY = window.height - scaledHeightInWindow
        val yWindow = y - 0.5f * offsetY
        val yTexture = (visible2WindowWidth * yWindow).roundToInt()

        Point(textureX, yTexture)
    }
}

val window = Size(
    width = 1080,
    height = 1584 //2054
)

val windowRatio get() = window.width.toFloat() / window.height.toFloat()

/**
 * Returns how many points were consumed by viewport when x coordinate is [viewportX]
 * Return value in range 0..2
 *
 * Formula - consumedPoints = ((x / screen_viewport_width) * 2 * ratio) / zoom
 */
private fun Scene.consumedPointsX(viewportX: Float): Float = 2 * viewportRatio * (viewportX / window.width) / zoom

/**
 * Returns how many points were consumed by viewport when y coordinate is [viewportY]
 * Return value in range 0..2
 *
 * Formula - consumedPoints = ((y / screen_viewport_width) * 2) / zoom
 **/
private fun Scene.consumedPointsY(viewportY: Float): Float =
    2 * (viewportY / window.height) / zoom

/**
 * Converts [point] in range -1..1 to texture coordinates in pixels in range [0..viewport]
 */
private fun toTextureCoordinatesPx(
    point: Float,
    viewport: Int,
): Float = (viewport * point / 2)//.roundToInt()

@Suppress("NOTHING_TO_INLINE")
private inline operator fun Rect.contains(
    point: Point,
): Boolean = point.x in topLeft.x..bottomRight.x && point.y in topLeft.y..bottomRight.y