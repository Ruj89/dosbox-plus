package org.dosboxplus.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dosboxplus.data.ProfileFiles
import org.dosboxplus.domain.*
import java.util.UUID
import kotlin.math.roundToInt

private enum class HudOrientation { LANDSCAPE, PORTRAIT }
private val profileSaver = Saver<Profile, String>(save = { ProfileCodec.encode(it) }, restore = ProfileCodec::decode)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(initial: Profile, title: String, onSave: (Profile) -> Unit, onClose: () -> Unit) {
    var profile by rememberSaveable(initial.id, stateSaver = profileSaver) { mutableStateOf(initial) }
    var orientation by rememberSaveable { mutableStateOf(HudOrientation.LANDSCAPE) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var placementId by rememberSaveable { mutableStateOf<String?>(null) }
    val undo = remember { mutableStateListOf<Profile>() }
    val redo = remember { mutableStateListOf<Profile>() }
    var gestureStart by remember { mutableStateOf<Profile?>(null) }
    var testMode by rememberSaveable { mutableStateOf(false) }
    var testFeedback by remember { mutableStateOf("Tocca un controllo per provarlo") }
    var keyPicker by remember { mutableStateOf(false) }
    var presetPicker by rememberSaveable { mutableStateOf(false) }
    var fileMenu by remember { mutableStateOf(false) }
    var orientationMenu by remember { mutableStateOf(false) }
    var propertiesOpen by rememberSaveable { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var exportSnapshot by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    fun controls(value: Profile = profile) = if (orientation == HudOrientation.LANDSCAPE) value.landscape else value.portrait
    fun withControls(value: List<Control>) = if (orientation == HudOrientation.LANDSCAPE) profile.copy(landscape = value) else profile.copy(portrait = value)
    fun rememberUndo(previous: Profile) {
        undo += previous
        if (undo.size > 80) undo.removeAt(0)
        redo.clear()
    }
    fun commit(next: Profile) {
        if (next == profile) return
        if (gestureStart == null) rememberUndo(profile)
        profile = next
    }
    fun beginGesture() { if (gestureStart == null) gestureStart = profile }
    fun endGesture() {
        gestureStart?.let { if (it != profile) rememberUndo(it) }
        gestureStart = null
    }
    fun cancelGesture() { gestureStart?.let { profile = it }; gestureStart = null }
    fun updateControl(next: Control) = commit(withControls(controls().map { if (it.id == next.id) next else it }))
    fun addControl(type: ControlType, key: KeyboardKey? = null) {
        val created = newControl(type, (controls().maxOfOrNull { it.zIndex } ?: -1) + 1, key)
        commit(withControls(controls() + created))
        selectedId = created.id
        placementId = created.id
    }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val message = try {
                val imported = withContext(Dispatchers.IO) { ProfileFiles.import(context.contentResolver, uri) }
                // Import into this editor without overwriting another game's profile identity.
                commit(imported.copy(id = profile.id))
                selectedId = null
                placementId = null
                "Configurazione importata (verticale e orizzontale). Premi Salva per applicarla."
            } catch (error: CancellationException) { throw error
            } catch (_: Exception) { "Importazione non riuscita. Controlla il file e riprova."
            } finally { busy = false }
            snackbar.showSnackbar(localized(context, message))
        }
    }
    val exportFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val snapshot = exportSnapshot
        exportSnapshot = null
        if (uri != null && snapshot != null) scope.launch {
            busy = true
            val message = try {
                withContext(Dispatchers.IO) { ProfileFiles.export(context.contentResolver, uri, ProfileCodec.decode(snapshot)) }
                "Configurazione esportata con entrambi gli orientamenti."
            } catch (error: CancellationException) { throw error
            } catch (_: Exception) { "Esportazione non riuscita. Scegli una destinazione diversa e riprova."
            } finally { busy = false }
            snackbar.showSnackbar(localized(context, message))
        }
    }
    val selected = controls().firstOrNull { it.id == selectedId }

    Surface(color = MaterialTheme.colorScheme.background) {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                     IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Torna alla libreria")) }
                    Column(Modifier.weight(1f)) {
                        Text("Controlli", style = MaterialTheme.typography.titleMedium)
                        androidx.compose.material3.Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box {
                        TextButton(onClick = { fileMenu = true }) { Text("File") }
                        DropdownMenu(expanded = fileMenu, onDismissRequest = { fileMenu = false }) {
                            DropdownMenuItem(text = { Text("Configurazioni pronte…") }, onClick = { fileMenu = false; presetPicker = true })
                            DropdownMenuItem(text = { Text("Importa configurazione…") }, onClick = {
                                fileMenu = false
                                endGesture()
                                importFile.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
                            })
                            DropdownMenuItem(text = { Text("Esporta configurazione…") }, onClick = {
                                fileMenu = false
                                endGesture()
                                exportSnapshot = ProfileCodec.encode(profile)
                                exportFile.launch("hud-${profile.name.replace(Regex("[^\\p{L}\\p{N}_-]"), "_")}.json")
                            })
                            DropdownMenuItem(text = { Text("Come funziona l'editor") }, onClick = { fileMenu = false; showHelp = true })
                        }
                    }
                    Button(onClick = { endGesture(); onSave(profile) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Salva") }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        TextButton(onClick = { orientationMenu = true }) {
                            Text(if (orientation == HudOrientation.LANDSCAPE) "Orizzontale" else "Verticale")
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = orientationMenu, onDismissRequest = { orientationMenu = false }) {
                            HudOrientation.entries.forEach { value ->
                                DropdownMenuItem(text = { Text(if (value == HudOrientation.LANDSCAPE) "Schermo orizzontale" else "Schermo verticale") }, onClick = {
                                    endGesture(); orientation = value; selectedId = null; placementId = null; orientationMenu = false
                                })
                            }
                        }
                    }
                    IconButton(enabled = undo.isNotEmpty(), onClick = {
                        endGesture(); redo += profile; profile = undo.removeAt(undo.lastIndex); placementId = null
                    }, modifier = Modifier.semantics { contentDescription = localized(context, "Annulla ultima modifica") }) { Text("↶", style = MaterialTheme.typography.headlineSmall) }
                    IconButton(enabled = redo.isNotEmpty(), onClick = {
                        endGesture(); undo += profile; profile = redo.removeAt(redo.lastIndex); placementId = null
                    }, modifier = Modifier.semantics { contentDescription = localized(context, "Ripristina modifica") }) { Text("↷", style = MaterialTheme.typography.headlineSmall) }
                    OutlinedButton(onClick = { endGesture(); testMode = !testMode; placementId = null }) { Text(if (testMode) "Modifica" else "Prova") }
                }
                if (!testMode) ControlPalette(onAddKey = { keyPicker = true }, onAdd = { addControl(it) }, onPresets = { presetPicker = true })
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) { Text(
                    when {
                        testMode -> testFeedback
                        placementId != null -> "Ora scegli il posto: tocca l'anteprima o trascina il pulsante."
                        selected != null -> "Trascina per spostare. In Modifica puoi cambiare nome, tasto e dimensioni."
                        else -> "Tocca un controllo per sceglierlo, oppure aggiungi un nuovo pulsante."
                    }, modifier = Modifier.fillMaxWidth().padding(10.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer
                ) }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
                    val properties: @Composable (Modifier) -> Unit = { modifier ->
                        ControlProperties(selected, modifier, ::updateControl, ::beginGesture, ::endGesture,
                            onDuplicate = { source ->
                                val copy = source.copy(id = UUID.randomUUID().toString(), zIndex = (controls().maxOfOrNull { it.zIndex } ?: 0) + 1).moveTo(source.x + .04f, source.y + .04f)
                                commit(withControls(controls() + copy)); selectedId = copy.id; placementId = copy.id
                            },
                            onDelete = { source -> commit(withControls(controls().filterNot { it.id == source.id })); selectedId = null; placementId = null; propertiesOpen = false },
                            onLayer = { source, front ->
                                val ordered = controls().sortedBy { it.zIndex }.filterNot { it.id == source.id }.toMutableList()
                                ordered.add(if (front) ordered.size else 0, source)
                                commit(withControls(ordered.mapIndexed { index, control -> control.copy(zIndex = index) }))
                            })
                    }
                    val canvas: @Composable (Modifier) -> Unit = { modifier ->
                        HudCanvas(controls(), orientation, selectedId, testMode, modifier,
                            onSelect = { selectedId = it },
                            onPlace = { x, y ->
                                val pending = controls().firstOrNull { it.id == placementId }
                                if (pending != null) {
                                    updateControl(pending.moveTo(x - pending.width / 2, y - pending.height / 2)); placementId = null
                                } else selectedId = null
                            },
                            onUpdate = ::updateControl, onGestureStart = { placementId = null; beginGesture() },
                            onGestureEnd = ::endGesture, onGestureCancel = ::cancelGesture,
                             onTest = { testFeedback = "${localized(context, it.label.ifBlank { controlName(it.type) })} · ${localized(context, actionDescription(it))}" })
                    }
                    if (maxWidth >= 840.dp) {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            canvas(Modifier.weight(1f).fillMaxHeight())
                            if (!testMode) properties(Modifier.width(290.dp).fillMaxHeight())
                        }
                    } else {
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            canvas(Modifier.weight(1f).fillMaxWidth())
                            if (!testMode) Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
                                Row(Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text(selected?.let { it.label.ifBlank { controlName(it.type) } } ?: "Scegli un controllo", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                                        Text(selected?.let(::actionDescription) ?: "Poi tocca Modifica per personalizzarlo.", maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    OutlinedButton(enabled = selected != null, onClick = { propertiesOpen = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Modifica") }
                                }
                            }
                        }
                        if (propertiesOpen && selected != null && !testMode) {
                            ModalBottomSheet(onDismissRequest = { endGesture(); propertiesOpen = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
                                TextButton(onClick = { endGesture(); propertiesOpen = false }, modifier = Modifier.align(Alignment.End).padding(end = 16.dp)) { Text("Fatto") }
                                properties(Modifier.fillMaxWidth().fillMaxHeight(.85f).imePadding())
                            }
                        }
                    }
                }
            }
        }
        if (keyPicker) KeyboardKeyDialog("Aggiungi pulsante", onDismiss = { keyPicker = false }) {
            addControl(ControlType.BUTTON, it); keyPicker = false
        }
        if (presetPicker) ControlPresetPicker(onDismiss = { presetPicker = false }, replacingLayouts = true, onChoose = { preset ->
            endGesture()
            commit(preset.applyTo(profile))
            selectedId = null; placementId = null; propertiesOpen = false; presetPicker = false; testMode = false
             scope.launch { snackbar.showSnackbar(localized(context, "${preset.title}: controlli pronti. Puoi annullare la scelta o modificarli. Premi Salva per applicarli.")) }
        })
        if (showHelp) AlertDialog(onDismissRequest = { showHelp = false }, title = { Text("I tuoi controlli, in 3 mosse") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                GuideStep("1", "Scegli da dove partire", "Preset offre configurazioni pronte per i classici DOS. Con + Pulsante puoi aggiungere qualsiasi tasto.")
                GuideStep("2", "Sistemalo sullo schermo", "Trascinalo dove ti è comodo. Tocca Modifica per cambiarne il nome e l'aspetto.")
                GuideStep("3", "Prova e salva", "Prova mostra l'azione assegnata. Salva applica entrambi i layout: verticale e orizzontale.")
            }
        }, confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Ho capito") } })
        if (busy) Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(); Text("Operazione in corso…")
                }
            }
        }
    }
}

