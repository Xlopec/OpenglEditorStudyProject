package com.epam.opengl.edu.ui

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.epam.opengl.edu.model.Displayed
import com.epam.opengl.edu.model.EditTransformation
import com.epam.opengl.edu.model.Editor
import com.epam.opengl.edu.model.geometry.Size
import com.epam.opengl.edu.model.transformation.GaussianBlur
import com.epam.opengl.edu.model.transformation.Grayscale
import com.epam.opengl.edu.model.transformation.Scene
import com.epam.opengl.edu.model.transformation.Transformations
import com.epam.opengl.edu.ui.theme.AppTheme


@Preview
@Composable
private fun EditMenuDisplayedPreview() {
    AppTheme {
        AppBottomBar(
            editor = Editor(
                current = Transformations(
                    Scene(
                        imageSize = Size(10, 10),
                        windowSize = Size(
                            width = 1080,
                            height = 1584
                        )
                    )
                ),
                image = Uri.EMPTY,
                state = Displayed
            ),
            handler = {}
        )
    }
}

@Preview
@Composable
private fun EditBlurTransformationPreview() {
    AppTheme {
        Surface {
            EditBlur(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                blur = GaussianBlur(3, 7),
                handler = {}
            )
        }
    }
}

@Preview
@Composable
private fun EditMenuEditTransformationPreview() {
    AppTheme {
        AppBottomBar(
            editor = Editor(
                image = Uri.EMPTY,
                current = Transformations(
                    Scene(
                        imageSize = Size(10, 10),
                        windowSize = Size(
                            width = 1080,
                            height = 1584
                        )
                    )
                ),
                state = EditTransformation(
                    which = Grayscale::class,
                    edited = Transformations(
                        scene = Scene(
                            imageSize = Size(10, 10),
                            windowSize = Size(
                                width = 1080,
                                height = 1584
                            )
                        ),
                        grayscale = Grayscale(0.37f)
                    )
                )
            ),
            handler = {}
        )
    }
}
