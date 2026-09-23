package org.dosboxplus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.dosboxplus.domain.*

@Composable fun ControlPresetPicker(
    onDismiss: () -> Unit,
    onChoose: (ControlPreset) -> Unit,
    initialSelection: String? = null,
    replacingLayouts: Boolean = false
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf(initialSelection) }
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    val selected = ControlPresets.find(selectedId)
    val matches = remember(query, locale) {
        val words = query.trim().split(Regex("\\s+"))
        ControlPresets.all.filter { preset -> words.all { word -> "${localized(context, preset.title)} ${localized(context, preset.description)}".contains(word, ignoreCase = true) } }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(12.dp).widthIn(max = 720.dp).fillMaxWidth().fillMaxHeight(.92f), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Configurazioni pronte", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Chiudi") }
                }
                OutlinedTextField(query, { query = it }, singleLine = true, label = { Text("Cerca un gioco") }, modifier = Modifier.fillMaxWidth())
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text(if (replacingLayouts) "Scegli un gioco: il preset sostituisce entrambi i layout. Nell'editor puoi annullare e personalizzare ogni pulsante."
                            else "Scegli i controlli per questo gioco. Ogni preset include una disposizione verticale e una orizzontale.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (matches.isEmpty()) item { Text("Nessun preset trovato. Prova un altro nome o scegli Base universale.") }
                    items(matches, key = { it.id }) { preset ->
                        val checked = preset.id == selectedId
                        Surface(shape = MaterialTheme.shapes.medium, color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
                            Column {
                                Row(Modifier.fillMaxWidth().selectable(selected = checked, role = Role.RadioButton, onClick = { selectedId = preset.id })
                                    .heightIn(min = 64.dp).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = checked, onClick = null)
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(preset.title, style = MaterialTheme.typography.titleSmall)
                                        Text(preset.description, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                if (checked) {
                                    val preview = remember(preset.id) { preset.createProfile() }
                                    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(preset.instructions, style = MaterialTheme.typography.bodyMedium)
                                        Text("Anteprima orizzontale · ${preview.landscape.size} controlli", style = MaterialTheme.typography.labelSmall)
                                        PresetPreview(preview.landscape)
                                    }
                                }
                            }
                        }
                    }
                }
                selected?.let { Text("Scelta: ${it.title}", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Button(onClick = { selected?.let(onChoose) }, enabled = selected != null, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Usa configurazione")
                }
            }
        }
    }
}

@Composable private fun PresetPreview(controls: List<Control>) {
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.background)) {
        controls.forEach { control ->
            Box(Modifier.offset(maxWidth * control.x, maxHeight * control.y)
                .size(maxWidth * control.width, maxHeight * control.height)
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) {
                Text(if (control.type == ControlType.DPAD) "✚" else control.label, fontSize = 9.sp, lineHeight = 10.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}
