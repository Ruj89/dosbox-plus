package org.dosboxplus.domain

import kotlinx.serialization.json.Json

object ProfileCodec {
    private val json = Json { ignoreUnknownKeys = false; prettyPrint = true; encodeDefaults = true }
    fun encode(profile: Profile): String = json.encodeToString(Profile.serializer(), profile)
    fun decode(text: String): Profile = json.decodeFromString(Profile.serializer(), text).also {
        require(it.version == 1) { "Unsupported input profile version ${it.version}" }
        listOf(it.portrait, it.landscape).forEach { controls ->
            require(controls.map { control -> control.id }.distinct().size == controls.size) { "ID dei controlli duplicati" }
            controls.forEach { control ->
                require(control.id.isNotBlank()) { "ID del controllo mancante" }
                require(control.x in 0f..1f && control.y in 0f..1f &&
                    control.width > 0f && control.width <= 1f && control.height > 0f && control.height <= 1f &&
                    control.x + control.width <= 1.00001f && control.y + control.height <= 1.00001f
                ) { "Controllo fuori dall'area di gioco" }
                require(control.opacity in 0f..1f) { "Opacità del controllo non valida" }
                require(ControlImages.valid(control.image)) { "Immagine del controllo non valida" }
            }
        }
    }
}
