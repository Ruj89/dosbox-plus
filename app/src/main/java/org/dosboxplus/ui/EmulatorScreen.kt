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
import androidx.compose.ui.viewinterop.AndroidView
import org.dosboxplus.data.GameRepository
import org.dosboxplus.data.GameStager
import org.dosboxplus.domain.*
import org.dosboxplus.emulator.DosboxNative
import org.dosboxplus.emulator.EmulatorAudio
import org.dosboxplus.emulator.EmulatorView
import org.dosboxplus.input.InputDispatcher
import org.dosboxplus.input.NativeInput
import java.io.File

@Composable fun EmulatorScreen(game: GameEntry, repo: GameRepository, close: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val profile = remember { game.profileId?.let(repo::profile) ?: repo.globalProfile() ?: Profile(name = "Default", portrait = defaultControls(), landscape = defaultControls()) }
    val portrait = androidx.compose.ui.platform.LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val activeControls = if (portrait) profile.portrait else profile.landscape
    val dispatcher = remember { InputDispatcher { e -> when (e) { is NativeInput.Key -> DosboxNative.sendKey(e.code, e.down); is NativeInput.Mouse -> DosboxNative.sendMouseButton(e.button, e.down) } } }
    val audio = remember { EmulatorAudio() }
    DisposableEffect(game) {
        val config = writeRuntimeConfig(context, game)
        val system = File(context.filesDir, "system").also { it.mkdirs() }
        val saves = File(context.filesDir, "saves").also { it.mkdirs() }
        DosboxNative.start(config, system.absolutePath, saves.absolutePath)
        audio.start()
        onDispose { audio.stop(); DosboxNative.stop() }
    }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(game.title, color = Color.White); TextButton(onClick = close) { Text("Esci") } }
        Box(Modifier.weight(1f).fillMaxWidth()) {
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
private fun defaultControls() = listOf(
    Control(type = ControlType.BUTTON, x=.05f, y=.2f, width=.16f, height=.16f, label="↑", action=InputAction.Key(72)),
    Control(type = ControlType.BUTTON, x=.72f, y=.2f, width=.2f, height=.16f, label="Ctrl", action=InputAction.Key(29)),
    Control(type = ControlType.BUTTON, x=.72f, y=.42f, width=.2f, height=.16f, label="Space", action=InputAction.Key(57))
)
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
    ) { Text(control.label.ifBlank { runtimeIcon(control.type) }, color = Color.White) }
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
                    OutlinedButton(onClick = { dispatcher.press(InputAction.Key(code)); dispatcher.release(InputAction.Key(code)) }, modifier = Modifier.weight(if (label == "SPACE") 3f else 1f).fillMaxHeight(), contentPadding = PaddingValues(1.dp)) { Text(label, maxLines = 1) }
                }
            } }
        }
    }
}
