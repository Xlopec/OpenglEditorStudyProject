package com.epam.opengl.edu.ui.gl

import android.graphics.PointF
import android.view.MotionEvent
import com.epam.opengl.edu.model.CropSelection
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt


class TouchHelper {

    @Volatile
    var currentSpan = 0f
        private set

    @Volatile
    var cropDx = 0f
    //private set

    @Volatile
    var cropDy = 0f
    //private set

    @Volatile
    var textureDx = 0f
        private set

    @Volatile
    var textureDy = 0f
        private set

    var textureWidth = 0f
    var textureHeight = 0f

    var viewportWidth = 0f
    var viewportHeight = 0f

    private var previousX = Float.NaN
    private var previousY = 0f
    private var oldSpan = Float.NaN
    val pointer = PointF()

    fun reset() {
        currentSpan = 0f
        oldSpan = Float.NaN
        previousX = 0f
        previousY = 0f
    }

    fun updateScene(
        event: MotionEvent,
        cropSelection: CropSelection,
        viewportWidth: Int,
        viewportHeight: Int,
        isCropSelectionMode: Boolean,
    ) {
        println(event)
        val delX = event.x - previousX
        val delY = event.y - previousY
        val zoom = (currentSpan + viewportWidth.toFloat()) / viewportWidth.toFloat()

        val xOnTexture = toTextureCoordinateX(event.x)
        val yOnTexture = toTextureCoordinateY(event.y)

        pointer.set(scaledX(xOnTexture), scaledY(yOnTexture))

        println("Pointer $pointer, ${event.x},${event.y} txt (${textureDx}, ${textureDy}) zoom $zoom")

        previousX = event.x
        previousY = event.y

        if (event.pointerCount > 1) {
            // handle zooming path
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
        } else {
            // handle movement path
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    if (isCropSelectionMode && cropSelection.contains(event, viewportWidth)) {
                        // check crop selection won't move outside texture bounds
                        val newDx = (cropDx + delX / 2)//.coerceIn(0f, (viewportWidth - cropSelection.width).toFloat())
                        val newDy = (cropDy + delY)//.coerceIn(0f, (viewportHeight - cropSelection.height).toFloat())

                        cropDx = newDx
                        cropDy = newDy
                    } else {
                        textureDx += delX
                        textureDy += delY
                    }
                }

                MotionEvent.ACTION_UP -> {
                    oldSpan = Float.NaN
                }
            }
        }
    }

    private fun CropSelection.contains(
        event: MotionEvent,
        viewportWidth: Int,
    ): Boolean {
        val zoom = (currentSpan + viewportWidth.toFloat()) / viewportWidth.toFloat()
        // texture dx is initially set to 0, when user moves texture to the left -
        // textureDx goes [-viewportWidth / 2, 0];
        // when user moves texture to the right - it goes [0, viewPortWidth / 2];
        val xOnTexture = toTextureCoordinateX(event.x)
        val yOnTexture = toTextureCoordinateY(event.y)

        val cond = xOnTexture.roundToInt() in topLeft.x..bottomRight.x && yOnTexture.roundToInt() in topLeft.y..bottomRight.y
        println("($xOnTexture, $yOnTexture) $this, $textureDy $cond")

        return cond
    }

    // texture dx is initially set to 0, when user moves texture to the left -
    // textureDx goes [-viewportWidth / 2, 0];
    // when user moves texture to the right - it goes [0, viewPortWidth / 2];
    private fun toTextureCoordinateX(viewportX: Float) = (viewportX - textureDx + viewportWidth / 2) / 2

    // textureDy [-textureDy..textureDy], so need to divide by 2 before
    private fun toTextureCoordinateY(viewportY: Float) = viewportY - textureDy / 2

    /** range is [0f .. 1f] */
    private fun scaledX(xOnTexture: Float) = xOnTexture * (textureWidth / viewportWidth) / viewportWidth

    /** range is [1f - textureHeight / viewportHeight .. 1f] */
    private fun scaledY(yOnTexture: Float) =
        1f - textureHeight / viewportHeight + yOnTexture * (textureHeight / viewportHeight) / viewportHeight

}