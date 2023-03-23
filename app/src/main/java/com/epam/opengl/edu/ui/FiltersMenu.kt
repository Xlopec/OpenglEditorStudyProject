package com.epam.opengl.edu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun FiltersMenu() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilterItem(image = Icons.Filled.Brightness3, enabled = true) {
                // TODO
            }

            FilterItem(image = Icons.Filled.Contrast, enabled = true) {
                // TODO
            }

            FilterItem(image = Icons.Filled.Crop, enabled = true) {
                // TODO
            }

            FilterItem(image = Icons.Filled.BrightnessAuto, enabled = true) {
                // TODO
            }
        }
    }
}