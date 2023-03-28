package io.github.xlopec.opengl.edu.model.transformation

import android.view.MotionEvent
import io.github.xlopec.opengl.edu.model.geometry.*
import kotlin.math.roundToInt

/**
 * Represents editor view scene
 */
class Scene(
    /**
     * Current image size
     */
    val imageSize: Size,
    /**
     * Current window size
     */
    val windowSize: Size,
    /**
     * Current visible image viewport in terms of original image. Can't exceed size of the original image.
     */
    val viewport: Rect = Rect(Point(0, 0), Point(imageSize.width, imageSize.height)),
) : Transformation {

    companion object {
        const val TolerancePx = 30
        const val MinSize = TolerancePx * 3
    }

    init {
        require(viewport.size.width <= imageSize.width)
        require(viewport.size.height <= imageSize.height)
    }

    /**
     * Current scene offset
     */
    var sceneOffset: SceneOffset = SceneOffset(0f, 0f)
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
        bottomRight = Point(imageSize.width, imageSize.height)
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
    ) = with(imageSize) {
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

fun Scene.onCropped(): Scene = Scene(
    imageSize = selection.size,
    windowSize = windowSize,
    viewport = Rect(
        topLeft = viewport.topLeft + leftTopImageOffset.toPoint(),
        bottomRight = viewport.bottomRight - rightBottomImageOffset.toPoint()
    ),
)

val Scene.leftTopImageOffset: Offset
    get() = Offset(selection.topLeft.x, selection.topLeft.y)

val Scene.rightBottomImageOffset: Offset
    get() = Offset(imageSize.width - selection.bottomRight.x, imageSize.height - selection.bottomRight.y)

context (Scene)
fun Offset.toSceneOffset(): SceneOffset {
    val widthScaleFactor = windowSize.width / imageSize.width.toFloat()
    val heightScaleFactor = windowSize.height / imageSize.height.toFloat()
    return SceneOffset(x * widthScaleFactor, y * heightScaleFactor)
}

context (Scene)
        @Suppress("NOTHING_TO_INLINE")
        inline fun Point.toGlPoint(): GlPoint = GlPoint.fromPoint(this, imageSize)

context (Scene)
        private fun ScenePoint.toImagePoint(): Point {
    return if (imageSize.isPortrait) {
        val viewport2WindowHeight = imageSize.height.toFloat() / windowSize.height
        // how many times image was stretched to fit the window height
        val scaleFactor = 1f / viewport2WindowHeight
        // convert image width to width that it occupies inside window,
        // it might be bigger than window width itself
        val scaledWidthInWindow = scaleFactor * imageSize.width
        val offsetX = windowSize.width - scaledWidthInWindow
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
        val onscreenImageWidthPx = windowSize.ratio * imageSize.width
        val offscreenImageWidthPx = imageSize.width - onscreenImageWidthPx
        val visible2WindowWidth = onscreenImageWidthPx / windowSize.width
        val imageX = (x * visible2WindowWidth + 0.5f * offscreenImageWidthPx).roundToInt()

        val scaleFactor = 1f / visible2WindowWidth
        val scaledHeightInWindow = scaleFactor * imageSize.height
        val offsetY = windowSize.height - scaledHeightInWindow
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