@Composable private fun ControlPalette(onAddKey: () -> Unit, onAdd: (ControlType) -> Unit, onPresets: () -> Unit) {
    var otherControls by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onAddKey, modifier = Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 10.dp)) { Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Pulsante") }
        OutlinedButton(onClick = onPresets, modifier = Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 10.dp)) { Text("Preset") }
        Box {
            OutlinedButton(onClick = { otherControls = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Altri"); Icon(Icons.Default.KeyboardArrowDown, null) }
            DropdownMenu(expanded = otherControls, onDismissRequest = { otherControls = false }) {
                ControlType.entries.filterNot { it == ControlType.BUTTON }.forEach { type ->
                    DropdownMenuItem(text = { Text(controlName(type)) }, onClick = { onAdd(type); otherControls = false })
                }
            }
        }
    }
}

@Composable private fun HudCanvas(
    controls: List<Control>, orientation: HudOrientation, selectedId: String?, testMode: Boolean,
    modifier: Modifier, onSelect: (String?) -> Unit, onPlace: (Float, Float) -> Unit,
    onUpdate: (Control) -> Unit, onGestureStart: () -> Unit, onGestureEnd: () -> Unit,
    onGestureCancel: () -> Unit, onTest: (Control) -> Unit
) {
    val place by rememberUpdatedState(onPlace)
    Surface(modifier, color = Color(0xff080b0f), shape = RoundedCornerShape(10.dp)) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
            val ratio = if (orientation == HudOrientation.LANDSCAPE) 16f / 9f else 9f / 16f
            val width = minOf(maxWidth, maxHeight * ratio)
            val height = width / ratio
            BoxWithConstraints(Modifier.size(width, height).clip(RoundedCornerShape(4.dp))
                .background(Color(0xff202b38)).border(1.dp, MaterialTheme.colorScheme.outline)
                .pointerInput(testMode) { if (!testMode) detectTapGestures { place(it.x / size.width, it.y / size.height) } }) {
                Text(if (controls.isEmpty()) "Il tuo schermo, i tuoi pulsanti.\nScegli un Preset o tocca + Pulsante." else "ANTEPRIMA DELLO SCHERMO", color = Color(0xffb2c3d6), style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.Center).padding(16.dp))
                controls.sortedBy { it.zIndex }.forEach { control ->
                    key(control.id) {
                        EditableHudControl(control, control.id == selectedId, testMode,
                            constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat(), maxWidth, maxHeight,
                            onSelect = { onSelect(control.id) }, onUpdate, onGestureStart, onGestureEnd, onGestureCancel, onTest)
                    }
                }
            }
        }
    }
}

