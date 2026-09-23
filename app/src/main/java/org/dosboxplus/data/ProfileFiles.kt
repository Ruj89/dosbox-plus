package org.dosboxplus.data

import android.content.ContentResolver
import android.net.Uri
import org.dosboxplus.domain.Profile
import org.dosboxplus.domain.ProfileCodec

object ProfileFiles {
    fun export(resolver: ContentResolver, uri: Uri, profile: Profile) {
        requireNotNull(resolver.openOutputStream(uri, "wt")) { "Impossibile aprire la destinazione" }.bufferedWriter(Charsets.UTF_8).use { it.write(ProfileCodec.encode(profile)) }
    }
    fun import(resolver: ContentResolver, uri: Uri): Profile =
        requireNotNull(resolver.openInputStream(uri)) { "Impossibile aprire il profilo" }.use {
            val bytes = it.readNBytesCompat(1024 * 1024 + 1)
            require(bytes.size <= 1024 * 1024) { "Il profilo supera 1 MB" }
            ProfileCodec.decode(bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF"))
        }

    private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (output.size() < limit) {
            val count = read(buffer, 0, minOf(buffer.size, limit - output.size()))
            if (count < 0) break
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
