package org.dosboxplus.data

import android.content.ContentResolver
import android.net.Uri
import org.dosboxplus.domain.Profile
import org.dosboxplus.domain.ProfileCodec

object ProfileFiles {
    fun export(resolver: ContentResolver, uri: Uri, profile: Profile) {
        requireNotNull(resolver.openOutputStream(uri)) { "Cannot open destination" }.bufferedWriter().use { it.write(ProfileCodec.encode(profile)) }
    }
    fun import(resolver: ContentResolver, uri: Uri): Profile = requireNotNull(resolver.openInputStream(uri)) { "Cannot open profile" }.bufferedReader().use { ProfileCodec.decode(it.readText()) }
}
