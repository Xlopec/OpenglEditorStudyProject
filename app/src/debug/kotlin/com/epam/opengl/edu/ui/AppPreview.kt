package com.epam.opengl.edu.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.epam.opengl.edu.model.AppState
import com.epam.opengl.edu.ui.theme.AppTheme
import io.github.xlopec.tea.core.Initial

@Preview(name = "App in light mode")
@Composable
fun AppPreviewLight() {
    AppTheme(darkTheme = false) {
        App(
            snapshot = Initial(AppState()),
            handler = {},
        )
    }
}

@Preview(name = "App in dark mode")
@Composable
fun AppPreviewDark() {
    AppTheme(darkTheme = true) {
        App(
            snapshot = Initial(AppState()),
            handler = {},
        )
    }
}