package org.dosboxplus.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.dosboxplus.R
import org.dosboxplus.domain.ControlImages
import java.io.ByteArrayOutputStream

internal val hudIcons = listOf(
    "gamepad" to R.drawable.hud_gamepad, "fire" to R.drawable.hud_fire,
    "run" to R.drawable.hud_run, "bolt" to R.drawable.hud_bolt,
    "map" to R.drawable.hud_map, "shield" to R.drawable.hud_shield,
    "rocket" to R.drawable.hud_rocket, "star" to R.drawable.hud_star,
    "eye" to R.drawable.hud_eye, "night" to R.drawable.hud_night,
    "save" to R.drawable.hud_save, "mouse" to R.drawable.hud_mouse
)

@Composable
internal fun HudArtwork(image: String?, modifier: Modifier = Modifier): Boolean {
    if (image == null) return false
    val resource = hudIcons.firstOrNull { image == "icon:${it.first}" }?.second
    val bitmap = remember(image) {
        if (!image.startsWith(ControlImages.prefix) || !ControlImages.valid(image)) null
        else runCatching {
            val bytes = Base64.decode(image.substring(ControlImages.prefix.length), Base64.DEFAULT)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth !in 1..256 || bounds.outHeight !in 1..256) null
            else BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
    if (resource == null && bitmap == null) return false
    Box(modifier, contentAlignment = Alignment.Center) {
        if (resource != null) Image(painterResource(resource), null, Modifier.fillMaxSize(.75f),
            contentScale = ContentScale.Fit, colorFilter = ColorFilter.tint(Color.White))
        else if (bitmap != null) Image(bitmap, null, Modifier.fillMaxSize(.85f), contentScale = ContentScale.Fit)
    }
    return true
}

/** Re-encode a bounded thumbnail as PNG so profiles can be exported without external file access. */
internal fun importHudArtwork(context: Context, uri: Uri): String {
    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            output.write(buffer, 0, count)
            require(output.size() <= 8 * 1024 * 1024) { "Immagine troppo grande (massimo 8 MB)." }
        }
        output.toByteArray()
    } ?: error("Impossibile aprire l'immagine.")
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    require(bounds.outWidth in 1..8192 && bounds.outHeight in 1..8192 && bounds.outMimeType?.startsWith("image/") == true) {
        "Scegli un'immagine PNG, JPEG o WebP valida."
    }
    val options = BitmapFactory.Options().apply {
        inSampleSize = generateSequence(1) { it * 2 }.first { bounds.outWidth / it <= 256 && bounds.outHeight / it <= 256 }
    }
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: error("Immagine non leggibile.")
    val output = ByteArrayOutputStream()
    try { decoded.compress(Bitmap.CompressFormat.PNG, 100, output) } finally { decoded.recycle() }
    require(output.size() <= ControlImages.maxBytes) { "Immagine troppo dettagliata; scegline una più semplice." }
    return ControlImages.prefix + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
}
