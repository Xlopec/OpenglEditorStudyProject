package com.epam.opengl.edu.model

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

        }
    }
}