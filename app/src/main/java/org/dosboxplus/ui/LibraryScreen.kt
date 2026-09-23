package org.dosboxplus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.dosboxplus.data.GameRepository
import org.dosboxplus.domain.GameEntry

@Composable fun LibraryScreen(repo: GameRepository, section: Int, onSection: (Int) -> Unit, launch: (GameEntry) -> Unit, editGlobal: () -> Unit, editGame: (GameEntry) -> Unit) {
    var games by remember { mutableStateOf(repo.games()) }
    var addingGame by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = section != 0 && !addingGame) { onSection(0) }
    if (addingGame) {
        ImportGameScreen(repo, onClose = { games = repo.games(); addingGame = false; onSection(0) },
            onLaunch = { addingGame = false; onSection(0); launch(it) }, onEditControls = { addingGame = false; onSection(0); editGame(it) })
        return
    }
    Scaffold(
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
                    Text(">_", Modifier.padding(10.dp), color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
                }
                Text("DOSBox Plus", style = MaterialTheme.typography.titleLarge)
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                listOf("Giochi", "Controlli", "Guida").forEachIndexed { index, label ->
                    NavigationBarItem(selected = section == index, onClick = { onSection(index) },
                        icon = { Icon(when (index) { 0 -> Icons.AutoMirrored.Filled.List; 1 -> Icons.Default.Settings; else -> Icons.Default.Info }, contentDescription = null) }, label = { Text(label) })
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxWidth(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                when (section) {
                    0 -> {
                        if (games.isEmpty()) {
                            item { WelcomeArtwork() }
                            item { SectionHeading("Bentornato nel mondo DOS", "Porta qui i giochi che hai già e gioca con i comandi sullo schermo.") }
                            item {
                                Button(onClick = { addingGame = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Aggiungi il primo gioco")
                                }
                                Text("I giochi non sono inclusi nell'app.", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            item {
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                        GuideStep("1", "Scegli la cartella", "Usa una cartella già estratta con i file del gioco.")
                                        GuideStep("2", "Scegli il file di avvio", "Ti mostreremo i file .exe, .bat e .com disponibili.")
                                        GuideStep("3", "Inizia a giocare", "Potrai personalizzare i pulsanti anche in seguito.")
                                    }
                                }
                            }
                        } else {
                             item { SectionHeading("La tua sala giochi", if (games.size == 1) tr("Un gioco pronto. Scegli da dove ripartire.") else tr("Giochi pronti: %d. Scegli da dove ripartire.").format(games.size)) }
                            item { Button(onClick = { addingGame = true }, modifier = Modifier.heightIn(min = 48.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Aggiungi gioco") } }
                            items(games, key = { it.id }) { game -> GameCard(game, { launch(game) }, { editGame(game) }) }
                        }
                    }
                    1 -> {
                        item { SectionHeading("Gioca a modo tuo", "I controlli sono i pulsanti che tocchi sullo schermo durante il gioco.") }
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                    Text("Una base per tutti i giochi", style = MaterialTheme.typography.titleLarge)
                                    Text("Crea una disposizione predefinita. Verrà usata dai giochi che non hanno controlli personalizzati.")
                                    Button(onClick = editGlobal, modifier = Modifier.heightIn(min = 48.dp)) { Text("Personalizza i controlli") }
                                }
                            }
                        }
                        item { GuideStep("1", "Parti da un gioco famoso", "Nell'editor tocca Preset: trovi The Lords of Midnight, The Citadel, DOOM, Wolfenstein 3D, Prince of Persia e Commander Keen 4.") }
                        item { GuideStep("2", "Dagli un posto e un nome", "Trascina il pulsante e scegli la scritta da mostrare.") }
                        item { GuideStep("3", "Salva o condividi", "Salva la disposizione. Dal menu File puoi importarla o esportarla.") }
                        item {
                            OutlinedCard {
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Ti serve una disposizione diversa?", style = MaterialTheme.typography.titleMedium)
                                    Text("Apri Giochi e scegli Controlli sotto il gioco interessato. Le modifiche saranno solo per quel gioco.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(onClick = { onSection(0) }) { Text("Vai ai giochi") }
                                }
                            }
                        }
                    }
                    else -> {
                        item { SectionHeading("Ti diamo una mano", "I passaggi essenziali, sempre a portata di mano.") }
                        item { HelpCard("Cosa serve per iniziare?", "La cartella di un gioco DOS che hai già. Se hai un archivio ZIP, estrailo prima con il gestore file del telefono. Tieni insieme tutti i file del gioco.") }
                        item { HelpCard("Quale file devo scegliere?", "Il file che avvia il gioco, spesso con estensione .exe, .bat o .com. Il nome cambia da gioco a gioco: consulta le sue istruzioni se trovi più opzioni. SETUP e INSTALL di solito servono alla configurazione.") }
                        item { HelpCard("Come funzionano i pulsanti?", "I controlli sullo schermo simulano i tasti della tastiera. In Controlli puoi scegliere il tasto, cambiarne il nome e trascinarlo. Le disposizioni verticale e orizzontale si salvano separatamente.") }
                        item { HelpCard("Ho già una configurazione", "Apri l'editor dei controlli, tocca File e poi Importa configurazione. Scegli il JSON e premi Salva. Con Esporta configurazione puoi condividere entrambi gli orientamenti.") }
                        item { HelpCard("Ci sono controlli già pronti?", "Sì. Puoi sceglierli mentre aggiungi un gioco oppure aprire Controlli → Preset. The Lords of Midnight classico e The Citadel hanno due configurazioni distinte. Ogni scelta include entrambi gli orientamenti e rimane modificabile.") }
                        item { Button(onClick = { addingGame = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Aggiungi un gioco") } }
                    }
                }
            }
        }
    }
}

@Composable private fun GameCard(game: GameEntry, launch: () -> Unit, edit: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(16.dp)) {
                    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Text(game.title.take(2).uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    androidx.compose.material3.Text(game.title, style = MaterialTheme.typography.titleMedium)
                    androidx.compose.material3.Text(game.command, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(if (game.profileId == null) "Controlli predefiniti" else "Controlli personalizzati", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = edit, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Controlli") }
                Button(onClick = launch, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Gioca") }
            }
        }
    }
}

@Composable private fun HelpCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