@Composable private fun EditableHudControl(
    control: Control, selected: Boolean, testMode: Boolean, canvasWidthPx: Float, canvasHeightPx: Float,
    canvasWidth: Dp, canvasHeight: Dp, onSelect: () -> Unit, onUpdate: (Control) -> Unit,
    onGestureStart: () -> Unit, onGestureEnd: () -> Unit, onGestureCancel: () -> Unit, onTest: (Control) -> Unit
) {
    val context = LocalContext.current
    val current by rememberUpdatedState(control)
    val update by rememberUpdatedState(onUpdate)
    val select by rememberUpdatedState(onSelect)
    val start by rememberUpdatedState(onGestureStart)
    val end by rememberUpdatedState(onGestureEnd)
    val cancel by rememberUpdatedState(onGestureCancel)
    val test by rememberUpdatedState(onTest)
    var pressed by remember { mutableStateOf(false) }
    val shape = if (control.type == ControlType.BUTTON || control.type == ControlType.JOYSTICK) CircleShape else RoundedCornerShape(12.dp)
    Box(Modifier.offset { IntOffset((control.x * canvasWidthPx).roundToInt(), (control.y * canvasHeightPx).roundToInt()) }
        .size(canvasWidth * control.width, canvasHeight * control.height).zIndex(control.zIndex.toFloat())) {
        Box(Modifier.fillMaxSize().alpha(if (selected && !testMode) maxOf(.4f, control.opacity) else control.opacity)
            .background(if (pressed) Color(0xff258650) else if (selected && !testMode) Color(0xff276557) else Color(0xff303945), shape)
            .border(if (selected && !testMode) 2.dp else 1.dp, if (selected) Color(0xff80e0ce) else Color(0xff8494a6), shape)
            .semantics(mergeDescendants = true) {
                 contentDescription = "${localized(context, control.label.ifBlank { controlName(control.type) })}: ${localized(context, actionDescription(control))}"
                 stateDescription = localized(context, if (selected) "Selezionato" else "Non selezionato")
                 onClick(label = localized(context, if (testMode) "Prova controllo" else "Seleziona controllo")) {
                    if (testMode) test(current) else select()
                    true
                }
            }
            .pointerInput(control.id, testMode) {
                detectTapGestures(onTap = { if (!testMode) select() }, onPress = {
                    if (testMode) {
                        pressed = true; test(current)
                        try { tryAwaitRelease() } finally { pressed = false }
                    }
                })
            }
            .pointerInput(control.id, testMode, canvasWidthPx, canvasHeightPx) {
                if (!testMode) {
                    var dragged = current
                    detectDragGestures(onDragStart = { dragged = current; select(); start() }, onDragEnd = { end() }, onDragCancel = { cancel() }) { change, drag ->
                        change.consume()
                        dragged = dragged.moveTo(dragged.x + drag.x / canvasWidthPx, dragged.y + drag.y / canvasHeightPx)
                        update(dragged)
                    }
                }
            }, contentAlignment = Alignment.Center) {
            if (!HudArtwork(control.image, Modifier.fillMaxSize()))
                Text(control.label.ifBlank { controlIcon(control.type) }, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(4.dp))
        }
        if (selected && !testMode) Box(Modifier.align(Alignment.BottomEnd).size(32.dp)
            .background(Color(0xffd9ecff), RoundedCornerShape(6.dp)).semantics { contentDescription = localized(context, "Trascina per ridimensionare") }
            .pointerInput(control.id, canvasWidthPx, canvasHeightPx) {
                var resized = current
                detectDragGestures(onDragStart = { resized = current; start() }, onDragEnd = { end() }, onDragCancel = { cancel() }) { change, drag ->
                    change.consume()
                    resized = resized.resizeTo(
                        (resized.width + drag.x / canvasWidthPx).coerceAtMost(1f - resized.x),
                        (resized.height + drag.y / canvasHeightPx).coerceAtMost(1f - resized.y))
                    update(resized)
                }
            }, contentAlignment = Alignment.Center) { Text("↘", color = Color(0xff101318)) }
    }
}

