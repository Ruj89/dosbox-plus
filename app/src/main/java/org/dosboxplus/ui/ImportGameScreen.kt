package org.dosboxplus.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dosboxplus.data.GameRepository
import org.dosboxplus.domain.GameEntry
import org.dosboxplus.domain.LaunchCommand
import org.dosboxplus.domain.ControlPresets

@Composable fun ImportGameScreen(repo: GameRepository, onClose: () -> Unit, onLaunch: (GameEntry) -> Unit, onEditControls: (GameEntry) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var treeUri by rememberSaveable { mutableStateOf<String?>(null) }
    var folderName by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var command by rememberSaveable { mutableStateOf("") }
    var arguments by rememberSaveable { mutableStateOf("") }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var presetId by rememberSaveable { mutableStateOf<String?>(null) }
    var presetPicker by rememberSaveable { mutableStateOf(false) }
    var savedId by rememberSaveable { mutableStateOf<String?>(null) }
    var candidates by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val savedGame = remember(savedId) { savedId?.let { id -> repo.games().firstOrNull { it.id == id } } }
    val preset = ControlPresets.find(presetId)
    val validCommand = command.isNotBlank() && LaunchCommand.candidates(listOf(command.trim())).isNotEmpty() && !command.contains('\n') && !command.contains('\r')
    fun goBack() { if (step == 1) step = 0 else onClose() }
    BackHandler { goBack() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                treeUri = uri.toString()
                title = ""; command = ""; arguments = ""; folderName = ""; candidates = emptyList()
                presetId = null
                error = null; step = 1; retry++
            } catch (_: Exception) {
                error = "Non riusciamo ad accedere a questa cartella. Scegline una nella memoria del telefono e consenti l'accesso."
            }
        }
    }
    LaunchedEffect(treeUri, retry) {
        if (step == 2) return@LaunchedEffect
        val uri = treeUri ?: return@LaunchedEffect
        loading = true
        error = null
        try {
            val result = withContext(Dispatchers.IO) {
                val folder = requireNotNull(DocumentFile.fromTreeUri(context, Uri.parse(uri)))
                require(folder.exists() && folder.canRead())
                (folder.name ?: "Il mio gioco") to LaunchCommand.candidates(folder.listFiles().filter { it.isFile }.mapNotNull { it.name })
            }
            folderName = result.first
            if (title.isBlank()) title = result.first
            candidates = result.second
            if (command.isBlank() && candidates.size == 1) command = candidates.single()
        } catch (exception: CancellationException) { throw exception
        } catch (_: Exception) {
            error = "Non è stato possibile leggere la cartella. Riprova oppure scegli una cartella diversa."
        } finally { loading = false }
    }

    Scaffold(
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = ::goBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Indietro")) }
                Text(if (step == 2) "Gioco aggiunto" else "Aggiungi un gioco", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onClose) { Text(if (step == 2) "Fine" else "Annulla") }
            }
        },
        bottomBar = {
            Surface(shadowElevation = 4.dp) {
                Box(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = {
                            when (step) {
                                0 -> if (treeUri != null && error == null) step = 1 else picker.launch(null)
                                1 -> {
                                    val game = GameEntry(title = title.trim(), treeUri = treeUri!!, command = command.trim(), arguments = arguments.trim())
                                    try {
                                        if (preset == null) repo.saveGame(game) else repo.assignProfile(game, preset.createProfile())
                                        savedId = game.id; step = 2
                                    }
                                    catch (_: Exception) { error = "Non è stato possibile salvare il gioco. Riprova." }
                                }
                                else -> savedGame?.let(onLaunch)
                            }
                        },
                        enabled = when (step) { 1 -> !loading && error == null && title.isNotBlank() && validCommand; 2 -> savedGame != null; else -> true },
                        modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().heightIn(min = 56.dp)
                    ) {
                        Text(when (step) { 0 -> if (treeUri != null && error == null) "Continua con questa cartella" else "Scegli la cartella del gioco"; 1 -> "Aggiungi alla libreria"; else -> "Gioca ora" })
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxWidth(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                item { ImportProgress(step) }
                error?.let { message -> item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
                            if (treeUri != null) TextButton(onClick = { retry++ }) { Text("Riprova") }
                            TextButton(onClick = { picker.launch(null) }) { Text("Cambia cartella") }
                        }
                    }
                } }
                when (step) {
                    0 -> {
                        item { SectionHeading("Partiamo dai file", "Scegli la cartella che contiene il gioco sul tuo telefono.") }
                        item { WelcomeArtwork() }
                        item { GuideStep("1", "Hai un archivio ZIP?", "Estrailo prima con il gestore file. Il gioco deve trovarsi in una normale cartella.") }
                        item { GuideStep("2", "Tieni tutti i file insieme", "Scegli la cartella del gioco, non un singolo file: servono anche gli altri dati per avviarlo.") }
                        item { GuideStep("3", "Consenti l'accesso alla cartella", "Android ti chiederà di usare la cartella selezionata. Ci serve per leggere i file del gioco.") }
                        if (treeUri != null) item {
                            OutlinedCard { Column(Modifier.padding(16.dp)) {
                                Text("Cartella selezionata: ${folderName.ifBlank { "Gioco" }}")
                                TextButton(onClick = { picker.launch(null) }) { Text("Scegli un'altra cartella") }
                            } }
                        }
                    }
                    1 -> {
                        item { SectionHeading("Come si avvia?", "Scegli il file che avvia il gioco e il nome da mostrare nella libreria.") }
                        item { Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(folderName.ifBlank { "Cartella selezionata" }, modifier = Modifier.weight(1f))
                                TextButton(onClick = { picker.launch(null) }) { Text("Cambia") }
                            }
                        } }
                        if (loading) item {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(24.dp)); Text("Cerchiamo i file di avvio…")
                            }
                        } else if (error == null) {
                            item { OutlinedTextField(title, { title = it }, label = { Text("Nome del gioco") }, supportingText = { Text("Sarà il titolo nella tua libreria.") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                            item {
                                Text("File di avvio", style = MaterialTheme.typography.titleMedium)
                                Text(if (candidates.isEmpty()) "Qui non ci sono file .exe, .bat o .com. Controlla di aver estratto il gioco e selezionato la cartella corretta."
                                    else "Se ce ne sono diversi, consulta le istruzioni del gioco. SETUP e INSTALL di solito servono alla configurazione.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                            }
                            items(candidates) { filename ->
                                Surface(shape = MaterialTheme.shapes.small, color = if (command == filename) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                                    Row(Modifier.fillMaxWidth().selectable(selected = command == filename, role = Role.RadioButton, onClick = { command = filename }).heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        RadioButton(selected = command == filename, onClick = null)
                                        androidx.compose.material3.Text(filename, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                            item {
                                TextButton(onClick = { advanced = !advanced }) { Text(if (advanced) "Nascondi opzioni avanzate" else "Opzioni avanzate") }
                                if (advanced) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(command, { command = it }, label = { Text("File o percorso relativo") }, supportingText = { Text("Per esempio DOS/GAME.EXE, se il file è in una sottocartella.") }, isError = command.isNotBlank() && !validCommand, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(arguments, { arguments = it }, label = { Text("Parametri di avvio (facoltativi)") }, supportingText = { Text("Lascia vuoto se le istruzioni del gioco non ne richiedono.") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                }
                            }
                            item {
                                OutlinedCard {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Controlli del gioco", style = MaterialTheme.typography.titleMedium)
                                        Text(preset?.title ?: "Usa i controlli comuni", color = MaterialTheme.colorScheme.primary)
                                        Text(preset?.description ?: "Puoi scegliere una configurazione pronta per un classico DOS oppure mantenere i tuoi controlli predefiniti.",
                                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        OutlinedButton(onClick = { presetPicker = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                                            Text(if (preset == null) "Scegli configurazione pronta" else "Cambia configurazione")
                                        }
                                        if (preset != null) TextButton(onClick = { presetId = null }) { Text("Usa invece i controlli comuni") }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        item {
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
                                Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text("Tutto pronto!", style = MaterialTheme.typography.headlineLarge)
                                    Text("${savedGame?.title ?: title} è nella tua libreria.", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                        item { GuideStep("1", "Tocca Gioca ora", preset?.let { "Controlli pronti: ${it.title}." } ?: "Il gioco si aprirà con i controlli predefiniti sullo schermo.") }
                        if (preset != null) item { Text(preset.instructions, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        item { GuideStep("2", "Personalizza quando vuoi", "Nella libreria, il pulsante Controlli ti permette di scegliere tasti e posizione solo per questo gioco.") }
                        item { OutlinedButton(onClick = { savedGame?.let(onEditControls) }, enabled = savedGame != null, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Prima personalizzo i controlli") } }
                        item { TextButton(onClick = onClose) { Text("Torna alla libreria") } }
                    }
                }
            }
        }
    }
    if (presetPicker) ControlPresetPicker(onDismiss = { presetPicker = false }, initialSelection = presetId, onChoose = {
        presetId = it.id; presetPicker = false
    })
}

@Composable private fun ImportProgress(step: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("PASSAGGIO ${step + 1} DI 3", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Cartella", "Avvio", "Pronto").forEachIndexed { index, name ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(progress = { if (index <= step) 1f else 0f }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    Text(name, style = MaterialTheme.typography.labelMedium, color = if (index == step) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
