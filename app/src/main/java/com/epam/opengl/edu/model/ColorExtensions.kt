package com.epam.opengl.edu.model

import androidx.compose.ui.graphics.Color
import java.util.*

inline val Color.hexCode: String
    get() {
        val a: Int = (alpha * 255).toInt()
        val r: Int = (red * 255).toInt()
        val g: Int = (green * 255).toInt()
        val b: Int = (blue * 255).toInt()
        return String.format(Locale.getDefault(), "%02X%02X%02X%02X", a, r, g, b)
    }

inline val Color.opaque: Color
    get() = copy(alpha = 1f)