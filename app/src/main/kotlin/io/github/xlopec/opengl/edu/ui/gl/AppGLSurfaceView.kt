package io.github.xlopec.opengl.edu.ui.gl

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.SurfaceHolder
import io.github.xlopec.opengl.edu.model.Editor
import io.github.xlopec.opengl.edu.model.geometry.Size

@SuppressLint("ViewConstructor")
class AppGLSurfaceView(
    context: Context,
    editor: Editor,
    onViewportUpdated: (Size) -> Unit,
    onFpsUpdated: (UInt) -> Unit,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {

    private val renderer = AppGLRenderer(
        context = context,
        view = this,
        editor = editor,
        onViewportSizeChange = onViewportUpdated,
        onFpsUpdated = onFpsUpdated
    )

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        preserveEGLContextOnPause = true
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setRenderer(renderer)
    }

    var isDebugModeEnabled by renderer::isDebugModeEnabled

    var editor by renderer::editor

    var backgroundColor by renderer::backgroundColor

    suspend fun exportFrame() = renderer.exportFrame()

    suspend fun crop() = renderer.crop()

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        super.surfaceDestroyed(holder)
        renderer.onSurfaceDestroyed()
    }
}