@Composable private fun ControlProperties(
    control: Control?, modifier: Modifier, onUpdate: (Control) -> Unit, onEditStart: () -> Unit, onEditEnd: () -> Unit,
    onDuplicate: (Control) -> Unit, onDelete: (Control) -> Unit, onLayer: (Control, Boolean) -> Unit
) {
    var chooseKey by remember(control?.id) { mutableStateOf(false) }
    var advanced by rememberSaveable(control?.id) { mutableStateOf(false) }
    var imageError by remember(control?.id) { mutableStateOf<String?>(null) }
    var pendingImageId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentControl by rememberUpdatedState(control)
    val currentUpdate by rememberUpdatedState(onUpdate)
    val chooseImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            imageError = null
            try {
                val image = withContext(Dispatchers.IO) { importHudArtwork(context, uri) }
                currentControl?.takeIf { it.id == pendingImageId }?.let { currentUpdate(it.copy(image = image)) }
            } catch (error: CancellationException) { throw error
            } catch (error: Exception) { imageError = error.message ?: "Immagine non leggibile." }
        }
    }
    Surface(modifier, color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
        if (control == null) Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Un controllo alla volta", style = MaterialTheme.typography.titleMedium)
            GuideStep("1", "Scegli", "Tocca un controllo nell'anteprima o aggiungi un pulsante.")
            GuideStep("2", "Personalizza", "Qui potrai cambiarne nome, tasto e aspetto.")
            GuideStep("3", "Salva", "Le modifiche saranno applicate quando premi Salva.")
        }
        else key(control.id) {
            LazyColumn(Modifier.padding(horizontal = 14.dp), contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("${controlName(control.type)} · Proprietà", style = MaterialTheme.typography.titleMedium) }
                item { OutlinedTextField(value = control.label, onValueChange = { onUpdate(control.copy(label = it)) }, modifier = Modifier.fillMaxWidth(), label = { Text("Testo sul pulsante") }, placeholder = { Text("Es. Salta, Fuoco, ↑") }, supportingText = { Text(if (control.image == null) "È la scritta che vedrai durante il gioco." else "Con un'immagine, il testo identifica il pulsante nell'editor.") }, singleLine = true) }
                if (control.type == ControlType.BUTTON) item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Immagine sul pulsante", style = MaterialTheme.typography.titleSmall)
                        Text("Scegli un'icona o una tua immagine. Il nome resta disponibile per riconoscere il pulsante.", style = MaterialTheme.typography.bodySmall)
                        hudIcons.chunked(6).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                row.forEach { (name, drawable) ->
                                    val id = "icon:$name"
                                    Box(Modifier.weight(1f).sizeIn(minHeight = 48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (control.image == id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { onUpdate(control.copy(image = id)); imageError = null }
                                        .semantics { contentDescription = "Icona $name" }, contentAlignment = Alignment.Center) {
                                        Icon(androidx.compose.ui.res.painterResource(drawable), contentDescription = null, modifier = Modifier.size(26.dp))
                                    }
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { pendingImageId = control.id; chooseImage.launch(arrayOf("image/png", "image/jpeg", "image/webp")) }) { Text("Scegli immagine…") }
                            TextButton(enabled = control.image != null, onClick = { onUpdate(control.copy(image = null)); imageError = null }) { Text("Solo testo") }
                        }
                        if (imageError != null) Text(imageError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
                item {
                    when (control.type) {
                        ControlType.BUTTON -> OutlinedButton(onClick = { chooseKey = true }, modifier = Modifier.fillMaxWidth()) { Text("Tasto: ${actionDescription(control)}") }
                        ControlType.MOUSE -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Sinistro", "Destro", "Centrale").forEachIndexed { index, label ->
                                FilterChip(selected = control.action == InputAction.MouseButton(index), onClick = { onUpdate(control.copy(action = InputAction.MouseButton(index))) }, label = { Text(label) })
                            }
                        }
                        else -> Text(actionDescription(control), style = MaterialTheme.typography.bodySmall)
                    }
                }
                item { PropertySlider("Visibilità", control.opacity, 0f..1f, onEditStart, onEditEnd) { onUpdate(control.copy(opacity = it)) } }
                if (control.type == ControlType.BUTTON || control.type == ControlType.MOUSE) item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Mantieni premuto al tocco", modifier = Modifier.weight(1f))
                        Switch(control.trigger == ControlTrigger.HOLD, { onUpdate(control.copy(trigger = if (it) ControlTrigger.HOLD else ControlTrigger.TAP)) })
                    }
                }
                item { TextButton(onClick = { advanced = !advanced }) { Text(if (advanced) "Nascondi dimensioni e posizione" else "Dimensioni, posizione e livelli") } }
                if (advanced) {
                    item { PropertySlider("Posizione orizzontale", control.x, 0f..1f, onEditStart, onEditEnd) { onUpdate(control.moveTo(it, control.y)) } }
                    item { PropertySlider("Posizione verticale", control.y, 0f..1f, onEditStart, onEditEnd) { onUpdate(control.moveTo(control.x, it)) } }
                    item { PropertySlider("Larghezza", control.width, .04f..1f, onEditStart, onEditEnd) { onUpdate(control.resizeTo(it, control.height)) } }
                    item { PropertySlider("Altezza", control.height, .04f..1f, onEditStart, onEditEnd) { onUpdate(control.resizeTo(control.width, it)) } }
                    item { Text("Se due controlli si sovrappongono, scegli quale mostrare davanti.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { onLayer(control, false) }, modifier = Modifier.weight(1f)) { Text("In fondo") }
                    OutlinedButton(onClick = { onLayer(control, true) }, modifier = Modifier.weight(1f)) { Text("In primo piano") }
                    } }
                }
                item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { onDuplicate(control) }, modifier = Modifier.weight(1f)) { Text("Duplica") }
                    Button(onClick = { onDelete(control) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xff8c2e35)), modifier = Modifier.weight(1f)) { Text("Elimina") }
                } }
            }
        }
    }
    if (chooseKey && control != null) KeyboardKeyDialog("Assegna un tasto", onDismiss = { chooseKey = false }) { key ->
        val oldDefault = (control.action as? InputAction.Key)?.let { KeyboardKeys.label(it.scanCode) }
        onUpdate(control.copy(action = InputAction.Key(key.scanCode), label = if (control.label.isBlank() || control.label == oldDefault) key.label else control.label))
        chooseKey = false
    }
}

