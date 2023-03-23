package io.github.xlopec.opengl.edu.model

interface Provider<out T> {
    val value: T
}

fun <T> StaticOf(
    value: T,
) = object : Provider<T> {
    override val value: T = value
}