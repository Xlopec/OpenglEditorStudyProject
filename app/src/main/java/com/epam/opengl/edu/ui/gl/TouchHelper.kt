package com.epam.opengl.edu.ui.gl

import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

class TouchHelper {

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
    val cropRect = RectF()

    /**
     * Current texture size, can't exceed [viewport] size
     */
    var texture = Size(0, 0)
        private set

    /**
     * Viewport size
     */
    var viewport = Size(0, 0)
        private set

    /**
     * Texture size given current [cropRect] and [viewport]
     */
    val croppedTextureSize: Size
        // new width = width * (selection.width / viewportWidth)
        get() = Size(
            (texture.width * (cropRect.width() / viewport.width)).roundToInt(),
            (texture.height * (cropRect.height() / viewport.height)).roundToInt()
        )
    private var currentSpan = 0f
    private val previousInput = PointF()
    private var oldSpan = Float.NaN

    val ratio: Float
        get() = viewport.width.toFloat() / viewport.height

    val zoom: Float
        get() = (currentSpan + viewport.width) / viewport.width.toFloat()

    private inline val consumedTextureOffset: Offset
        get() = Offset(
            x = cropRect.left * (texture.width.toFloat() / viewport.width),
            y = -(viewport.height - cropRect.bottom) * (texture.height.toFloat() / viewport.height)
        )

    fun reset() {
        currentSpan = 0f
        oldSpan = Float.NaN
        previousInput.set(0f, 0f)
    }

    fun resetCropSelection() {
        cropRect.set(0f, 0f, viewport.width.toFloat(), viewport.height.toFloat())
    }

    fun onSurfaceChanged(
        width: Int,
        height: Int,
    ) {
        viewport = Size(width, height)
        texture = viewport
        cropOriginOffset = Offset(0f, 0f)
        resetCropSelection()
    }

    fun onTouch(
        event: MotionEvent,
        isCropSelectionMode: Boolean,
    ) {
        val offset = Offset(event.x - previousInput.x, event.y - previousInput.y)

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
                            (cropRect.top + offset.y).coerceIn(0f, viewport.height - cropRect.height())
                        )
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

    fun onTexturesCropped() {
        cropOriginOffset += consumedTextureOffset
        texture = croppedTextureSize
        resetCropSelection()
    }

    /** range is [0f .. 1f] */
    fun normalizedX(xOnTexture: Float) = xOnTexture * (texture.width.toFloat() / viewport.width) / viewport.width.toFloat()

    /** range is [1f - textureHeight / viewportHeight .. 1f] */
    fun normalizedY(yOnTexture: Float) =
        1f - texture.height.toFloat() / viewport.height + yOnTexture * (texture.height.toFloat() / viewport.height) / viewport.height.toFloat()

    @Suppress("NOTHING_TO_INLINE")
    private inline operator fun RectF.contains(
        point: PointF,
    ) = point.x in left..right && point.y in top..bottom

    // texture dx is initially set to 0, when user moves texture to the left -
    // textureDx goes [-viewportWidth / 2, 0];
    // when user moves texture to the right - it goes [0, viewPortWidth / 2];
    private fun toTextureCoordinateX(viewportX: Float) = (viewportX - textureOffset.x + viewport.width / 2) / 2

    // textureDy [-textureDy..textureDy], so need to divide by 2 before
    private fun toTextureCoordinateY(viewportY: Float) = viewportY - textureOffset.y / 2

}