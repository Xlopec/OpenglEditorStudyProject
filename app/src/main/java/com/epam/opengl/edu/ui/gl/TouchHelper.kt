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

    private var previousX = Float.NaN
    private var previousY = 0f
    private var oldSpan = Float.NaN
    val pointer = PointF(0f, 0f)

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
        val aspectRatio = viewportWidth.toFloat() / viewportHeight.toFloat()
        val delX = event.x - previousX
        val delY = event.y - previousY

        val zoom = (currentSpan + viewportWidth.toFloat()) / viewportWidth.toFloat()

        // texture dx is initially set to 0, when user moves texture to the left -
        // textureDx goes [-viewportWidth / 2, 0];
        // when user moves texture to the right - it goes [0, viewPortWidth / 2];
        val xOnTexture = (((event.x - textureDx + viewportWidth.toFloat() / 2)) / 2) / zoom
        val yOnTexture = (event.y - textureDy / 2) / zoom

        pointer.set(xOnTexture, yOnTexture)

        println("Pointer $pointer, ${event.x},${event.y} txt ${textureDy}")

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
        val xOnTexture = (((event.x - textureDx + viewportWidth.toFloat() / 2)) / 2) / zoom
        val yOnTexture = (event.y - textureDy / 2) / zoom

        val cond = xOnTexture.roundToInt() in topLeft.x..bottomRight.x && yOnTexture.roundToInt() in topLeft.y..bottomRight.y
        println("($xOnTexture, $yOnTexture) $this, $textureDy $cond")

        return cond
    }

}