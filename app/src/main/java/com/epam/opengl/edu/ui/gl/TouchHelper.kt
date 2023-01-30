package com.epam.opengl.edu.ui.gl

import android.view.MotionEvent
import com.epam.opengl.edu.model.CropSelection
import com.epam.opengl.edu.model.height
import com.epam.opengl.edu.model.width
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

class TouchHelper {

    @Volatile
    var currentSpan = 0f
        private set

    @Volatile
    var cropDx = 0f
        private set

    @Volatile
    var cropDy = 0f
        private set

    @Volatile
    var textureDx = 0f
        private set

    @Volatile
    var textureDy = 0f
        private set

    private var previousX = 0f
    private var previousY = 0f
    private var oldSpan = Float.NaN

    fun reset() {
        /*_deltaX = 0f
        _deltaY = 0f*/
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
    ) {
        println(event)
        val aspectRatio = viewportWidth.toFloat() / viewportHeight.toFloat()
        val delX = event.x - previousX
        val delY = event.y - previousY

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
                    if (cropSelection.contains(event, viewportWidth, viewportHeight)) {
                        // check crop selection won't move outside texture bounds
                        val newDx = (cropDx + delX * aspectRatio).coerceIn(0f, (viewportWidth - cropSelection.width).toFloat())
                        val newDy = (cropDy + delY).coerceIn(0f, (viewportHeight - cropSelection.height).toFloat())

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
        viewportHeight: Int,
    ): Boolean {
        val ratio = viewportWidth.toFloat() / viewportHeight.toFloat()
        val zoom = (currentSpan + viewportWidth.toFloat()) / viewportWidth.toFloat()
        // texture dx is initially set to 0, when user moves texture to the left -
        // textureDx goes [-viewportWidth / 2, 0];
        // when user moves texture to the right - it goes [0, viewPortWidth / 2];
        val xOnTexture = ((viewportWidth / 2 - textureDx + event.x) / zoom) * ratio
        val yOnTexture = ((event.y - textureDy) / zoom)

        return xOnTexture.roundToInt() in topLeft.x..bottomRight.x && yOnTexture.roundToInt() in topLeft.y..bottomRight.y
    }

}