@Composable private fun PropertySlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, start: () -> Unit, end: () -> Unit, update: (Float) -> Unit) {
    Text("${tr(label)} ${(value * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall)
    Slider(value.coerceIn(range), onValueChange = { start(); update(it) }, onValueChangeFinished = end, valueRange = range)
}

@Composable private fun KeyboardKeyDialog(title: String, onDismiss: () -> Unit, onChoose: (KeyboardKey) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    val matches = remember(query, locale) {
        val common = listOf(57, 28, 1, 29, 56, 42, 72, 80, 75, 77)
        KeyboardKeys.all.filter { localized(context, it.label).contains(query.trim(), ignoreCase = true) || localized(context, it.group).contains(query.trim(), ignoreCase = true) }
            .sortedBy { common.indexOf(it.scanCode).let { index -> if (index < 0) common.size else index } }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } }, text = {
        Column {
            OutlinedTextField(query, { query = it }, label = { Text("Cerca tasto o gruppo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Scegli cosa inviare al gioco. Potrai cambiare la scritta sul pulsante in Modifica.", modifier = Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodySmall)
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                if (matches.isEmpty()) item { Text("Nessun tasto trovato") }
                items(matches, key = { it.scanCode }) { key ->
                    Row(Modifier.fillMaxWidth().clickable { onChoose(key) }.heightIn(min = 48.dp).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(key.label, modifier = Modifier.weight(1f))
                        Text(key.group, style = MaterialTheme.typography.labelSmall, modifier = Modifier.widthIn(max = 110.dp))
                    }
                }
            }
        }
    })
}

