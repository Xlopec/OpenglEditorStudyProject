package io.github.xlopec.opengl.edu

import android.app.Activity
import android.app.Application
import io.github.xlopec.opengl.edu.model.AppInitializer
import io.github.xlopec.opengl.edu.model.AppState
import io.github.xlopec.opengl.edu.model.Command
import io.github.xlopec.opengl.edu.model.Environment
import io.github.xlopec.opengl.edu.model.Message
import io.github.xlopec.opengl.edu.model.update
import io.github.xlopec.tea.core.Component
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class OpenGLDemoApp : Application() {
    val component: Component<Message, AppState, Command> by lazy {
        val env = Environment(application = this)

        Component(
            initializer = AppInitializer(env),
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
