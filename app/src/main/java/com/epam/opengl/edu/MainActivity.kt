package com.epam.opengl.edu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.epam.opengl.edu.model.Message
import com.epam.opengl.edu.ui.App
import com.epam.opengl.edu.ui.MessageHandler
import com.epam.opengl.edu.ui.theme.AppTheme
import io.github.xlopec.tea.core.ExperimentalTeaApi
import io.github.xlopec.tea.core.toStatesComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val messages = MutableSharedFlow<Message>()

    @OptIn(ExperimentalTeaApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val scope = rememberCoroutineScope { Dispatchers.Main.immediate }
                val handler = remember { scope.messageHandler(messages) }
                val statesFlow = remember { component.toStatesComponent()(messages) }
                val state = statesFlow.collectAsState(null).value ?: return@AppTheme

                App(
                    state = state,
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
