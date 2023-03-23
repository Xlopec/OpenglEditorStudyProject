package com.epam.opengl.edu.model

fun update(message: Message, state: AppState): AppState =
    when (message) {
        is OnEditModeChanged -> state.withEditMode(message.isAppInEditMode)
        is OnImageSelected -> state.withImage(message.image)
    }