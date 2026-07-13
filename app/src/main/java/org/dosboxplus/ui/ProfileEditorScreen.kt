package org.dosboxplus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.dosboxplus.domain.*
import java.util.UUID
import kotlin.math.roundToInt

private enum class HudOrientation { LANDSCAPE, PORTRAIT }

@Composable
fun ProfileEditorScreen(initial: Profile, title: String, onSave: (Profile) -> Unit, onClose: () -> Unit) {
    var profile by remember(initial.id) { mutableStateOf(initial) }
    var orientation by remember { mutableStateOf(HudOrientation.LANDSCAPE) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val undo = remember { mutableStateListOf<Profile>() }
    val redo = remember { mutableStateListOf<Profile>() }
    var testMode by remember { mutableStateOf(false) }

    fun controls(value: Profile = profile) = if (orientation == HudOrientation.LANDSCAPE) value.landscape else value.portrait
    fun withControls(value: List<Control>, source: Profile = profile) = if (orientation == HudOrientation.LANDSCAPE) source.copy(landscape = value) else source.copy(portrait = value)
    fun commit(next: Profile) {
        if (next == profile) return
        undo += profile
        if (undo.size > 80) undo.removeAt(0)
        redo.clear()
        profile = next
    }
    fun updateControl(next: Control) = commit(withControls(controls().map { if (it.id == next.id) next else it }))
    val selected = controls().firstOrNull { it.id == selectedId }

    Column(Modifier.fillMaxSize().background(Color(0xff101318))) {
        HudEditorTopBar(
            title = title, orientation = orientation, canUndo = undo.isNotEmpty(), canRedo = redo.isNotEmpty(), testMode = testMode,
            onClose = onClose, onOrientation = { orientation = it; selectedId = null },
            onUndo = { if (undo.isNotEmpty()) { redo += profile; profile = undo.removeAt(undo.lastIndex) } },
            onRedo = { if (redo.isNotEmpty()) { undo += profile; profile = redo.removeAt(redo.lastIndex) } },
            onTest = { testMode = !testMode }, onSave = { onSave(profile) }
        )
        Row(Modifier.weight(1f).fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ControlPalette(Modifier.width(126.dp).fillMaxHeight()) { type ->
                val created = newControl(type, controls().size)
                commit(withControls(controls() + created)); selectedId = created.id
            }
            HudCanvas(
                controls = controls(), orientation = orientation, selectedId = selectedId, testMode = testMode,
                modifier = Modifier.weight(1f).fillMaxHeight(), onSelect = { selectedId = it }, onUpdate = ::updateControl
            )
            ControlProperties(
                control = selected, modifier = Modifier.width(280.dp).fillMaxHeight(),
                onUpdate = ::updateControl,
                onDuplicate = { source -> val copy = source.copy(id = UUID.randomUUID().toString(), x = (source.x + .04f).coerceAtMost(.9f), y = (source.y + .04f).coerceAtMost(.9f), zIndex = controls().maxOfOrNull { it.zIndex }?.plus(1) ?: 0); commit(withControls(controls() + copy)); selectedId = copy.id },
                onDelete = { source -> commit(withControls(controls().filterNot { it.id == source.id })); selectedId = null }
            )
        }
        Surface(color = Color(0xff171c23)) {
            Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("Griglia 8 dp", color = Color.LightGray); Text("Aggancio ai bordi", color = Color.LightGray)
                Spacer(Modifier.weight(1f)); Text("${controls().size} controlli", color = Color.Gray)
            }
        }
    }
}

@Composable private fun HudEditorTopBar(
    title: String, orientation: HudOrientation, canUndo: Boolean, canRedo: Boolean, testMode: Boolean,
    onClose: () -> Unit, onOrientation: (HudOrientation) -> Unit, onUndo: () -> Unit, onRedo: () -> Unit,
    onTest: () -> Unit, onSave: () -> Unit
) {
    Surface(color = Color(0xff151a21)) { Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onClose) { Text("←") }; Text("HUD · $title", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.weight(1f))
        FilterChip(selected = orientation == HudOrientation.LANDSCAPE, onClick = { onOrientation(HudOrientation.LANDSCAPE) }, label = { Text("Landscape") })
        FilterChip(selected = orientation == HudOrientation.PORTRAIT, onClick = { onOrientation(HudOrientation.PORTRAIT) }, label = { Text("Portrait") })
        TextButton(enabled = canUndo, onClick = onUndo) { Text("↶") }; TextButton(enabled = canRedo, onClick = onRedo) { Text("↷") }
        OutlinedButton(onClick = onTest) { Text(if (testMode) "Modifica" else "Prova") }
        Button(onClick = onSave) { Text("Salva") }
    } }
}

