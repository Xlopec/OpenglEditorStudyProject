package com.epam.opengl.edu

import android.app.Activity
import android.app.Application
import com.epam.opengl.edu.model.AppState
import com.epam.opengl.edu.model.Message
import com.epam.opengl.edu.model.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class App : Application() {
    val stateFlow = MutableStateFlow(AppState())
}

fun Activity.updateState(
    message: Message
) {
    app.stateFlow.value = update(message, app.stateFlow.value)
}

inline val Activity.app
    get() = application as App

inline val Activity.appStateFlow: StateFlow<AppState>
    get() = app.stateFlow.asStateFlow()
