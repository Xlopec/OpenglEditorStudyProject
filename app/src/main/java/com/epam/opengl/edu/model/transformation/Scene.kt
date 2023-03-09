package com.epam.opengl.edu.model.transformation

import android.view.MotionEvent
import com.epam.opengl.edu.model.geometry.NormalizedPoint
import com.epam.opengl.edu.model.geometry.Offset
import com.epam.opengl.edu.model.geometry.Point
import com.epam.opengl.edu.model.geometry.Rect
import com.epam.opengl.edu.model.geometry.SceneOffset
import com.epam.opengl.edu.model.geometry.ScenePoint
import com.epam.opengl.edu.model.geometry.Size
import com.epam.opengl.edu.model.geometry.height
import com.epam.opengl.edu.model.geometry.isOnBottomEdgeOf
import com.epam.opengl.edu.model.geometry.isOnLeftEdgeOf
import com.epam.opengl.edu.model.geometry.isOnRightEdgeOf
import com.epam.opengl.edu.model.geometry.isOnTopEdgeOf
import com.epam.opengl.edu.model.geometry.minus
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
    /**
     * Current window size
     */
    val window: Size,
) : Transformation {

    companion object {
        const val TolerancePx = 30
        const val MinSize = TolerancePx * 3
    }

    /**
     * Current scene offset
     */
    var sceneOffset: SceneOffset = SceneOffset()
        private set

    /**
     * User input in image coordinate system
     */
    var imagePoint = Point(0, 0)
        private set

    private var previousScenePoint = ScenePoint(0f, 0f)

    /**
     * Cropping rect coordinates in viewport coordinate system. The latter means none of the vertices can be located outside viewport
     */
    var selection = Rect(
        topLeft = Point(0, 0),
        bottomRight = Point(image.width, image.height)
    )
        private set

    private var previousImagePoint = Point(0, 0)

    fun onTouch(
        event: MotionEvent,
        isCropSelectionMode: Boolean,
    ) {
        val scenePoint = ScenePoint(event.x, event.y)
        val windowOffsetDelta = SceneOffset(scenePoint, previousScenePoint)
        previousScenePoint = scenePoint

        imagePoint = (event.toScenePoint() - sceneOffset).toImagePoint()
        val imageOffsetDelta = Offset(imagePoint.x - previousImagePoint.x, imagePoint.y - previousImagePoint.y)
        previousImagePoint = imagePoint

        if (event.pointerCount == 1) {
            handleMovement(event.action, isCropSelectionMode, imageOffsetDelta, windowOffsetDelta, imagePoint)
        }
    }

    private fun handleMovement(
        action: Int,
        isCropSelectionMode: Boolean,
        imageOffsetDelta: Offset,
        sceneOffsetDelta: SceneOffset,
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
                        selection = selection.offsetByWithinBounds(imageOffsetDelta)
                    }

                    else -> {
                        sceneOffset += sceneOffsetDelta
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
        private fun ScenePoint.toImagePoint(): Point {
    return if (image.isPortrait) {
        val viewport2WindowHeight = image.height.toFloat() / window.height
        // how many times image was stretched to fit the window height
        val scaleFactor = 1f / viewport2WindowHeight
        // convert image width to width that it occupies inside window,
        // it might be bigger than window width itself
        val scaledWidthInWindow = scaleFactor * image.width
        val offsetX = window.width - scaledWidthInWindow
        val halfOffsetPx = offsetX * 0.5f
        val xWindow = x - halfOffsetPx
        val xImage = (viewport2WindowHeight * xWindow).roundToInt()
        val yImage = (y * viewport2WindowHeight).roundToInt()

        Point(xImage, yImage)
    } else {
        // in this case the image occupies window.ratio pixels,
        // (1f - window.ratio) won't be drawn and go to offset.
        // 1f == image.width
        // window.ratio == visible image on screen px
        val onscreenImageWidthPx = window.ratio * image.width
        val offscreenImageWidthPx = image.width - onscreenImageWidthPx
        val visible2WindowWidth = onscreenImageWidthPx / window.width
        val imageX = (x * visible2WindowWidth + 0.5f * offscreenImageWidthPx).roundToInt()

        val scaleFactor = 1f / visible2WindowWidth
        val scaledHeightInWindow = scaleFactor * image.height
        val offsetY = window.height - scaledHeightInWindow
        val yWindow = y - 0.5f * offsetY
        val yImage = (visible2WindowWidth * yWindow).roundToInt()

        Point(imageX, yImage)
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun MotionEvent.toScenePoint() = ScenePoint(x, y)

@Suppress("NOTHING_TO_INLINE")
private inline operator fun Rect.contains(
    imagePoint: Point,
): Boolean = imagePoint.x in topLeft.x..bottomRight.x && imagePoint.y in topLeft.y..bottomRight.y

inline val Size.isPortrait: Boolean
    get() = width < height

inline val Size.ratio: Float
    get() = width.toFloat() / height