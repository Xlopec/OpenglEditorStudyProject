package com.epam.opengl.edu.model.transformation

import android.view.MotionEvent
import com.epam.opengl.edu.model.geometry.NormalizedPoint
import com.epam.opengl.edu.model.geometry.Offset
import com.epam.opengl.edu.model.geometry.Point
import com.epam.opengl.edu.model.geometry.Rect
import com.epam.opengl.edu.model.geometry.Size
import com.epam.opengl.edu.model.geometry.component1
import com.epam.opengl.edu.model.geometry.component2
import com.epam.opengl.edu.model.geometry.height
import com.epam.opengl.edu.model.geometry.isOnBottomEdgeOf
import com.epam.opengl.edu.model.geometry.isOnLeftEdgeOf
import com.epam.opengl.edu.model.geometry.isOnRightEdgeOf
import com.epam.opengl.edu.model.geometry.isOnTopEdgeOf
import com.epam.opengl.edu.model.geometry.moveBottomEdgeWithinBounds
import com.epam.opengl.edu.model.geometry.moveLeftEdgeWithinBounds
import com.epam.opengl.edu.model.geometry.moveRightEdgeWithinBounds
import com.epam.opengl.edu.model.geometry.moveTopEdgeWithinBounds
import com.epam.opengl.edu.model.geometry.offsetByWithinBounds
import com.epam.opengl.edu.model.geometry.plus
import com.epam.opengl.edu.model.geometry.width
import com.epam.opengl.edu.model.geometry.x
import com.epam.opengl.edu.model.geometry.y
import kotlin.math.roundToInt

/**
 * Represents editor view scene
 */
class Scene(
    /**
     * Current image size
     */
    val image: Size,
    val window: Size,
) : Transformation {

    companion object {
        const val TolerancePx = 30
        const val MinSize = TolerancePx * 3
    }

    var sceneOffset = Offset()
        private set

    /**
     * User input in image coordinate system
     */
    var userInput = Point(0, 0)
        private set

    private var previousRawPoint = Point(0, 0)

    /**
     * Cropping rect coordinates in viewport coordinate system. The latter means none of the vertices can be located outside viewport
     */
    var selection = Rect(
        topLeft = Point(0, 0),
        bottomRight = Point(image.width, image.height)
    )
        private set

    // fixme problems with zoom
    private var previousInput = Point(0, 0)

    fun onTouch(
        event: MotionEvent,
        isCropSelectionMode: Boolean,
    ) {
        val rawPoint = Point(event.x.roundToInt(), event.y.roundToInt())
        val rawOffsetDelta = Offset(rawPoint.x - previousRawPoint.x, rawPoint.y - previousRawPoint.y)
        previousRawPoint = rawPoint

        userInput = event.toImagePoint(sceneOffset)
        println("Input $userInput")
        val offset = Offset(userInput.x - previousInput.x, userInput.y - previousInput.y)
        previousInput = userInput

        if (event.pointerCount > 1) {
            // not implemented yet
            return
        } else {
            handleMovement(event.action, isCropSelectionMode, offset, rawOffsetDelta, userInput)
        }
    }

    private fun handleMovement(
        action: Int,
        isCropSelectionMode: Boolean,
        imageOffset: Offset,
        sceneOffset: Offset,
        userInput: Point,
    ) = with(image) {
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
                        selection = selection.offsetByWithinBounds(imageOffset)
                    }

                    else -> {
                        this@Scene.sceneOffset += sceneOffset
                    }
                }
            }
        }
    }

}

fun Scene.onCropped(): Scene {
    return Scene(
        image = croppedImageSize,
        window = window
    )
}

/**
 * Image size given current [Scene.selection] and [Scene.image]
 */
inline val Scene.croppedImageSize: Size
    get() = Size(
        image.width - (selection.topLeft.x + image.width - selection.bottomRight.x),
        image.height - (selection.topLeft.y + image.height - selection.bottomRight.y)
    )

context (Scene)
        @Suppress("NOTHING_TO_INLINE")
        inline fun Point.toNormalized(): NormalizedPoint =
    NormalizedPoint.safeOf(
        // Puts x in range [0 .. 1f]
        x = x / image.width.toFloat(),
        // Puts y in range [0 .. 1f]
        y = y / image.height.toFloat()
    )

context (Scene)
        private fun MotionEvent.toImagePoint(sceneOffset: Offset): Point {
    val (osX, osY) = sceneOffset

    return if (image.isPortrait) {
        val viewport2WindowHeight = image.height.toFloat() / window.height
        // how many times image was stretched to fit the window height
        val scaleFactor = 1f / viewport2WindowHeight
        // convert image width to width that it occupies inside window,
        // it might be bigger than window width itself
        val scaledWidthInWindow = scaleFactor * image.width
        val offsetX = window.width - scaledWidthInWindow
        val halfOffsetPx = offsetX * 0.5f
        val xWindow = x - halfOffsetPx - osX
        val xImage = (viewport2WindowHeight * xWindow).roundToInt()
        val yImage = ((y - osY) * viewport2WindowHeight).roundToInt()

        Point(xImage, yImage)
    } else {
        // in this case the image occupies window.ratio pixels,
        // (1f - window.ratio) won't be drawn and go to offset.
        // 1f == image.width
        // window.ratio == visible image on screen px
        val onscreenImageWidthPx = window.ratio * image.width
        val offscreenImageWidthPx = image.width - onscreenImageWidthPx
        val visible2WindowWidth = onscreenImageWidthPx / window.width
        val imageX = ((x - osX) * visible2WindowWidth + 0.5f * offscreenImageWidthPx).roundToInt()

        val scaleFactor = 1f / visible2WindowWidth
        val scaledHeightInWindow = scaleFactor * image.height
        val offsetY = window.height - scaledHeightInWindow
        val yWindow = (y - osY) - 0.5f * offsetY
        val yImage = (visible2WindowWidth * yWindow).roundToInt()

        Point(imageX, yImage)
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline operator fun Rect.contains(
    point: Point,
): Boolean = point.x in topLeft.x..bottomRight.x && point.y in topLeft.y..bottomRight.y

inline val Size.isPortrait: Boolean
    get() = width < height

inline val Size.ratio: Float
    get() = width.toFloat() / height