package com.epam.opengl.edu.ui.gl

import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

class TouchHelper(
    viewport: Size = Size(0, 0),
) {

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
     * Texture offset accumulated relative to the left side of crop rect.
     * This is needed to account invisible offset accumulated after each crop because
     * we need to calculate offset in coordinate system of the original texture
     */
    var cropOriginOffset = Offset()
        private set

    /**
     * User input in texture coordinate system
     */
    val userInput = PointF()

    /**
     * Cropping rect coordinates in viewport coordinate system. The latter means none of the vertices can be located outside viewport
     */
    val cropRect = RectF(0f, 0f, viewport.width.toFloat(), viewport.height.toFloat())

    /**
     * Current texture size, can't exceed [viewport] size
     */
    var texture = viewport
        private set

    /**
     * Viewport size
     */
    var viewport = viewport
        private set

    var currentSpan = 1f
        private set
    var initialSpan = 0f
    var currentSpanX = 0f
    private val previousInput = PointF()
    private var oldSpan = Float.NaN

    val zoom: Float
        get() = 2f//if (initialSpan == 0f || currentSpan == 0f) 1f else currentSpan / initialSpan

    // todo recreate
    fun reset() {
        currentSpan = 0f
        oldSpan = Float.NaN
        previousInput.set(0f, 0f)
    }

    // todo recreate
    fun resetCropSelection() {
        cropRect.set(0f, 0f, viewport.width.toFloat(), viewport.height.toFloat())
    }

    // todo recreate
    fun onSurfaceChanged(
        width: Int,
        height: Int,
    ) {
        viewport = Size(width, height)
        texture = viewport
        cropOriginOffset = Offset(0f, 0f)
        resetCropSelection()
    }

    // todo recreate
    fun onTexturesCropped() {
        cropOriginOffset += consumedTextureOffset
        texture = croppedTextureSize
        resetCropSelection()
    }

    fun onTouch(
        event: MotionEvent,
        isCropSelectionMode: Boolean,
    ) {
        println("event x ${event.x}")
        val offset = if (previousInput.x == 0f && previousInput.y == 0f) {
            Offset(0f, 0f)
        } else {
            Offset(event.x - previousInput.x, event.y - previousInput.y)
        }

        userInput.set(toTextureCoordinateX(event.x), toTextureCoordinateY(event.y))
        previousInput.set(event.x, event.y)

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
            initialSpan = span//event.x
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
                    isCropSelectionMode && abs(userInput.x - cropRect.right) <= TolerancePx && userInput.y in cropRect.top..cropRect.bottom -> {
                        cropRect.right = userInput.x.coerceAtLeast(cropRect.left + MinSize)
                    }

                    isCropSelectionMode && abs(userInput.x - cropRect.left) <= TolerancePx && userInput.y in cropRect.top..cropRect.bottom -> {
                        cropRect.left = userInput.x.coerceAtMost(cropRect.right - MinSize)
                    }

                    isCropSelectionMode && abs(userInput.y - cropRect.top) <= TolerancePx && userInput.x in cropRect.left..cropRect.right -> {
                        cropRect.top = userInput.y.coerceAtMost(cropRect.bottom - MinSize)
                    }

                    isCropSelectionMode && abs(userInput.y - cropRect.bottom) <= TolerancePx && userInput.x in cropRect.left..cropRect.right -> {
                        cropRect.bottom = userInput.y.coerceAtLeast(cropRect.top + MinSize)
                    }

                    isCropSelectionMode && userInput in cropRect -> {
                        // check crop selection won't exceed texture bounds
                        cropRect.offsetTo(
                            (cropRect.left + offset.x / 2).coerceIn(0f, viewport.width - cropRect.width()),
                            (cropRect.top - offset.y).coerceIn(0f, viewport.height - cropRect.height())
                        )
                    }

                    else -> {
                        textureOffset += offset
                        println("offset ${textureOffset.x}")
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                oldSpan = Float.NaN
            }
        }
    }

}

/**
 * Texture size given current [TouchHelper.cropRect] and [TouchHelper.viewport]
 */
inline val TouchHelper.croppedTextureSize: Size
    // new width = width * (selection.width / viewportWidth)
    get() = Size(
        (texture.width * (cropRect.width() / viewport.width)).roundToInt(),
        (texture.height * (cropRect.height() / viewport.height)).roundToInt()
    )

private inline val TouchHelper.consumedTextureOffset: Offset
    get() = Offset(
        x = cropRect.left * (texture.width.toFloat() / viewport.width),
        y = -(viewport.height - cropRect.bottom) * (texture.height.toFloat() / viewport.height)
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

inline val TouchHelper.maxOffsetDistanceXBeforeEdgeVisible: Float
    get() = 1 - ratio + (2 * ratio - 2 * ratio / zoom) / 2

inline val TouchHelper.maxOffsetDistanceYBeforeEdgeVisible: Float
    get() = (2f - 2f / zoom) / 2f

inline val TouchHelper.maxOffsetDistanceYBeforeEdgeVisiblePx: Float
    get() = maxOffsetDistanceYBeforeEdgeVisible * viewport.height

inline val TouchHelper.maxOffsetDistanceBeforeEdgeVisiblePx: Float
    get() = maxOffsetDistanceXBeforeEdgeVisible * viewport.width

inline val TouchHelper.consumedOffsetX: Float
    get() = maxOffsetDistanceXBeforeEdgeVisible + (maxOffsetDistanceXBeforeEdgeVisible * -textureOffset.x / maxOffsetDistanceBeforeEdgeVisiblePx)

inline val TouchHelper.consumedOffsetY: Float
    get() = if (maxOffsetDistanceXBeforeEdgeVisible == 0f) {
        textureOffset.y / viewport.height
    } else {
        maxOffsetDistanceYBeforeEdgeVisible + (maxOffsetDistanceYBeforeEdgeVisible * textureOffset.y / maxOffsetDistanceYBeforeEdgeVisiblePx)
    }

//0.5258033 ratio, vp 1080 x 2054
// 1080 x 1584
private fun TouchHelper.toTextureCoordinateX(viewportX: Float): Float {
    val consumedPx = 2 * ratio * (viewportX / viewport.width) / zoom
    val realXNorm2 = consumedPx + consumedOffsetX
    val result = (viewport.width * realXNorm2) / 2

    println("result x  $result")

    return result
}

// textureDy [-textureDy..textureDy], so need to divide by 2 before
private fun TouchHelper.toTextureCoordinateY(viewportY: Float): Float {
    val invertPy = viewport.height - viewportY
    val consumePy = 2 * (invertPy / viewport.height) / zoom
    val realNormY = consumePy + consumedOffsetY
    val result = (viewport.height * realNormY) / 2

    println("result y $result")
    return result
}

@Suppress("NOTHING_TO_INLINE")
private inline operator fun RectF.contains(
    point: PointF,
): Boolean = point.x in left..right && point.y in top..bottom