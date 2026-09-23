package org.dosboxplus.domain

/** Coordinates are fractions of the canvas, independent of screen size and density. */
fun Control.moveTo(left: Float, top: Float): Control = copy(
    x = left.coerceIn(0f, (1f - width).coerceAtLeast(0f)),
    y = top.coerceIn(0f, (1f - height).coerceAtLeast(0f))
)

fun Control.resizeTo(newWidth: Float, newHeight: Float): Control {
    val w = newWidth.coerceIn(.04f, 1f)
    val h = newHeight.coerceIn(.04f, 1f)
    return copy(width = w, height = h).moveTo(x, y)
}
