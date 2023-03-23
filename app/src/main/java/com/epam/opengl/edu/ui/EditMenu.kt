package com.epam.opengl.edu.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.epam.opengl.edu.R
import com.epam.opengl.edu.model.*
import com.epam.opengl.edu.ui.theme.AppTheme

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EditMenu(
    modifier: Modifier = Modifier,
    menu: EditMenu,
    handler: MessageHandler,
) {
    Surface(
        modifier = modifier
    ) {
        val transition = updateTransition(targetState = menu.state, label = "Edit Menu Transition")

        transition.AnimatedContent(
            contentKey = { state -> state::class },
            transitionSpec = {
                if (targetState is Hidden) {
                    EnterTransition.None with slideOutOfContainer(AnimatedContentScope.SlideDirection.Down)
                } else {
                    slideIntoContainer(AnimatedContentScope.SlideDirection.Up) with slideOutOfContainer(AnimatedContentScope.SlideDirection.Down)
                }
            }
        ) { state ->
            when (state) {
                is EditTransformation -> EditTransformationMenu(state, handler)
                Hidden -> Box(modifier = Modifier.fillMaxWidth())
                Displayed -> EditMenuDisplayed(handler)
            }
        }
    }
}

@Composable
private fun EditTransformationMenu(
    state: EditTransformation,
    handler: MessageHandler,
) {
    when (state.which) {
        Grayscale::class -> EditGrayscale(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            grayscale = state.edited.grayscale,
            handler = handler
        )
    }
}

@Composable
private fun EditMenuDisplayed(
    handler: MessageHandler,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(
            image = Icons.Filled.FilterBAndW,
            enabled = true
        ) { handler(OnSwitchedToEditTransformation(Grayscale::class)) }

        IconButton(
            image = Icons.Filled.Contrast,
            enabled = false
        ) {
            // TODO
        }

        IconButton(
            image = Icons.Filled.Crop,
            enabled = false
        ) {
            // TODO
        }

        IconButton(
            image = Icons.Filled.BrightnessAuto,
            enabled = false
        ) {
            // TODO
        }
    }
}

@Composable
private fun EditGrayscale(
    modifier: Modifier,
    grayscale: Grayscale,
    handler: MessageHandler,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Slider(
            value = grayscale.value,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { handler(OnGrayscaleUpdated(it)) }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                image = Icons.Filled.Close,
                onClick = { handler(OnDiscardChanges) }
            )

            Text(text = stringResource(R.string.message_adjust_grayscale))

            IconButton(
                image = Icons.Filled.Done,
                onClick = { handler(OnApplyChanges) }
            )
        }
    }
}

@Composable
private fun IconButton(
    image: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled
    ) {
        Icon(
            imageVector = image,
            contentDescription = null
        )
    }
}

@Preview
@Composable
private fun EditMenuDisplayedPreview() {
    AppTheme {
        EditMenu(
            menu = EditMenu(state = Displayed),
            handler = {}
        )
    }
}

@Preview
@Composable
private fun EditMenuEditTransformationPreview() {
    AppTheme {
        EditMenu(
            menu = EditMenu(state = EditTransformation(Grayscale::class, Transformations(Grayscale(0.37f)))),
            handler = {}
        )
    }
}