@Composable private fun ControlPalette(modifier: Modifier, add: (ControlType) -> Unit) {
    Surface(modifier, color = Color(0xff171c23), shape = RoundedCornerShape(10.dp)) { Column(Modifier.padding(10.dp)) {
        Text("CONTROLLI", color = Color.LightGray, style = MaterialTheme.typography.labelMedium); Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) { items(ControlType.entries) { type ->
            OutlinedButton(onClick = { add(type) }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)) { Text(controlIcon(type)); Spacer(Modifier.width(5.dp)); Text(controlName(type), maxLines = 1) }
        } }
    } }
}

@Composable private fun HudCanvas(
    controls: List<Control>, orientation: HudOrientation, selectedId: String?, testMode: Boolean,
    modifier: Modifier, onSelect: (String?) -> Unit, onUpdate: (Control) -> Unit
) {
    Surface(modifier, color = Color(0xff080b0f), shape = RoundedCornerShape(10.dp)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val ratio = if (orientation == HudOrientation.LANDSCAPE) 16f / 9f else 9f / 16f
            BoxWithConstraints(Modifier.fillMaxSize(.94f).aspectRatio(ratio).clip(RoundedCornerShape(4.dp)).background(Color(0xff202733)).border(1.dp, Color(0xff398ee8), RoundedCornerShape(4.dp)).pointerInput(testMode) { if (!testMode) detectTapGestures { onSelect(null) } }) {
                Text("ANTEPRIMA GIOCO", color = Color(0xff536173), modifier = Modifier.align(Alignment.Center))
                controls.sortedBy { it.zIndex }.forEach { control ->
                    EditableHudControl(control, selected = control.id == selectedId, testMode = testMode, canvasWidthPx = constraints.maxWidth.toFloat(), canvasHeightPx = constraints.maxHeight.toFloat(), canvasWidthDp = maxWidth, canvasHeightDp = maxHeight, onSelect = { onSelect(control.id) }, onUpdate = onUpdate)
                }
            }
        }
    }
}

@Composable private fun BoxScope.EditableHudControl(
    control: Control, selected: Boolean, testMode: Boolean, canvasWidthPx: Float, canvasHeightPx: Float,
    canvasWidthDp: androidx.compose.ui.unit.Dp, canvasHeightDp: androidx.compose.ui.unit.Dp,
    onSelect: () -> Unit, onUpdate: (Control) -> Unit
) {
    val shape = if (control.type == ControlType.BUTTON || control.type == ControlType.JOYSTICK) CircleShape else RoundedCornerShape(12.dp)
    Box(
        Modifier.offset { IntOffset((control.x * canvasWidthPx).roundToInt(), (control.y * canvasHeightPx).roundToInt()) }
            .size(width = (control.width * canvasWidthDp.value).dp, height = (control.height * canvasHeightDp.value).dp)
            .alpha(control.opacity).clip(shape).background(if (selected) Color(0xff247fda) else Color(0xff303945))
            .then(if (selected) Modifier.border(2.dp, Color(0xff66b5ff), shape) else Modifier)
            .pointerInput(control.id, testMode) {
                if (!testMode) detectDragGestures(onDragStart = { onSelect() }) { change, drag ->
                    change.consume()
                    onUpdate(control.copy(x = (control.x + drag.x / canvasWidthPx).coerceIn(0f, 1f - control.width), y = (control.y + drag.y / canvasHeightPx).coerceIn(0f, 1f - control.height)))
                }
            }, contentAlignment = Alignment.Center
    ) {
        Text(control.label.ifBlank { controlIcon(control.type) }, color = Color.White)
        if (selected && !testMode) Box(
            Modifier.align(Alignment.BottomEnd).size(20.dp).background(Color(0xffd9ecff), RoundedCornerShape(4.dp))
                .pointerInput(control.id) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        onUpdate(control.copy(
                            width = (control.width + drag.x / canvasWidthPx).coerceIn(.06f, 1f - control.x),
                            height = (control.height + drag.y / canvasHeightPx).coerceIn(.06f, 1f - control.y)
                        ))
                    }
                }
        )
    }
}

