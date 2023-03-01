package com.epam.opengl.edu

import android.app.Activity
import android.app.Application
import com.epam.opengl.edu.model.*
import io.github.xlopec.tea.core.Component
import io.github.xlopec.tea.core.Initializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class OpenGLDemoApp : Application() {
    val component: Component<Message, AppState, Command> by lazy {
        val env = Environment(application = this)

        Component(
            initializer = Initializer(AppState()),
            resolver = { snapshot, context -> with(env) { resolve(snapshot, context) } },
            updater = ::update,
            scope = CoroutineScope(Job() + Dispatchers.Unconfined),
        )
    }
}

inline val Activity.app
    get() = application as OpenGLDemoApp

inline val Activity.component: Component<Message, AppState, Command>
    get() = app.component
