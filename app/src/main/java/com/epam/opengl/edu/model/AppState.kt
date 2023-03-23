package com.epam.opengl.edu.model

import android.net.Uri
import androidx.compose.runtime.Immutable

sealed interface Filter

@Immutable
data class AppState(
    val isAppInEditMode: Boolean = false,
    val image: Uri? = null,
    val appliedFilters: List<Filter> = listOf(),
)

fun AppState.revertLastFilter(): AppState = copy(appliedFilters = appliedFilters.dropLast(1))

fun AppState.withEditMode(
    isAppInEditMode: Boolean
) = copy(isAppInEditMode = isAppInEditMode)

fun AppState.withImage(
    image: Uri?
) = copy(image = image)


