package com.epam.opengl.edu

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.epam.opengl.edu.model.Message
import com.epam.opengl.edu.model.OnImageSelected
import com.epam.opengl.edu.ui.App
import com.epam.opengl.edu.ui.MessageHandler
import com.epam.opengl.edu.ui.theme.AppTheme
import io.github.xlopec.tea.core.ExperimentalTeaApi
import io.github.xlopec.tea.core.toStatesComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), CoroutineScope by MainScope() {

    private val messages = MutableSharedFlow<Message>()

    @OptIn(ExperimentalTeaApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val chooserLauncher = registerForActivityResult(GetContent()) { uri: Uri? ->
            if (uri != null) {
                launch { messages.emit(OnImageSelected(uri)) }
            }
        }

        setContent {
            AppTheme {
                val scope = rememberCoroutineScope()
                val handler = remember { scope.messageHandler(messages) }
                val statesFlow = remember { component.toStatesComponent()(messages) }
                val state = statesFlow.collectAsState(null).value ?: return@AppTheme

                App(
                    state = state,
                    handler = handler,
                    onChooseImage = { chooserLauncher.launch("image/*") }
                )
            }
        }
    }

    override fun onDestroy() {
        cancel()
        super.onDestroy()
    }

}

private fun CoroutineScope.messageHandler(
    messages: FlowCollector<Message>,
): MessageHandler =
    { message -> launch { messages.emit(message) } }
