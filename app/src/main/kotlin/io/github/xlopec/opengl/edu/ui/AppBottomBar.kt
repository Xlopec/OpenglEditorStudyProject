package io.github.xlopec.opengl.edu.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.xlopec.opengl.edu.R
import io.github.xlopec.opengl.edu.model.*
import io.github.xlopec.opengl.edu.model.transformation.*
import io.github.xlopec.opengl.edu.model.transformation.Contrast

private val MenuItemHorizontalSpacing = 10.dp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppBottomBar(
    modifier: Modifier = Modifier,
    editor: Editor,
    handler: MessageHandler,
) {
    Surface(
        modifier = modifier,
        elevation = 4.dp
    ) {
        val transition = updateTransition(targetState = editor.state, label = "Edit Menu Transition")

        transition.AnimatedContent(
            contentKey = { state -> state::class },
            transitionSpec = {
                if (targetState is Hidden) {
                    EnterTransition.None with slideOutOfContainer(AnimatedContentScope.SlideDirection.Down)
                } else {
                    slideIntoContainer(AnimatedContentScope.SlideDirection.Up) with slideOutOfContainer(
                        AnimatedContentScope.SlideDirection.Down
                    )
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

        GaussianBlur::class -> EditBlur(
            modifier = editMenuModifier,
            blur = state.edited.blur,
            handler = handler
        )

        Scene::class -> EditCrop(
            modifier = editMenuModifier,
            handler = handler
        )
    }
}

@Composable
private fun EditMenuDisplayed(
    handler: MessageHandler,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp, horizontal = MenuItemHorizontalSpacing),
        horizontalArrangement = Arrangement.spacedBy(MenuItemHorizontalSpacing)
    ) {
        IconButton(
            image = Icons.Filled.FilterBAndW,
            text = stringResource(R.string.menu_item_grayscale),
            onClick = { handler(OnSwitchedToEditTransformation(Grayscale::class)) }
        )

        IconButton(
            image = Icons.Filled.Contrast,
            text = stringResource(R.string.menu_item_contrast),
            onClick = { handler(OnSwitchedToEditTransformation(Contrast::class)) }
        )

        IconButton(
            image = Icons.Filled.Tonality,
            text = stringResource(R.string.menu_item_saturation),
            onClick = { handler(OnSwitchedToEditTransformation(Saturation::class)) }
        )

        IconButton(
            image = Icons.Filled.SettingsBrightness,
            text = stringResource(R.string.menu_item_brightness),
            onClick = { handler(OnSwitchedToEditTransformation(Brightness::class)) }
        )

        IconButton(
            image = Icons.Filled.Palette,
            text = stringResource(R.string.menu_item_tint),
            onClick = { handler(OnSwitchedToEditTransformation(Tint::class)) }
        )

        IconButton(
            image = Icons.Filled.BlurOn,
            text = stringResource(R.string.menu_item_blur),
            onClick = { handler(OnSwitchedToEditTransformation(GaussianBlur::class)) }
        )

        IconButton(
            image = Icons.Filled.Crop,
            text = stringResource(R.string.menu_item_crop),
            onClick = { handler(OnSwitchedToEditTransformation(Scene::class)) }
        )
    }
}

@Composable
private fun EditTint(
    modifier: Modifier,
    tint: Tint,
    handler: MessageHandler,
) {
    EditValue(
        modifier = modifier,
        title = stringResource(R.string.message_adjust_tint),
        value = tint.value,
        valueRange = Tint.Min.value..Tint.Max.value,
        onValueChange = { handler(OnTransformationUpdated(Tint(it))) },
        onApplyChanges = { handler(OnApplyChanges) },
        onDiscardChanges = { handler(OnDiscardChanges) },
    )
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
        onValueChange = { handler(OnTransformationUpdated(Grayscale(it))) },
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
        onValueChange = { handler(OnTransformationUpdated(Brightness(it))) },
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
        onValueChange = { handler(OnTransformationUpdated(Saturation(it))) },
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
        onValueChange = { handler(OnTransformationUpdated(Contrast(it))) },
        onApplyChanges = { handler(OnApplyChanges) },
        onDiscardChanges = { handler(OnDiscardChanges) },
    )
}

@Composable
internal fun EditBlur(
    modifier: Modifier,
    blur: GaussianBlur,
    handler: MessageHandler,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = stringResource(R.string.message_adjust_radius, blur.radius),
        )
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = blur.radius.toFloat(),
            valueRange = GaussianBlur.Min.radius.toFloat()..GaussianBlur.Max.radius.toFloat(),
            onValueChange = { handler(OnTransformationUpdated(blur.copy(radius = it.toInt()))) }
        )
        Text(
            text = stringResource(R.string.message_adjust_sigma, blur.sigma)
        )
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = blur.sigma.toFloat(),
            valueRange = GaussianBlur.Min.sigma.toFloat()..GaussianBlur.Max.sigma.toFloat(),
            onValueChange = { handler(OnTransformationUpdated(blur.copy(sigma = it.toInt()))) }
        )
        EditActions(
            title = stringResource(R.string.message_adjust_blur),
            onDiscardChanges = { handler(OnDiscardChanges) },
            onApplyChanges = { handler(OnApplyChanges) }
        )
    }
}

@Composable
private fun EditCrop(
    modifier: Modifier,
    handler: MessageHandler,
) {
    Box(
        modifier = modifier,
    ) {
        EditActions(
            title = stringResource(R.string.message_adjust_crop),
            onDiscardChanges = { handler(OnDiscardChanges) },
            onApplyChanges = { handler(OnApplyChanges) }
        )
    }
}

@Composable
private fun IconButton(
    image: ImageVector,
    enabled: Boolean = true,
    text: String? = null,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
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

        if (text != null) {
            Text(
                text = text,
                style = MaterialTheme.typography.caption
            )
        }
    }
}