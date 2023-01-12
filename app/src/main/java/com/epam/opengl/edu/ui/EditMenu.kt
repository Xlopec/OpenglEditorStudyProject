package com.epam.opengl.edu.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.epam.opengl.edu.R
import com.epam.opengl.edu.model.*
import com.epam.opengl.edu.ui.theme.AppTheme
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EditMenu(
    modifier: Modifier = Modifier,
    menu: EditMenu,
    handler: MessageHandler,
) {
    Surface(
        modifier = modifier,
        elevation = 4.dp
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
    val editMenuModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 4.dp)

    when (state.which) {
        Grayscale::class -> EditGrayscale(
            modifier = editMenuModifier,
            grayscale = state.edited.grayscale,
            handler = handler
        )

        Brightness::class -> EditBrightness(
            modifier = editMenuModifier,
            brightness = state.edited.brightness,
            handler = handler
        )

        Saturation::class -> EditSaturation(
            modifier = editMenuModifier,
            saturation = state.edited.saturation,
            handler = handler
        )

        Contrast::class -> EditContrast(
            modifier = editMenuModifier,
            contrast = state.edited.contrast,
            handler = handler
        )

        Tint::class -> EditTint(
            modifier = editMenuModifier,
            tint = state.edited.tint,
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
            onClick = { handler(OnSwitchedToEditTransformation(Grayscale::class)) }
        )

        IconButton(
            image = Icons.Filled.Contrast,
            onClick = { handler(OnSwitchedToEditTransformation(Contrast::class)) }
        )

        IconButton(
            image = Icons.Filled.Tonality,
            onClick = { handler(OnSwitchedToEditTransformation(Saturation::class)) }
        )

        IconButton(
            image = Icons.Filled.SettingsBrightness,
            onClick = { handler(OnSwitchedToEditTransformation(Brightness::class)) }
        )

        IconButton(
            image = Icons.Filled.Palette,
            onClick = { handler(OnSwitchedToEditTransformation(Tint::class)) }
        )
    }
}

@Composable
private fun EditTint(
    modifier: Modifier,
    tint: Tint,
    handler: MessageHandler,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val controller = rememberColorPickerController()

        LaunchedEffect(tint.color) {
            controller.setWheelColor(tint.color.opaque)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AlphaSlider(
                    modifier = Modifier.height(30.dp),
                    controller = controller
                )

                Row(
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 50.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(25.dp)
                            .background(tint.color, RoundedCornerShape(5.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(5.dp))
                    )

                    Text(
                        modifier = Modifier.weight(1f),
                        text = "#" + tint.color.hexCode,
                        style = MaterialTheme.typography.body2
                    )

                    if (tint.color != Tint.InitialColor) {
                        IconButton(
                            image = Icons.Filled.RestartAlt,
                            tint = LocalContentColor.current.copy(alpha = 0.8f),
                            onClick = { handler(OnTintUpdated(Tint.InitialColor)) }
                        )
                    }
                }
            }


            HsvColorPicker(
                modifier = Modifier.height(150.dp).weight(1f),
                controller = controller,
                onColorChanged = { envelope ->
                    if (envelope.fromUser) {
                        handler(OnTintUpdated(envelope.color))
                    }
                }
            )
        }

        EditActions(
            title = stringResource(R.string.message_adjust_tint),
            onDiscardChanges = { handler(OnDiscardChanges) },
            onApplyChanges = { handler(OnApplyChanges) }
        )
    }
}

@Composable
private fun EditValue(
    modifier: Modifier,
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onDiscardChanges: () -> Unit,
    onApplyChanges: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            valueRange = valueRange,
            onValueChange = onValueChange
        )
        EditActions(
            title = title,
            onDiscardChanges = onDiscardChanges,
            onApplyChanges = onApplyChanges
        )
    }
}

@Composable
private fun EditActions(
    title: String,
    onDiscardChanges: () -> Unit,
    onApplyChanges: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            image = Icons.Filled.Close,
            onClick = onDiscardChanges
        )

        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1
        )

        IconButton(
            image = Icons.Filled.Done,
            onClick = onApplyChanges
        )
    }
}

@Composable
private fun EditGrayscale(
    modifier: Modifier,
    grayscale: Grayscale,
    handler: MessageHandler,
) {
    EditValue(
        modifier = modifier,
        title = stringResource(R.string.message_adjust_grayscale),
        value = grayscale.value,
        valueRange = Grayscale.Min.value..Grayscale.Max.value,
        onValueChange = { handler(OnGrayscaleUpdated(it)) },
        onApplyChanges = { handler(OnApplyChanges) },
        onDiscardChanges = { handler(OnDiscardChanges) },
    )
}

@Composable
private fun EditBrightness(
    modifier: Modifier,
    brightness: Brightness,
    handler: MessageHandler,
) {
    EditValue(
        modifier = modifier,
        title = stringResource(R.string.message_adjust_brightness),
        value = brightness.delta,
        valueRange = Brightness.Min.delta..Brightness.Max.delta,
        onValueChange = { handler(OnBrightnessUpdated(it)) },
        onApplyChanges = { handler(OnApplyChanges) },
        onDiscardChanges = { handler(OnDiscardChanges) },
    )
}

@Composable
private fun EditSaturation(
    modifier: Modifier,
    saturation: Saturation,
    handler: MessageHandler,
) {
    EditValue(
        modifier = modifier,
        title = stringResource(R.string.message_adjust_saturation),
        value = saturation.delta,
        valueRange = Saturation.Min.delta..Saturation.Max.delta,
        onValueChange = { handler(OnSaturationUpdated(it)) },
        onApplyChanges = { handler(OnApplyChanges) },
        onDiscardChanges = { handler(OnDiscardChanges) },
    )
}

@Composable
private fun EditContrast(
    modifier: Modifier,
    contrast: Contrast,
    handler: MessageHandler,
) {
    EditValue(
        modifier = modifier,
        title = stringResource(R.string.message_adjust_contrast),
        value = contrast.delta,
        valueRange = Contrast.Min.delta..Contrast.Max.delta,
        onValueChange = { handler(OnContrastUpdated(it)) },
        onApplyChanges = { handler(OnApplyChanges) },
        onDiscardChanges = { handler(OnDiscardChanges) },
    )
}

@Composable
private fun IconButton(
    image: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
) {
    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        enabled = enabled
    ) {
        Icon(
            imageVector = image,
            tint = tint,
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
private fun EditTintTransformationPreview() {
    AppTheme {
        Surface {
            EditTint(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                tint = Tint(Color.Red),
                handler = {}
            )
        }
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