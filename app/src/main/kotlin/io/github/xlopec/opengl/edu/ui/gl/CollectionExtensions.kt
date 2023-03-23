package io.github.xlopec.opengl.edu.ui.gl

/**
 * Unlike [List.forEachIndexed] this extension doesn't allocate iterator
 */
internal inline fun <T> List<T>.fastForEachIndexed(
    f: (index: Int, t: T) -> Unit,
) {
    var index = 0
    while (index < size) {
        f(index, this[index])
        index++
    }
}

internal inline fun <T, E> List<E>.fastFoldIndexed(
    initial: T,
    f: (index: Int, t: T, e: E) -> T,
): T {
    var acc = initial
    fastForEachIndexed { index, e ->
        acc = f(index, acc, e)
    }
    return acc
}

internal inline fun <T, E> List<E>.fastFold(
    initial: T,
    f: (t: T, e: E) -> T,
): T = fastFoldIndexed(initial) { _, t, e -> f(t, e) }