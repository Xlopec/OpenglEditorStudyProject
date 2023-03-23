package io.github.xlopec.opengl.edu.model

import android.app.Application

interface Environment : Provider<Application>, AppResolver<Environment>

fun Environment(
    application: Application,
): Environment = object : Environment,
    Provider<Application> by StaticOf(application),
    AppResolver<Environment> by AppResolver() {

}
