package io.github.xlopec.opengl.edu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import io.github.xlopec.opengl.edu.model.Message
import io.github.xlopec.opengl.edu.ui.App
import io.github.xlopec.opengl.edu.ui.MessageHandler
import io.github.xlopec.opengl.edu.ui.theme.AppTheme
import io.github.xlopec.opengl.edu.ui.theme.navigationBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val messages = MutableSharedFlow<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        setContent {
            val isDarkTheme = isSystemInDarkTheme()

            AppTheme(darkTheme = isDarkTheme) {
                val scope = rememberCoroutineScope { Dispatchers.Main.immediate }
                val handler = remember { scope.messageHandler(messages) }
                val statesFlow = remember { component(messages) }
                val snapshot = statesFlow.collectAsState(null).value ?: return@AppTheme
                val statusBarColor = MaterialTheme.navigationBar

                LaunchedEffect(statusBarColor, isDarkTheme) {
                    window.navigationBarColor = statusBarColor.toArgb()
                    windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
                }

                App(
                    snapshot = snapshot,
                    handler = handler,
                )
            }
        }
    }

}

private fun CoroutineScope.messageHandler(
    messages: FlowCollector<Message>,
): MessageHandler =
    { message -> launch { messages.emit(message) } }
