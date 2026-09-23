package org.dosboxplus.domain

/** Stored in profiles; bundled icons have stable IDs and imported pictures travel with the JSON. */
object ControlImages {
    const val maxBytes = 128 * 1024
    const val prefix = "data:image/png;base64,"
    val icons = listOf("gamepad", "fire", "run", "bolt", "map", "shield", "rocket", "star", "eye", "night", "save", "mouse")

    fun valid(image: String?): Boolean = image == null ||
        (image.startsWith("icon:") && image.removePrefix("icon:") in icons) ||
        (image.startsWith(prefix) && image.length <= prefix.length + (maxBytes + 2) / 3 * 4 &&
            image.substring(prefix.length).matches(Regex("[A-Za-z0-9+/]*={0,2}")))
}
