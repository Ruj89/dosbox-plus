package org.dosboxplus.data

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import java.io.File

/** Materializes a read-only SAF tree for unmodified DOSBox 0.74-3 mounting. */
object GameStager {
    fun stage(context: Context, gameId: String, treeUri: String): File {
        val source = requireNotNull(DocumentFile.fromTreeUri(context, android.net.Uri.parse(treeUri))) { "Invalid game directory" }
        val target = File(context.cacheDir, "games/$gameId").also { it.deleteRecursively(); it.mkdirs() }
        copy(context, source, target); return target
    }
    private fun copy(context: Context, source: DocumentFile, destination: File) {
        source.listFiles().forEach { child ->
            val out = File(destination, child.name ?: return@forEach)
            if (child.isDirectory) { out.mkdirs(); copy(context, child, out) }
            else context.contentResolver.openInputStream(child.uri)?.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }
}
