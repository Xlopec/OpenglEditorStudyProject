package com.epam.opengl.edu

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import com.epam.opengl.edu.model.OnEditModeChanged
import com.epam.opengl.edu.model.OnImageSelected
import com.epam.opengl.edu.ui.AppGLSurfaceView
import com.epam.opengl.edu.ui.FiltersMenu
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

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(text = stringResource(R.string.app_name))
                            },
                            actions = {
                                IconButton(onClick = {
                                    updateState(OnEditModeChanged(isAppInEditMode = !state.isAppInEditMode))
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = {
                            chooserLauncher.launch("image/*")
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Image,
                                contentDescription = null
                            )
                        }
                    },
                    bottomBar = {
                        AnimatedVisibility(
                            visible = state.isAppInEditMode,
                            enter = expandIn { IntSize(it.width, 0) },
                            exit = shrinkOut { IntSize(it.width, 0) },
                        ) {
                            FiltersMenu()
                        }
                    }
                ) {
                    val layoutDirection = LocalLayoutDirection.current
                    // ignore bottom padding, let bottom bar overlap the SurfaceView
                    val padding = PaddingValues(
                        start = it.calculateStartPadding(layoutDirection),
                        end = it.calculateEndPadding(layoutDirection),
                        top = it.calculateTopPadding()
                    )
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (state.image == null) {
                            Text(text = stringResource(R.string.message_no_image))
                        } else {
                            AndroidView({ context -> AppGLSurfaceView(context, state.image) }) { view ->
                                view.image = state.image
                            }
                        }
                    }
                }
            }
        }
    }

}
