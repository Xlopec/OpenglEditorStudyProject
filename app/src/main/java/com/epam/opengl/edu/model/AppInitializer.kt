package com.epam.opengl.edu.model

import io.github.xlopec.tea.core.Initial
import io.github.xlopec.tea.core.Initializer
import kotlinx.coroutines.Dispatchers

fun AppInitializer(
    environment: Environment,
): Initializer<AppState, Command> = Initializer(Dispatchers.IO) {
    Initial(AppState(isDebugModeEnabled = environment.value.isDebugModeEnabled))
}