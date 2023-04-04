package io.github.xlopec.opengl.edu.ui

import android.content.Intent
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.xlopec.opengl.edu.MimeTypePng
import io.github.xlopec.opengl.edu.R
import io.github.xlopec.opengl.edu.model.*
import io.github.xlopec.tea.core.Snapshot

@Composable
fun AppNotificationsHandler(
    snapshot: Snapshot<Message, AppState, Command>,
    scaffoldState: ScaffoldState,
) {
    val exceptionNotification: NotifyException? = snapshot.commands.firstInstanceOfOrNull()

    if (exceptionNotification != null) {
        val exceptionMessage =
            stringResource(R.string.notification_export_exception, exceptionNotification.th.message ?: "")
        LaunchedEffect(Unit) {
            scaffoldState.snackbarHostState.showSnackbar(
                message = exceptionMessage,
                duration = SnackbarDuration.Short
            )
        }
    }

    val exportNotification: NotifyImageExported? = snapshot.commands.firstInstanceOfOrNull()

    if (exportNotification != null) {
        val actionLabel = stringResource(R.string.action_open_image)
        val successMessage = stringResource(R.string.notification_export_success, exportNotification.filename)
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val action = scaffoldState.snackbarHostState.showSnackbar(
                message = successMessage,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Short
            )

            if (action == SnackbarResult.ActionPerformed) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    setDataAndType(exportNotification.path, MimeTypePng)
                }

                context.startActivity(intent)
            }
        }
    }
}

inline fun <reified C : A, A> Collection<A>.firstInstanceOfOrNull() = find { it is C } as? C