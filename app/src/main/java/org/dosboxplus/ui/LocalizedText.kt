package org.dosboxplus.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

/** Translate at render time without rewriting persisted control labels or preset data. */
internal fun localized(context: Context, text: String): String {
    if (context.resources.configuration.locales[0].language == "it") return text
    translations[text]?.let { return it }
    return when {
        text.startsWith("Cartella selezionata: ") -> "Selected folder: " + localized(context, text.removePrefix("Cartella selezionata: "))
        text.endsWith(" è nella tua libreria.") -> text.removeSuffix(" è nella tua libreria.") + " is in your library."
        text.startsWith("Controlli pronti: ") -> "Controls ready: " + localized(context, text.removePrefix("Controlli pronti: ").removeSuffix(".")) + "."
        text.startsWith("PASSAGGIO ") && text.endsWith(" DI 3") -> "STEP " + text.removePrefix("PASSAGGIO ").removeSuffix(" DI 3") + " OF 3"
        text.startsWith("Anteprima orizzontale · ") -> "Landscape preview · " + text.removePrefix("Anteprima orizzontale · ").replace(" controlli", " controls")
        text.startsWith("Scelta: ") -> "Selected: " + localized(context, text.removePrefix("Scelta: "))
        text.startsWith("Tasto: ") -> "Key: " + localized(context, text.removePrefix("Tasto: "))
        text.endsWith(" · Proprietà") -> localized(context, text.removeSuffix(" · Proprietà")) + " · Properties"
        text.startsWith("Arma ") -> "Weapon " + text.removePrefix("Arma ")
        text.startsWith("Codice ") -> "Code " + text.removePrefix("Codice ")
        text.startsWith("Mouse ") -> "Mouse " + localized(context, text.removePrefix("Mouse "))
        text.startsWith("Macro (") && text.endsWith(" passi)") -> "Macro (" + text.removePrefix("Macro (").removeSuffix(" passi)") + " steps)"
        text.startsWith("Importazione non riuscita: ") -> "Import failed: " + localized(context, text.removePrefix("Importazione non riuscita: "))
        text.startsWith("Esportazione non riuscita: ") -> "Export failed: " + localized(context, text.removePrefix("Esportazione non riuscita: "))
        text.endsWith(": controlli pronti. Puoi annullare la scelta o modificarli. Premi Salva per applicarli.") ->
            localized(context, text.substringBefore(": controlli pronti.")) + ": controls ready. You can undo or edit your choice. Tap Save to apply it."
        else -> text
    }
}

@Composable internal fun tr(text: String): String = localized(LocalContext.current, text)

// Keep the same call shape as Material Text for the screens in this package.
@Composable internal fun Text(
    text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified, fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null, letterSpacing: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified, maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip, style: TextStyle = androidx.compose.material3.LocalTextStyle.current
) {
    androidx.compose.material3.Text(localized(LocalContext.current, text), modifier, color, fontSize,
        fontWeight = fontWeight, fontFamily = fontFamily, letterSpacing = letterSpacing,
        lineHeight = lineHeight, maxLines = maxLines, overflow = overflow, style = style)
}

