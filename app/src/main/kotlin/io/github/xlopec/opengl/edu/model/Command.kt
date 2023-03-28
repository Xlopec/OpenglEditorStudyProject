package io.github.xlopec.opengl.edu.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import io.github.xlopec.opengl.edu.model.transformation.Transformation
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass

@Immutable
sealed interface Command

@JvmInline
value class DoExportImage(
    val filename: String = filename(Date()),
) : Command {
    companion object {
        private val DateFormatter = SimpleDateFormat("dd-MM-yyyy-HHmmss.SSS", Locale.US)

        fun filename(date: Date) = "${DateFormatter.format(date)}.png"
    }
}

data class NotifyImageExported(
    val filename: String,
    val path: Uri
) : Command

@JvmInline
value class NotifyException(
    val th: Throwable
) : Command

@JvmInline
value class NotifyTransformationApplied(
    val which: KClass<out Transformation>,
) : Command

@JvmInline
value class StoreDebugMode(
    val isDebugModeEnabled: Boolean
) : Command