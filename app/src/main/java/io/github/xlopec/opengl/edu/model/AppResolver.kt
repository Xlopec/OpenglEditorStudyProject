package io.github.xlopec.opengl.edu.model

import io.github.xlopec.tea.core.ResolveCtx
import io.github.xlopec.tea.core.Snapshot

interface AppResolver<Env> {

    fun Env.resolve(
        snapshot: Snapshot<Message, AppState, Command>,
        context: ResolveCtx<Message>,
    )

}

fun AppResolver(): AppResolver<Environment> = object : AppResolver<Environment> {

    override fun Environment.resolve(
        snapshot: Snapshot<Message, AppState, Command>,
        context: ResolveCtx<Message>,
    ) {
        snapshot.commands.forEach { command ->
            when (command) {
                is DoExportImage -> Unit
                is NotifyException -> Unit
                is NotifyImageExported -> Unit
                is NotifyTransformationApplied -> Unit
                is StoreDebugMode -> {
                    value.isDebugModeEnabled = command.isDebugModeEnabled
                }
            }
        }
    }
}