private val translations = mapOf(
    "Giochi" to "Games", "Controlli" to "Controls", "Guida" to "Help",
    "Bentornato nel mondo DOS" to "Welcome back to DOS",
    "Porta qui i giochi che hai già e gioca con i comandi sullo schermo." to "Bring your own games here and play with on-screen controls.",
    "Aggiungi il primo gioco" to "Add your first game", "I giochi non sono inclusi nell'app." to "Games are not included with the app.",
    "Scegli la cartella" to "Choose a folder", "Usa una cartella già estratta con i file del gioco." to "Use an extracted folder containing the game files.",
    "Scegli il file di avvio" to "Choose the launch file", "Ti mostreremo i file .exe, .bat e .com disponibili." to "We'll show the available .exe, .bat and .com files.",
    "Inizia a giocare" to "Start playing", "Potrai personalizzare i pulsanti anche in seguito." to "You can customize the buttons later too.",
    "La tua sala giochi" to "Your game library", "Aggiungi gioco" to "Add game",
    "Un gioco pronto. Scegli da dove ripartire." to "One game ready. Pick up where you left off.",
    "Giochi pronti: %d. Scegli da dove ripartire." to "%d games ready. Pick up where you left off.",
    "Per tutti i giochi" to "For all games",
    "Gioca a modo tuo" to "Play your way", "I controlli sono i pulsanti che tocchi sullo schermo durante il gioco." to "Controls are the buttons you tap on the screen while playing.",
    "Una base per tutti i giochi" to "One layout for all games", "Crea una disposizione predefinita. Verrà usata dai giochi che non hanno controlli personalizzati." to "Create a default layout for games without custom controls.",
    "Personalizza i controlli" to "Customize controls", "Parti da un gioco famoso" to "Start with a familiar game",
    "Nell'editor tocca Preset: trovi The Lords of Midnight, The Citadel, DOOM, Wolfenstein 3D, Prince of Persia e Commander Keen 4." to "Tap Preset in the editor to find The Lords of Midnight, The Citadel, DOOM, Wolfenstein 3D, Prince of Persia and Commander Keen 4.",
    "Dagli un posto e un nome" to "Place and name it", "Trascina il pulsante e scegli la scritta da mostrare." to "Drag the button and choose its label.",
    "Salva o condividi" to "Save or share", "Salva la disposizione. Dal menu File puoi importarla o esportarla." to "Save the layout. You can import or export it from the File menu.",
    "Ti serve una disposizione diversa?" to "Need a different layout?", "Apri Giochi e scegli Controlli sotto il gioco interessato. Le modifiche saranno solo per quel gioco." to "Open Games and choose Controls under a game. Changes will apply only to that game.",
    "Vai ai giochi" to "Go to games", "Ti diamo una mano" to "We're here to help", "I passaggi essenziali, sempre a portata di mano." to "The essentials, always at hand.",
    "Cosa serve per iniziare?" to "What do I need to get started?", "La cartella di un gioco DOS che hai già. Se hai un archivio ZIP, estrailo prima con il gestore file del telefono. Tieni insieme tutti i file del gioco." to "A folder containing a DOS game you own. If you have a ZIP archive, extract it with your phone's file manager first. Keep all the game files together.",
    "Quale file devo scegliere?" to "Which file should I choose?", "Il file che avvia il gioco, spesso con estensione .exe, .bat o .com. Il nome cambia da gioco a gioco: consulta le sue istruzioni se trovi più opzioni. SETUP e INSTALL di solito servono alla configurazione." to "The file that starts the game, usually ending in .exe, .bat or .com. If there are several options, check the game's instructions. SETUP and INSTALL are usually for configuration.",
    "Come funzionano i pulsanti?" to "How do the buttons work?", "I controlli sullo schermo simulano i tasti della tastiera. In Controlli puoi scegliere il tasto, cambiarne il nome e trascinarlo. Le disposizioni verticale e orizzontale si salvano separatamente." to "On-screen controls simulate keyboard keys. In Controls you can choose a key, rename the button and drag it. Portrait and landscape layouts are saved separately.",
    "Ho già una configurazione" to "I already have a layout", "Apri l'editor dei controlli, tocca File e poi Importa configurazione. Scegli il JSON e premi Salva. Con Esporta configurazione puoi condividere entrambi gli orientamenti." to "Open the controls editor, tap File, then Import layout. Choose the JSON file and tap Save. Export layout shares both orientations.",
    "Ci sono controlli già pronti?" to "Are there ready-made controls?", "Sì. Puoi sceglierli mentre aggiungi un gioco oppure aprire Controlli → Preset. The Lords of Midnight classico e The Citadel hanno due configurazioni distinte. Ogni scelta include entrambi gli orientamenti e rimane modificabile." to "Yes. Choose them when adding a game or open Controls → Preset. Classic The Lords of Midnight and The Citadel have separate layouts. Each includes both orientations and can be edited.",
    "Aggiungi un gioco" to "Add a game", "Controlli predefiniti" to "Default controls", "Controlli personalizzati" to "Custom controls", "Gioca" to "Play",
    "Indietro" to "Back", "Gioco aggiunto" to "Game added", "Fine" to "Done", "Annulla" to "Cancel",
    "Non riusciamo ad accedere a questa cartella. Scegline una nella memoria del telefono e consenti l'accesso." to "We can't access this folder. Choose one on your phone and grant access.",
    "Non è stato possibile leggere la cartella. Riprova oppure scegli una cartella diversa." to "Could not read the folder. Try again or choose another folder.",
    "Non è stato possibile salvare il gioco. Riprova." to "Could not save the game. Try again.",
    "Il mio gioco" to "My game", "Gioco" to "Game", "Continua con questa cartella" to "Continue with this folder", "Scegli la cartella del gioco" to "Choose the game folder",
    "Aggiungi alla libreria" to "Add to library", "Gioca ora" to "Play now", "Riprova" to "Try again", "Cambia cartella" to "Change folder",
    "Partiamo dai file" to "Start with the files", "Scegli la cartella che contiene il gioco sul tuo telefono." to "Choose the folder containing the game on your phone.",
    "Hai un archivio ZIP?" to "Have a ZIP archive?", "Estrailo prima con il gestore file. Il gioco deve trovarsi in una normale cartella." to "Extract it with your file manager first. The game must be in a regular folder.",
    "Tieni tutti i file insieme" to "Keep all files together", "Scegli la cartella del gioco, non un singolo file: servono anche gli altri dati per avviarlo." to "Choose the game folder, not an individual file: the other files are needed to run it.",
    "Consenti l'accesso alla cartella" to "Allow access to the folder", "Android ti chiederà di usare la cartella selezionata. Ci serve per leggere i file del gioco." to "Android will ask you to grant access to the selected folder so we can read the game files.",
    "Scegli un'altra cartella" to "Choose another folder", "Come si avvia?" to "How does it start?", "Scegli il file che avvia il gioco e il nome da mostrare nella libreria." to "Choose the file that starts the game and the name to show in your library.",
    "Cartella selezionata" to "Selected folder", "Cambia" to "Change", "Cerchiamo i file di avvio…" to "Looking for launch files…",
    "Nome del gioco" to "Game name", "Sarà il titolo nella tua libreria." to "This will be its title in your library.", "File di avvio" to "Launch file",
    "Qui non ci sono file .exe, .bat o .com. Controlla di aver estratto il gioco e selezionato la cartella corretta." to "No .exe, .bat or .com files found here. Make sure the game is extracted and you selected the right folder.",
    "Se ce ne sono diversi, consulta le istruzioni del gioco. SETUP e INSTALL di solito servono alla configurazione." to "If there are several, check the game's instructions. SETUP and INSTALL are usually for configuration.",
    "Nascondi opzioni avanzate" to "Hide advanced options", "Opzioni avanzate" to "Advanced options", "File o percorso relativo" to "File or relative path",
    "Per esempio DOS/GAME.EXE, se il file è in una sottocartella." to "For example DOS/GAME.EXE, if the file is in a subfolder.",
    "Parametri di avvio (facoltativi)" to "Launch arguments (optional)", "Lascia vuoto se le istruzioni del gioco non ne richiedono." to "Leave blank unless the game's instructions require them.",
    "Controlli del gioco" to "Game controls", "Usa i controlli comuni" to "Use shared controls",
    "Puoi scegliere una configurazione pronta per un classico DOS oppure mantenere i tuoi controlli predefiniti." to "Choose a ready-made layout for a DOS classic, or keep your default controls.",
    "Scegli configurazione pronta" to "Choose a ready-made layout", "Cambia configurazione" to "Change layout", "Usa invece i controlli comuni" to "Use shared controls instead",
    "Tutto pronto!" to "All set!", "Tocca Gioca ora" to "Tap Play now", "Il gioco si aprirà con i controlli predefiniti sullo schermo." to "The game will open with the default on-screen controls.",
    "Personalizza quando vuoi" to "Customize anytime", "Nella libreria, il pulsante Controlli ti permette di scegliere tasti e posizione solo per questo gioco." to "In the library, use Controls to customize the keys and their positions just for this game.",
    "Prima personalizzo i controlli" to "Customize controls first", "Torna alla libreria" to "Back to library", "Cartella" to "Folder", "Avvio" to "Launch", "Pronto" to "Ready",
    "Torna ai giochi" to "Back to games", "Avvio non riuscito" to "Launch failed", "Sessione terminata" to "Session ended",
    "Puoi riavviare il gioco oppure tornare alla libreria." to "You can restart the game or return to the library.", "Riavvia gioco" to "Restart game",
    "Prepariamo i file del gioco…" to "Preparing game files…", "La prima schermata apparirà al termine della preparazione." to "The first screen will appear when preparation is complete.",
    "Il motore DOSBox non è riuscito ad avviare il gioco. Riprova o torna alla libreria." to "DOSBox could not start the game. Try again or return to the library.",
    "Controlla che la cartella sia ancora disponibile e accessibile all'app, poi riprova." to "Make sure the folder is still available and accessible to the app, then try again.",
    "Chiudi tastiera" to "Close keyboard", "INVIO" to "ENTER", "Tastiera" to "Keyboard", "Spazio" to "Space",
    "C:\\> Sei pronto?" to "C:\\> Ready?", "I tuoi classici, un tocco alla volta." to "Your classics, one tap at a time.",
    "file non valido" to "invalid file", "destinazione non disponibile" to "destination unavailable",
    "Configurazione importata (verticale e orizzontale). Premi Salva per applicarla." to "Layout imported (portrait and landscape). Tap Save to apply it.",
    "Configurazione esportata con entrambi gli orientamenti." to "Layout exported with both orientations.",
    "Importazione non riuscita. Controlla il file e riprova." to "Import failed. Check the file and try again.",
    "Esportazione non riuscita. Scegli una destinazione diversa e riprova." to "Export failed. Choose another destination and try again.",
    "sinistro" to "left", "destro" to "right", "centrale" to "middle",
    "Configurazioni pronte" to "Ready-made layouts", "Chiudi" to "Close", "Cerca un gioco" to "Search for a game",
    "Scegli un gioco: il preset sostituisce entrambi i layout. Nell'editor puoi annullare e personalizzare ogni pulsante." to "Choose a game: the preset replaces both layouts. You can undo the choice and customize each button in the editor.",
    "Scegli i controlli per questo gioco. Ogni preset include una disposizione verticale e una orizzontale." to "Choose controls for this game. Every preset includes portrait and landscape layouts.",
    "Nessun preset trovato. Prova un altro nome o scegli Base universale." to "No presets found. Try another name or choose Universal basics.",
    "Usa configurazione" to "Use layout", "Configurazioni pronte…" to "Ready-made layouts…", "Importa configurazione…" to "Import layout…", "Esporta configurazione…" to "Export layout…",
    "Come funziona l'editor" to "How the editor works", "Salva" to "Save", "Orizzontale" to "Landscape", "Verticale" to "Portrait",
    "Schermo orizzontale" to "Landscape screen", "Schermo verticale" to "Portrait screen", "Annulla ultima modifica" to "Undo last change", "Ripristina modifica" to "Redo change",
    "Modifica" to "Edit", "Prova" to "Test", "Tocca un controllo per provarlo" to "Tap a control to test it",
    "Ora scegli il posto: tocca l'anteprima o trascina il pulsante." to "Now choose a spot: tap the preview or drag the button.",
    "Trascina per spostare. In Modifica puoi cambiare nome, tasto e dimensioni." to "Drag to move. In Edit you can change its name, key and size.",
    "Tocca un controllo per sceglierlo, oppure aggiungi un nuovo pulsante." to "Tap a control to select it, or add a new button.",
    "Scegli un controllo" to "Select a control", "Poi tocca Modifica per personalizzarlo." to "Then tap Edit to customize it.", "Fatto" to "Done",
    "I tuoi controlli, in 3 mosse" to "Your controls in 3 steps", "Scegli da dove partire" to "Choose a starting point",
    "Preset offre configurazioni pronte per i classici DOS. Con + Pulsante puoi aggiungere qualsiasi tasto." to "Preset offers ready-made layouts for DOS classics. Use + Button to add any key.",
    "Sistemalo sullo schermo" to "Place it on the screen", "Trascinalo dove ti è comodo. Tocca Modifica per cambiarne il nome e l'aspetto." to "Drag it wherever you like. Tap Edit to change its name and appearance.",
    "Prova e salva" to "Test and save", "Prova mostra l'azione assegnata. Salva applica entrambi i layout: verticale e orizzontale." to "Test shows the assigned action. Save applies both portrait and landscape layouts.",
    "Ho capito" to "Got it", "Operazione in corso…" to "Working…", "Pulsante" to "Button", "Altri" to "More",
    "Il tuo schermo, i tuoi pulsanti.\nScegli un Preset o tocca + Pulsante." to "Your screen, your buttons.\nChoose a Preset or tap + Button.", "ANTEPRIMA DELLO SCHERMO" to "SCREEN PREVIEW",
    "Selezionato" to "Selected", "Non selezionato" to "Not selected", "Prova controllo" to "Test control", "Seleziona controllo" to "Select control", "Trascina per ridimensionare" to "Drag to resize",
    "Un controllo alla volta" to "One control at a time", "Scegli" to "Select", "Tocca un controllo nell'anteprima o aggiungi un pulsante." to "Tap a control in the preview or add a button.",
    "Personalizza" to "Customize", "Qui potrai cambiarne nome, tasto e aspetto." to "Change its name, key and appearance here.", "Le modifiche saranno applicate quando premi Salva." to "Changes are applied when you tap Save.",
    "Proprietà" to "Properties", "Testo sul pulsante" to "Button label", "Es. Salta, Fuoco, ↑" to "E.g. Jump, Fire, ↑", "È la scritta che vedrai durante il gioco." to "This is the label shown during play.",
    "Sinistro" to "Left", "Destro" to "Right", "Centrale" to "Middle", "Visibilità" to "Visibility", "Mantieni premuto al tocco" to "Hold while touching",
    "Nascondi dimensioni e posizione" to "Hide size and position", "Dimensioni, posizione e livelli" to "Size, position and layers",
    "Posizione orizzontale" to "Horizontal position", "Posizione verticale" to "Vertical position", "Larghezza" to "Width", "Altezza" to "Height",
    "Se due controlli si sovrappongono, scegli quale mostrare davanti." to "If controls overlap, choose which appears on top.", "In fondo" to "Send to back", "In primo piano" to "Bring to front",
    "Duplica" to "Duplicate", "Elimina" to "Delete", "Aggiungi pulsante" to "Add button", "Assegna un tasto" to "Assign a key",
    "Cerca tasto o gruppo" to "Search for a key or group", "Scegli cosa inviare al gioco. Potrai cambiare la scritta sul pulsante in Modifica." to "Choose what to send to the game. You can change the button label in Edit.",
    "Nessun tasto trovato" to "No keys found", "Frecce direzionali ↑ ↓ ← →" to "Arrow keys ↑ ↓ ← →", "Apri tastiera virtuale" to "Open virtual keyboard",
    "Frecce direzionali" to "Arrow keys", "Levetta direzionale" to "Joystick",
    "Lettere" to "Letters", "Numeri" to "Numbers", "Comandi" to "Controls", "Navigazione" to "Navigation", "Funzione" to "Function",
    "Simboli (layout DOS US)" to "Symbols (US DOS layout)", "Tastierino numerico" to "Numeric keypad",
    "Invio" to "Enter", "Ctrl sinistro" to "Left Ctrl", "Ctrl destro" to "Right Ctrl", "Alt sinistro" to "Left Alt", "Alt destro (AltGr)" to "Right Alt (AltGr)",
    "Shift sinistro" to "Left Shift", "Shift destro" to "Right Shift", "Stampa schermo" to "Print Screen", "Pausa / Break" to "Pause / Break",
    "↑ Su" to "↑ Up", "↓ Giù" to "↓ Down", "← Sinistra" to "← Left", "→ Destra" to "→ Right", "Fine (End)" to "End",
    "Pagina su" to "Page Up", "Pagina giù" to "Page Down", "Inserisci" to "Insert", "Canc (Delete)" to "Delete", "Num Invio" to "Num Enter",
    "Base universale" to "Universal basics", "Frecce, Spazio, Ctrl, Esc e tastiera virtuale." to "Arrow keys, Space, Ctrl, Esc and virtual keyboard.",
    "Una base semplice da adattare ai giochi che non hanno un preset dedicato." to "A simple starting point for games without a dedicated preset.",
    "The Lords of Midnight · DOS classico" to "The Lords of Midnight · classic DOS",
    "Conversione di Chris Wild (MIDNIGHT.COM). Bussola, azioni ed eroi." to "Chris Wild's port (MIDNIGHT.COM). Compass, actions and heroes.",
    "Nuova avvia la partita. La bussola (1–8) cambia la direzione; Avanza (Q) fa un passo. Usa Scegli per le azioni e Notte (U) per concludere il turno: il tempo non avanza da solo." to "New starts the game. The compass (1–8) changes direction; Forward (Q) takes a step. Use Choose for actions and Night (U) to end the turn: time does not advance on its own.",
    "Edizione DOS 3D. Mouse, mappa, personaggi e controllo del tempo." to "3D DOS edition. Mouse, map, characters and time control.",
    "Trascina lo sfondo per muovere il puntatore, poi usa Clic. Tempo (M) ferma o riavvia l'orologio; Stop interrompe il movimento. I tasti F1–F8 aprono le sezioni del gioco." to "Drag the background to move the pointer, then tap Click. Time (M) pauses or resumes the clock; Stop stops movement. F1–F8 open game sections.",
    "Frecce, fuoco, porte, corsa, spostamenti laterali e armi 1–7." to "Arrows, fire, doors, run, strafe and weapons 1–7.",
    "Usa i comandi DOS originali. Tieni premuto Fuoco per sparare e Corsa insieme alle frecce per correre. Usa apre porte e interruttori; Mappa mostra l'automappa." to "Use the original DOS controls. Hold Fire to shoot and Run with the arrow keys to run. Use opens doors and switches; Map shows the automap.",
    "Frecce, fuoco, apertura porte, corsa e armi 1–4." to "Arrows, fire, open doors, run and weapons 1–4.",
    "Fuoco usa Ctrl, Apri usa Spazio. Tieni premuto Laterale insieme alle frecce per spostarti di lato. Le armi si scelgono con i pulsanti numerati." to "Fire uses Ctrl, Open uses Space. Hold Strafe with the arrow keys to move sideways. Choose weapons with the numbered buttons.",
    "Prince of Persia · DOS" to "Prince of Persia · DOS", "Movimento, azione, salti diagonali e salvataggio con Ctrl+G." to "Movement, action, diagonal jumps and saving with Ctrl+G.",
    "Azione (Shift) serve per afferrare, raccogliere e combattere; con le frecce permette passi cauti. Abilita tasti invia Ctrl+K. Salva funziona dal terzo livello; Carica si usa nella schermata iniziale." to "Action (Shift) lets you grab, pick up and fight; with the arrows it allows cautious steps. Enable keys sends Ctrl+K. Save works from level three; Load works on the title screen.",
    "Frecce, salto, pogo e pistola stordente." to "Arrows, jump, pogo and stun gun.",
    "Salta usa Ctrl, Pogo usa Alt e Fuoco usa Spazio. Tieni premuto Salta per un salto più alto. Il preset usa i tasti originali con fuoco a due pulsanti disattivato." to "Jump uses Ctrl, Pogo uses Alt and Fire uses Space. Hold Jump for a higher jump. This preset uses the original keys with two-button firing disabled.",
    "Fuoco" to "Fire", "Usa" to "Use", "Corsa" to "Run", "Laterale" to "Strafe", "Sposta ←" to "Move ←", "Sposta →" to "Move →",
    "Mappa" to "Map", "Apri" to "Open", "Azione" to "Action", "Tempo" to "Time", "Salto ↖" to "Jump ↖", "Salto ↗" to "Jump ↗",
    "Abilita tasti" to "Enable keys", "Carica" to "Load", "Salta" to "Jump", "Stato" to "Status", "Eroi" to "Heroes", "Nuova" to "New",
    "Avanza" to "Forward", "Guarda" to "Look", "Pensa" to "Think", "Scegli" to "Choose", "Notte" to "Night", "Sì" to "Yes", "No" to "No",
    "Almanacco" to "Almanac", "Qui e ora" to "Here and now", "Compagnie" to "Companies", "Cronaca" to "Chronicle", "Alleanza" to "Alliance",
    "Indice" to "Index", "Clic" to "Click", "Parla" to "Talk", "Vai" to "Go", "+1 ora" to "+1 hour", "Vista 1" to "View 1"
)
