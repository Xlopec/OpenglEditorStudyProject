package com.epam.opengl.edu.ui.gl

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.opengl.*
import android.opengl.GLSurfaceView.RENDERMODE_CONTINUOUSLY
import android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY
import android.view.MotionEvent
import android.view.View
import androidx.compose.ui.graphics.Color
import com.epam.opengl.edu.model.*
import com.epam.opengl.edu.model.geometry.*
import javax.microedition.khronos.egl.EGLConfig
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

    @Volatile
    var editor: Editor = editor
        set(value) {
            val old = field
            field = value

            val imageChanged = old.image != value.image
            val viewportChanged = old.displayTransformations.scene.windowSize != value.displayTransformations.scene.windowSize
            val transformationsChanged = value.displayTransformations != old.displayTransformations
            val cropSelectionChanged = value.displayCropSelection != old.displayCropSelection

            if (imageChanged) {
                view.queueEvent {
                    with(context) {
                        delegate!!.updateImage(value.image)
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

    init {
        view.setOnTouchListener(this)
    }

    private var delegate: GlRendererDelegate? = null
    private lateinit var gl: GL10

    override fun onSurfaceCreated(
        gl: GL10,
        config: EGLConfig
    ) = with(gl) {
        this@AppGLRenderer.gl = gl
        delegate = GlRendererDelegate(context, editor.image)
        view.renderMode = if (isDebugModeEnabled) RENDERMODE_CONTINUOUSLY else RENDERMODE_WHEN_DIRTY
    }

    override fun onDrawFrame(
        gl: GL10,
    ) = with(gl) {
        val isDebugEnabled = isDebugModeEnabled
        val delegate = delegate ?: return@with

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

    // fixme rework, also, it'll stuck forever if glThread is stopped before event is enqueued
    suspend fun bitmap(): Bitmap = suspendCoroutine { continuation ->
        view.queueEvent {
            continuation.resume(delegate!!.captureScene(editor.displayTransformations.scene))
        }
    }

    suspend fun crop() = suspendCoroutine { continuation ->
        view.queueEvent {
            try {
                with(gl) {
                    delegate!!.onDrawFrame(
                        backgroundColor = backgroundColor,
                        editor = editor,
                        cropRequested = true,
                        isDebugModeEnabled = isDebugModeEnabled
                    )
                    continuation.resume(Unit)
                }
            } catch (th: Throwable) {
                continuation.resumeWithException(th)
            }
        }
    }

    override fun onSurfaceChanged(
        gl: GL10,
        width: Int,
        height: Int,
    ) {
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

}
