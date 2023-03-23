package io.github.xlopec.opengl.edu.model

import android.content.Context
import android.content.SharedPreferences

private const val SharedPrefsName = "Opengl-editor-prefs"
private const val KeyDebugModeEnabled = "debug-mode-enabled"

val Context.sharedPreferences: SharedPreferences
    get() = getSharedPreferences(SharedPrefsName, Context.MODE_PRIVATE)

var Context.isDebugModeEnabled: Boolean
    set(value) = sharedPreferences.edit().putBoolean(KeyDebugModeEnabled, value).apply()
    get() = sharedPreferences.getBoolean(KeyDebugModeEnabled, false)