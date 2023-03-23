package io.github.xlopec.opengl.edu.ui.gl

import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

class FpsCounter(
    private val onFpsUpdated: (UInt) -> Unit,
) {
    private companion object {
        val UpdatePeriod = 500.milliseconds
    }

    private var beginStamp = 0L
    private var frames = 0L

    var fps: UInt = 0U
        private set

    @Synchronized
    fun reset() {
        beginStamp = 0L
        frames = 0L
        fps = 0U
    }

    @Synchronized
    fun onFrame() {
        frames++
        val current = System.nanoTime()
        val period = (current - beginStamp).nanoseconds

        if (beginStamp == 0L) {
            beginStamp = current
        } else if (period > UpdatePeriod) {
            fps = (1000 * frames.toFloat() / period.inWholeMilliseconds).roundToInt().toUInt()
            beginStamp = current
            frames = 0L
            onFpsUpdated(fps)
        }
    }

}