private fun actionDescription(control: Control): String = when (control.type) {
    ControlType.DPAD, ControlType.JOYSTICK -> "Frecce direzionali ↑ ↓ ← →"
    ControlType.KEYBOARD -> "Apri tastiera virtuale"
    else -> when (val action = control.action) {
        is InputAction.Key -> KeyboardKeys.label(action.scanCode)
        is InputAction.MouseButton -> "Mouse ${listOf("sinistro", "destro", "centrale").getOrNull(action.button) ?: action.button}"
        is InputAction.Combo -> action.scanCodes.joinToString(" + ") { KeyboardKeys.label(it) }
        is InputAction.Macro -> "Macro (${action.steps.size} passi)"
        is InputAction.JoystickButton -> "Joystick ${action.button}"
    }
}
private fun controlName(type: ControlType) = when (type) { ControlType.BUTTON -> "Pulsante"; ControlType.DPAD -> "Frecce direzionali"; ControlType.JOYSTICK -> "Levetta direzionale"; ControlType.MOUSE -> "Mouse"; ControlType.KEYBOARD -> "Tastiera" }
private fun controlIcon(type: ControlType) = when (type) { ControlType.BUTTON -> "A"; ControlType.DPAD -> "✚"; ControlType.JOYSTICK -> "●"; ControlType.MOUSE -> "⌁"; ControlType.KEYBOARD -> "⌨" }
private fun newControl(type: ControlType, layer: Int, key: KeyboardKey?) = Control(
    type = type, x = .4f, y = .4f,
    width = if (type == ControlType.DPAD || type == ControlType.JOYSTICK) .22f else .16f,
    height = if (type == ControlType.DPAD || type == ControlType.JOYSTICK) .28f else .18f,
    zIndex = layer, label = key?.label ?: "", action = when (type) {
        ControlType.MOUSE -> InputAction.MouseButton(0)
        ControlType.DPAD, ControlType.JOYSTICK -> InputAction.Combo(emptyList())
        else -> InputAction.Key(key?.scanCode ?: 57)
    }
)
