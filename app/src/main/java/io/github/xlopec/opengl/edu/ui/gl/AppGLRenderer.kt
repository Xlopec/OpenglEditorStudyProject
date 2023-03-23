package io.github.xlopec.opengl.edu.ui.gl

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.opengl.*
import android.opengl.GLSurfaceView.RENDERMODE_CONTINUOUSLY
import android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY
import android.view.MotionEvent
import android.view.View
import androidx.compose.ui.graphics.Color
import io.github.xlopec.opengl.edu.model.*
import io.github.xlopec.opengl.edu.model.geometry.*
import io.github.xlopec.opengl.edu.model.transformation.subImage
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL
import javax.microedition.khronos.opengles.GL10
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AppGLRenderer(
    private val context: Context,
    private val view: GLSurfaceView,
    editor: Editor,
    private val onViewportSizeChange: (Size) -> Unit,
    onFpsUpdated: (UInt) -> Unit,
    isDebugModeEnabled: Boolean = false,
) : GLSurfaceView.Renderer, View.OnTouchListener {

    init {
        view.setOnTouchListener(this)
    }

    @Volatile
    var editor: Editor = editor
        set(value) {
            val old = field
            field = value

            val imageChanged = old.image != value.image
            val viewportChanged =
                old.displayTransformations.scene.windowSize != value.displayTransformations.scene.windowSize
            val transformationsChanged = value.displayTransformations != old.displayTransformations
            val cropSelectionChanged = value.displayCropSelection != old.displayCropSelection

            if (imageChanged) {
                view.queueEvent {
                    val delegate = renderDelegate
                    if (delegate != null) {
                        val bitmap = with(context) { value.image.asBitmap() }
                        delegate.updateImage(bitmap)
                        bitmap.recycle()
                    }
                }
            }

            if (imageChanged || transformationsChanged || viewportChanged || cropSelectionChanged) {
                view.requestRender()
            }
        }

    @Volatile
    var backgroundColor: Color = Color.White

    @Volatile
    var isDebugModeEnabled: Boolean = isDebugModeEnabled
        set(value) {
            val old = field
            field = value
            if (old != value) {
                fpsCounter.reset()
                view.renderMode = if (value) RENDERMODE_CONTINUOUSLY else RENDERMODE_WHEN_DIRTY
            }
        }

    private val fpsCounter = FpsCounter(onFpsUpdated)

    @Volatile
    private var renderDelegate: GlRendererDelegate? = null
    private inline val renderDelegateOrThrow: GlRendererDelegate
        get() = requireNotNull(renderDelegate) { "gl renderer gone" }

    override fun onSurfaceCreated(
        gl: GL10,
        config: EGLConfig,
    ) = with(gl) {
        initDelegateIfNeeded()
    }

    override fun onDrawFrame(
        gl: GL10,
    ) = with(gl) {
        val isDebugEnabled = isDebugModeEnabled
        val delegate = renderDelegate ?: return@with

        delegate.onDrawFrame(
            backgroundColor = backgroundColor,
            editor = editor,
            cropRequested = false,
            isDebugModeEnabled = isDebugEnabled,
        )

        if (isDebugEnabled) {
            fpsCounter.onFrame()
        }
    }

    fun onSurfaceDestroyed() {
        renderDelegate = null
        fpsCounter.reset()
    }

    // fixme rework, also, it'll stuck forever if glThread is stopped before event is enqueued
    suspend fun bitmap(): Bitmap = suspendCoroutine { continuation ->
        view.queueEvent {
            continuation.resume(renderDelegateOrThrow.captureScene(editor.displayTransformations.scene))
        }
    }

    suspend fun crop() = suspendCoroutine { continuation ->
        view.queueEvent {
            try {
                renderDelegateOrThrow.onDrawFrame(
                    backgroundColor = backgroundColor,
                    editor = editor,
                    cropRequested = true,
                    isDebugModeEnabled = isDebugModeEnabled
                )
                continuation.resume(Unit)
            } catch (th: Throwable) {
                continuation.resumeWithException(th)
            }
        }
    }

    override fun onSurfaceChanged(
        gl: GL10,
        width: Int,
        height: Int,
    ) = with(gl) {
        initDelegateIfNeeded()
        onViewportSizeChange(Size(width, height))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(
        v: View,
        event: MotionEvent,
    ): Boolean {
        val selectionMode = editor.displayCropSelection
        editor.displayTransformations.scene.onTouch(event, selectionMode)
        view.requestRender()
        return true
    }

    context (GL)
            private fun initDelegateIfNeeded() {
        if (renderDelegate == null) {
            val scene = editor.displayTransformations.scene
            // restores crop state by reading subregion of the original image
            val bitmap = with(context) { editor.image.asBitmap(scene.subImage) }
            renderDelegate = GlRendererDelegate(context, bitmap)
            bitmap.recycle()
            view.renderMode = if (isDebugModeEnabled) RENDERMODE_CONTINUOUSLY else RENDERMODE_WHEN_DIRTY
        }
    }

}
