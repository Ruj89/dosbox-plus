package org.dosboxplus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import org.dosboxplus.data.GameRepository
import org.dosboxplus.domain.GameEntry
import org.dosboxplus.domain.LaunchCommand
import org.dosboxplus.domain.Profile
import org.dosboxplus.ui.EmulatorScreen
import org.dosboxplus.ui.ProfileEditorScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) { super.onCreate(state); setContent { MaterialTheme { DosboxPlusApp(GameRepository(this)) } } }
}

@Composable private fun DosboxPlusApp(repo: GameRepository) {
    var gameToRun by remember { mutableStateOf<GameEntry?>(null) }
    var editor by remember { mutableStateOf<EditorRequest?>(null) }
    when {
        gameToRun != null -> EmulatorScreen(gameToRun!!, repo) { gameToRun = null }
        editor != null -> {
            val request = editor!!
            val initial = request.game?.profileId?.let(repo::profile)
                ?: (if (request.game == null) repo.globalProfile() else null)
                ?: Profile(name = request.game?.title ?: "Globale")
            ProfileEditorScreen(initial, request.game?.title ?: "Globale", onSave = { profile ->
                if (request.game == null) repo.saveGlobalProfile(profile) else repo.assignProfile(request.game, profile)
                editor = null
            }, onClose = { editor = null })
        }
        else -> LibraryScreen(repo, launch = { gameToRun = it }, editGlobal = { editor = EditorRequest(null) }, editGame = { editor = EditorRequest(it) })
    }
}

private data class EditorRequest(val game: GameEntry?)

@Composable private fun LibraryScreen(repo: GameRepository, launch: (GameEntry) -> Unit, editGlobal: () -> Unit, editGame: (GameEntry) -> Unit) {
    var games by remember { mutableStateOf(repo.games()) }; var pendingUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }.onSuccess { pendingUri = uri }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("DOSBox Plus", style = MaterialTheme.typography.headlineMedium)
        Text("DOSBox libretro con controlli touch configurabili")
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { picker.launch(null) }) { Text("Importa cartella") }
            OutlinedButton(onClick = editGlobal) { Text("HUD globale") }
        }
        pendingUri?.let { SetupGame(it, repo) { games = repo.games(); pendingUri = null } }
        LazyColumn(Modifier.padding(top = 12.dp)) { items(games, key = { it.id }) { game ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f).clickable { launch(game) }) { Text(game.title, style = MaterialTheme.typography.titleMedium); Text("${game.command} ${game.arguments}") }; OutlinedButton(onClick = { editGame(game) }) { Text("HUD") }; Spacer(Modifier.width(6.dp)); Button(onClick = { launch(game) }) { Text("Avvia") } } }
        } }
    }
}

@Composable private fun SetupGame(uri: Uri, repo: GameRepository, done: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val names = remember(uri) { DocumentFile.fromTreeUri(context, uri)?.listFiles()?.mapNotNull { it.name } ?: emptyList() }
    val candidates = remember(names) { LaunchCommand.candidates(names) }; var title by remember { mutableStateOf("") }; var command by remember { mutableStateOf(candidates.firstOrNull() ?: "") }; var args by remember { mutableStateOf("") }
    Card(Modifier.fillMaxWidth().padding(top = 12.dp)) { Column(Modifier.padding(12.dp)) {
        Text("Configura gioco", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(title, { title = it }, label = { Text("Titolo") })
        OutlinedTextField(command, { command = it }, label = { Text("Comando (.exe/.bat/.com)") })
        OutlinedTextField(args, { args = it }, label = { Text("Argomenti") })
        Button(enabled = title.isNotBlank() && command.isNotBlank(), onClick = { repo.saveGame(GameEntry(title = title, treeUri = uri.toString(), command = command, arguments = args)); done() }) { Text("Salva") }
    } }
}
