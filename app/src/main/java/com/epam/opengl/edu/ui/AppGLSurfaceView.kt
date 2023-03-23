package com.epam.opengl.edu.ui

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.opengl.GLSurfaceView

@SuppressLint("ViewConstructor")
class AppGLSurfaceView(context: Context, image: Uri) : GLSurfaceView(context) {

    private val renderer = AppGLRenderer(context, image, this)

    var image: Uri by renderer::image

    init {
        setEGLContextClientVersion(3)
        setRenderer(renderer)
    }
}