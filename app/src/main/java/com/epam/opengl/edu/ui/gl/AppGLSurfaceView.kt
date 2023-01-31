package com.epam.opengl.edu.ui.gl

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.opengl.GLSurfaceView
import com.epam.opengl.edu.model.Transformations

@SuppressLint("ViewConstructor")
class AppGLSurfaceView(context: Context, image: Uri, transformations: Transformations) : GLSurfaceView(context) {

    val renderer = AppGLRenderer(context, image, transformations, this)

    init {
        setEGLContextClientVersion(3)
        setRenderer(renderer)
    }
}