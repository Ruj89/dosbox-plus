package org.dosboxplus.domain

import kotlinx.serialization.json.Json

object ProfileCodec {
    private val json = Json { ignoreUnknownKeys = false; prettyPrint = true; encodeDefaults = true }
    fun encode(profile: Profile): String = json.encodeToString(Profile.serializer(), profile)
    fun decode(text: String): Profile = json.decodeFromString(Profile.serializer(), text).also {
        require(it.version == 1) { "Unsupported input profile version ${it.version}" }
        (it.portrait + it.landscape).forEach { control ->
            require(control.x in 0f..1f && control.y in 0f..1f && control.width > 0f && control.height > 0f) { "Invalid control geometry" }
            require(control.opacity in 0f..1f) { "Invalid control opacity" }
        }
    }
}
