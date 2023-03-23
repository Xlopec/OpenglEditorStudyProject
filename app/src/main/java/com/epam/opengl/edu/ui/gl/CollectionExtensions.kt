package com.epam.opengl.edu.ui.gl

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