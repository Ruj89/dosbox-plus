package org.dosboxplus.data

import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.dosboxplus.domain.GameEntry
import org.dosboxplus.domain.Profile
import org.dosboxplus.domain.ProfileCodec

class GameRepository(context: Context) {
    private val prefs = context.getSharedPreferences("dosbox-plus", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = false }
    fun games(): List<GameEntry> = prefs.getString("games", "[]")?.let { json.decodeFromString(ListSerializer(GameEntry.serializer()), it) } ?: emptyList()
    fun saveGame(game: GameEntry) { saveGames(games().filterNot { it.id == game.id } + game) }
    fun deleteGame(id: String) { saveGames(games().filterNot { it.id == id }) }
    fun globalProfile(): Profile? = prefs.getString("global-profile", null)?.let(ProfileCodec::decode)
    fun saveGlobalProfile(profile: Profile) { prefs.edit().putString("global-profile", ProfileCodec.encode(profile)).apply() }
    fun saveProfile(profile: Profile) { prefs.edit().putString("profile-${profile.id}", ProfileCodec.encode(profile)).apply() }
    fun profile(id: String): Profile? = prefs.getString("profile-$id", null)?.let(ProfileCodec::decode)
    fun assignProfile(game: GameEntry, profile: Profile) {
        saveProfile(profile)
        saveGame(game.copy(profileId = profile.id))
    }
    private fun saveGames(value: List<GameEntry>) { prefs.edit().putString("games", json.encodeToString(ListSerializer(GameEntry.serializer()), value)).apply() }
}
