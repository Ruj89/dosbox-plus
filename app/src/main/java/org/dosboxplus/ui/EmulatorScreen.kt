package org.dosboxplus.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dosboxplus.data.GameRepository
import org.dosboxplus.data.GameStager
import org.dosboxplus.domain.*
import org.dosboxplus.emulator.DosboxNative
import org.dosboxplus.emulator.EmulatorAudio
import org.dosboxplus.emulator.EmulatorView
import org.dosboxplus.input.InputDispatcher
import org.dosboxplus.input.NativeInput
import java.io.File

private val stagingMutex = Mutex()

@Composable fun EmulatorScreen(game: GameEntry, repo: GameRepository) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val profile = remember { game.profileId?.let(repo::profile) ?: repo.globalProfile() ?: DefaultProfiles.create() }
    val portrait = androidx.compose.ui.platform.LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val activeControls = if (portrait) profile.portrait else profile.landscape
    val dispatcher = remember { InputDispatcher { e -> when (e) { is NativeInput.Key -> DosboxNative.sendKey(e.code, e.down); is NativeInput.Mouse -> DosboxNative.sendMouseButton(e.button, e.down) } } }
    val audio = remember { EmulatorAudio() }
    var prepared by remember(game.id) { mutableStateOf(false) }
    var launchError by remember(game.id) { mutableStateOf<String?>(null) }
    var ended by remember(game.id) { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(game.id, attempt) {
        launchError = null
        ended = false
        prepared = false
        var started = false
        var filesReady = false
        var audioStarted = false
        try {
            val config = withContext(Dispatchers.IO) {
                // A cancelled copy must finish before another launch stages the same game.
                stagingMutex.withLock { writeRuntimeConfig(context, game) }
            }
            val system = File(context.filesDir, "system").also { it.mkdirs() }
            val saves = File(context.filesDir, "saves").also { it.mkdirs() }
            filesReady = true
            check(DosboxNative.start(config, system.absolutePath, saves.absolutePath))
            started = true
            while (isActive) {
                when (DosboxNative.state()) {
                    DosboxNative.STATE_STARTING -> Unit
                    DosboxNative.STATE_RUNNING -> {
                        if (!audioStarted) { audio.start(); audioStarted = true }
                        prepared = true
                    }
                    DosboxNative.STATE_STOPPED -> { ended = true; break }
                    else -> error("Il motore DOSBox non è riuscito ad avviarsi")
                }
                delay(50)
            }
        } catch (error: CancellationException) { throw error
        } catch (_: Exception) {
            launchError = if (filesReady) "Il motore DOSBox non è riuscito ad avviare il gioco. Riprova o torna alla libreria."
                else "Controlla che la cartella sia ancora disponibile e accessibile all'app, poi riprova."
        } finally {
            prepared = false
            if (audioStarted) audio.stop()
            if (started) DosboxNative.stop()
        }
    }
    DisposableEffect(game.id) {
        onDispose { audio.stop(); DosboxNative.stop() }
    }
    Box(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding(), contentAlignment = Alignment.Center) {
        if (!prepared) Column(Modifier.widthIn(max = 420.dp).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (launchError != null) {
                Text("Avvio non riuscito", style = MaterialTheme.typography.titleLarge)
                Text(launchError!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { attempt++ }) { Text("Riprova") }
            } else if (ended) {
                Text("Sessione terminata", style = MaterialTheme.typography.titleLarge)
                Text("Puoi riavviare il gioco oppure tornare alla libreria.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { attempt++ }) { Text("Riavvia gioco") }
            } else {
                CircularProgressIndicator()
                Text("Prepariamo i file del gioco…", style = MaterialTheme.typography.titleMedium)
                Text("La prima schermata apparirà al termine della preparazione.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            AndroidView(
                factory = { EmulatorView(it) },
                modifier = Modifier.fillMaxSize().background(Color.Black)
                    .pointerInput(Unit) { detectDragGestures { _, drag -> DosboxNative.sendMouseMotion(drag.x.toInt(), drag.y.toInt()) } }
            )
            TouchOverlay(activeControls, dispatcher)
        }
    }
}
private fun writeRuntimeConfig(context: Context, game: GameEntry): String {
    val file = File(context.cacheDir, "run-${game.id}.conf")
    val staged = GameStager.stage(context, game.id, game.treeUri)
    file.writeText("[autoexec]\nmount ${game.drive} \"${staged.absolutePath}\"\n${LaunchCommand.command(game)}\n")
    return file.absolutePath
}
@Composable private fun TouchOverlay(controls: List<Control>, dispatcher: InputDispatcher) {
    var keyboardVisible by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        controls.sortedBy { it.zIndex }.forEach { control ->
            val modifier = Modifier
                .offset(x = maxWidth * control.x, y = maxHeight * control.y)
                .size(width = maxWidth * control.width, height = maxHeight * control.height)
                .zIndex(control.zIndex.toFloat())
            when (control.type) {
                ControlType.DPAD, ControlType.JOYSTICK -> DirectionPad(control, modifier, dispatcher)
                else -> RuntimeHudButton(control, modifier, dispatcher) { keyboardVisible = true }
            }
        }
        if (keyboardVisible) VirtualDosKeyboard(dispatcher, { keyboardVisible = false }, Modifier.align(Alignment.BottomCenter).zIndex(1000f))
    }
}

@Composable private fun RuntimeHudButton(control: Control, modifier: Modifier, dispatcher: InputDispatcher, openKeyboard: () -> Unit) {
    val shape = if (control.type == ControlType.BUTTON) CircleShape else RoundedCornerShape(12.dp)
    Box(
        modifier.alpha(control.opacity).background(Color(0xff313944), shape)
            .pointerInput(control.id, control.action, control.trigger) {
                detectTapGestures(onPress = {
                    if (control.type == ControlType.KEYBOARD) {
                        if (tryAwaitRelease()) openKeyboard()
                    } else if (control.trigger == ControlTrigger.HOLD) {
                        dispatcher.press(control.action)
                        try { tryAwaitRelease() } finally { dispatcher.release(control.action) }
                    } else if (tryAwaitRelease()) dispatcher.execute(control.action)
                })
            }, contentAlignment = Alignment.Center
    ) {
        if (control.type != ControlType.BUTTON || !HudArtwork(control.image, Modifier.fillMaxSize()))
            Text(control.label.ifBlank { runtimeIcon(control.type) }, color = Color.White,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(4.dp))
    }
}

@Composable private fun DirectionPad(control: Control, modifier: Modifier, dispatcher: InputDispatcher) {
    Box(modifier.alpha(control.opacity)) {
        DirectionKey("↑", 72, Modifier.align(Alignment.TopCenter).fillMaxWidth(.34f).fillMaxHeight(.42f), dispatcher)
        DirectionKey("↓", 80, Modifier.align(Alignment.BottomCenter).fillMaxWidth(.34f).fillMaxHeight(.42f), dispatcher)
        DirectionKey("←", 75, Modifier.align(Alignment.CenterStart).fillMaxWidth(.42f).fillMaxHeight(.34f), dispatcher)
        DirectionKey("→", 77, Modifier.align(Alignment.CenterEnd).fillMaxWidth(.42f).fillMaxHeight(.34f), dispatcher)
    }
}

@Composable private fun DirectionKey(label: String, scanCode: Int, modifier: Modifier, dispatcher: InputDispatcher) {
    val action = remember(scanCode) { InputAction.Key(scanCode) }
    Box(modifier.background(Color(0xff313944), RoundedCornerShape(8.dp)).pointerInput(scanCode) {
        detectTapGestures(onPress = { dispatcher.press(action); try { tryAwaitRelease() } finally { dispatcher.release(action) } })
    }, contentAlignment = Alignment.Center) { Text(label, color = Color.White) }
}

private fun runtimeIcon(type: ControlType) = when (type) { ControlType.BUTTON -> "A"; ControlType.DPAD -> "✚"; ControlType.JOYSTICK -> "●"; ControlType.MOUSE -> "⌁"; ControlType.KEYBOARD -> "⌨" }

@Composable private fun VirtualDosKeyboard(dispatcher: InputDispatcher, close: () -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val rows = remember { listOf(
        listOf("ESC" to 1, "1" to 2, "2" to 3, "3" to 4, "4" to 5, "5" to 6, "6" to 7, "7" to 8, "8" to 9, "9" to 10, "0" to 11),
        listOf("Q" to 16, "W" to 17, "E" to 18, "R" to 19, "T" to 20, "Y" to 21, "U" to 22, "I" to 23, "O" to 24, "P" to 25),
        listOf("A" to 30, "S" to 31, "D" to 32, "F" to 33, "G" to 34, "H" to 35, "J" to 36, "K" to 37, "L" to 38, "INVIO" to 28),
        listOf("SHIFT" to 42, "Z" to 44, "X" to 45, "C" to 46, "V" to 47, "B" to 48, "N" to 49, "M" to 50, "←" to 75, "↑" to 72, "↓" to 80, "→" to 77),
        listOf("CTRL" to 29, "ALT" to 56, "SPACE" to 57, "TAB" to 15, "BACK" to 14)
    ) }
    Surface(modifier.fillMaxWidth().fillMaxHeight(.58f), color = Color(0xee151a21), tonalElevation = 8.dp) {
        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = close) { Text("Chiudi tastiera") } }
            rows.forEach { row -> Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                row.forEach { (label, code) ->
                    OutlinedButton(onClick = { scope.launch { dispatcher.execute(InputAction.Key(code)) } }, modifier = Modifier.weight(if (label == "SPACE") 3f else 1f).fillMaxHeight(), contentPadding = PaddingValues(1.dp)) { Text(label, maxLines = 1) }
                }
            } }
        }
    }
}
