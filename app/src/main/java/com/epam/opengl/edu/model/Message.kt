package com.epam.opengl.edu.model

import android.net.Uri

sealed interface Message

@JvmInline
value class OnEditModeChanged(
    val isAppInEditMode: Boolean
) : Message

@JvmInline
value class OnImageSelected(
    val image: Uri
) : Message