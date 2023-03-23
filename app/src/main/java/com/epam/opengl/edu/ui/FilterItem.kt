package com.epam.opengl.edu.ui

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun FilterItem(
    image: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = image,
            contentDescription = null
        )
    }
}