@Composable private fun ControlProperties(
    control: Control?, modifier: Modifier, onUpdate: (Control) -> Unit,
    onDuplicate: (Control) -> Unit, onDelete: (Control) -> Unit
) {
    Surface(modifier, color = Color(0xff171c23), shape = RoundedCornerShape(10.dp)) {
        if (control == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Seleziona un controllo", color = Color.Gray) }
        else LazyColumn(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(controlName(control.type).uppercase(), color = Color.White, style = MaterialTheme.typography.titleMedium) }
            item { OutlinedTextField(value = control.label, onValueChange = { onUpdate(control.copy(label = it.take(12))) }, modifier = Modifier.fillMaxWidth(), label = { Text("Etichetta") }, singleLine = true) }
            item { KeySelector(control) { onUpdate(control.copy(action = InputAction.Key(it))) } }
            item { Text("Opacità ${(control.opacity * 100).roundToInt()}%", color = Color.LightGray); Slider(control.opacity, { onUpdate(control.copy(opacity = it)) }, valueRange = .2f..1f) }
            item { Text("Larghezza ${(control.width * 100).roundToInt()}%", color = Color.LightGray); Slider(control.width, { onUpdate(control.copy(width = it.coerceAtMost(1f - control.x))) }, valueRange = .06f..0.4f) }
            item { Text("Altezza ${(control.height * 100).roundToInt()}%", color = Color.LightGray); Slider(control.height, { onUpdate(control.copy(height = it.coerceAtMost(1f - control.y))) }, valueRange = .06f..0.4f) }
            item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Pressione prolungata", color = Color.LightGray, modifier = Modifier.weight(1f)); Switch(control.trigger == ControlTrigger.HOLD, { onUpdate(control.copy(trigger = if (it) ControlTrigger.HOLD else ControlTrigger.TAP)) }) } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { OutlinedButton(onClick = { onUpdate(control.copy(zIndex = control.zIndex - 1)) }, modifier = Modifier.weight(1f)) { Text("Sotto") }; OutlinedButton(onClick = { onUpdate(control.copy(zIndex = control.zIndex + 1)) }, modifier = Modifier.weight(1f)) { Text("Sopra") } } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { OutlinedButton(onClick = { onDuplicate(control) }, modifier = Modifier.weight(1f)) { Text("Duplica") }; Button(onClick = { onDelete(control) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xff8c2e35)), modifier = Modifier.weight(1f)) { Text("Elimina") } } }
        }
    }
}

@Composable private fun KeySelector(control: Control, choose: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }; val current = (control.action as? InputAction.Key)?.scanCode
    Column { Text("Azione", color = Color.LightGray); Box { OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text(keyChoices.firstOrNull { it.second == current }?.first ?: "Scegli tasto") }; DropdownMenu(open, { open = false }) { keyChoices.forEach { (name, code) -> DropdownMenuItem(text = { Text(name) }, onClick = { choose(code); open = false }) } } } }
}

private val keyChoices = listOf("SPACE" to 57, "CTRL" to 29, "ALT" to 56, "SHIFT" to 42, "INVIO" to 28, "ESC" to 1, "TAB" to 15, "SU" to 72, "GIÙ" to 80, "SINISTRA" to 75, "DESTRA" to 77, "A" to 30, "B" to 48, "C" to 46, "D" to 32, "E" to 18, "F" to 33, "Q" to 16, "R" to 19, "S" to 31, "W" to 17, "X" to 45, "Z" to 44, "F1" to 59, "F2" to 60, "F3" to 61, "F4" to 62)
private fun controlName(type: ControlType) = when (type) { ControlType.BUTTON -> "Pulsante"; ControlType.DPAD -> "D-pad"; ControlType.JOYSTICK -> "Joystick"; ControlType.MOUSE -> "Mouse"; ControlType.KEYBOARD -> "Tastiera" }
private fun controlIcon(type: ControlType) = when (type) { ControlType.BUTTON -> "A"; ControlType.DPAD -> "✚"; ControlType.JOYSTICK -> "●"; ControlType.MOUSE -> "⌁"; ControlType.KEYBOARD -> "⌨" }
private fun newControl(type: ControlType, index: Int) = Control(
    type = type, x = (.08f + index * .035f).coerceAtMost(.72f), y = (.12f + index * .035f).coerceAtMost(.7f),
    width = if (type == ControlType.DPAD || type == ControlType.JOYSTICK) .22f else .14f,
    height = if (type == ControlType.DPAD || type == ControlType.JOYSTICK) .28f else .18f,
    zIndex = index, label = if (type == ControlType.BUTTON) "A" else "", action = when (type) {
        ControlType.MOUSE -> InputAction.MouseButton(0)
        ControlType.DPAD, ControlType.JOYSTICK -> InputAction.Combo(emptyList())
        else -> InputAction.Key(57)
    }
)
