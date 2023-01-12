package com.epam.opengl.edu

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.runtime.collectAsState
import com.epam.opengl.edu.model.OnImageSelected
import com.epam.opengl.edu.ui.App
import com.epam.opengl.edu.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val chooserLauncher = registerForActivityResult(GetContent()) { uri: Uri? ->
            if (uri != null) {
                updateState(OnImageSelected(uri))
            }
        }

        setContent {
            AppTheme {
                val state = appStateFlow.collectAsState().value

                App(
                    state = state,
                    handler = ::updateState,
                    onChooseImage = { chooserLauncher.launch("image/*") }
                )
            }
        }
    }

}
