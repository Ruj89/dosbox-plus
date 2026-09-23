package org.dosboxplus.domain

class ControlPreset(
    val id: String,
    val title: String,
    val description: String,
    val instructions: String,
    private val build: () -> Profile
) {
    /** Each use owns its profile and control IDs; editing a game never changes a template. */
    fun createProfile(): Profile = build()
    fun applyTo(profile: Profile): Profile = createProfile().copy(id = profile.id, name = profile.name)
}

object ControlPresets {
    val all: List<ControlPreset> = listOf(
        preset("midnight-classic", "The Lords of Midnight · DOS classico",
            "Conversione di Chris Wild (MIDNIGHT.COM). Bussola, azioni ed eroi.",
            "Nuova avvia la partita. La bussola (1–8) cambia la direzione; Avanza (Q) fa un passo. " +
                "Usa Scegli per le azioni e Notte (U) per concludere il turno: il tempo non avanza da solo.", ::midnight),
        preset("midnight-citadel", "Lords of Midnight: The Citadel",
            "Edizione DOS 3D. Mouse, mappa, personaggi e controllo del tempo.",
            "Trascina lo sfondo per muovere il puntatore, poi usa Clic. Tempo (M) ferma o riavvia l'orologio; " +
                "Stop interrompe il movimento. I tasti F1–F8 aprono le sezioni del gioco.", ::citadel),
        preset("doom", "DOOM / DOOM II",
            "Frecce, fuoco, porte, corsa, spostamenti laterali e armi 1–7.",
            "Usa i comandi DOS originali. Tieni premuto Fuoco per sparare e Corsa insieme alle frecce per correre. " +
                "Usa apre porte e interruttori; Mappa mostra l'automappa.", ::doom),
        preset("wolfenstein", "Wolfenstein 3D",
            "Frecce, fuoco, apertura porte, corsa e armi 1–4.",
            "Fuoco usa Ctrl, Apri usa Spazio. Tieni premuto Laterale insieme alle frecce per spostarti di lato. " +
                "Le armi si scelgono con i pulsanti numerati.", ::wolfenstein),
        preset("prince", "Prince of Persia · DOS",
            "Movimento, azione, salti diagonali e salvataggio con Ctrl+G.",
            "Azione (Shift) serve per afferrare, raccogliere e combattere; con le frecce permette passi cauti. " +
                "Abilita tasti invia Ctrl+K. Salva funziona dal terzo livello; Carica si usa nella schermata iniziale.", ::prince),
        preset("keen4", "Commander Keen 4",
            "Frecce, salto, pogo e pistola stordente.",
            "Salta usa Ctrl, Pogo usa Alt e Fuoco usa Spazio. Tieni premuto Salta per un salto più alto. " +
                "Il preset usa i tasti originali con fuoco a due pulsanti disattivato.", ::keen),
        ControlPreset("standard", "Base universale",
            "Frecce, Spazio, Ctrl, Esc e tastiera virtuale.",
            "Una base semplice da adattare ai giochi che non hanno un preset dedicato.") { DefaultProfiles.create() }
    )

    fun find(id: String?): ControlPreset? = all.firstOrNull { it.id == id }

    private fun preset(id: String, title: String, description: String, instructions: String, layout: (Boolean) -> List<Control>) =
        ControlPreset(id, title, description, instructions) {
            Profile(name = title, portrait = layout(true), landscape = layout(false))
        }

    private data class Button(
        val label: String, val action: InputAction,
        val trigger: ControlTrigger = ControlTrigger.TAP, val type: ControlType = ControlType.BUTTON,
        val image: String? = null
    )
    private fun key(label: String, code: Int, hold: Boolean = false, icon: String? = null) =
        Button(label, InputAction.Key(code), if (hold) ControlTrigger.HOLD else ControlTrigger.TAP, image = icon?.let { "icon:$it" })
    private fun combo(label: String, vararg codes: Int) = Button(label, InputAction.Combo(codes.toList()))
    private fun mouse(label: String, button: Int) = Button(label, InputAction.MouseButton(button), type = ControlType.MOUSE)
    private val keyboard get() = Button("Tastiera", InputAction.Key(57), type = ControlType.KEYBOARD)

    private fun Button.at(x: Float, y: Float, width: Float, height: Float) = Control(
        type = type, x = x, y = y, width = width, height = height,
         label = label, action = action, trigger = trigger, opacity = .78f, image = image
    )

    private fun grid(buttons: List<Button>, columns: Int, x: Float, y: Float, width: Float, height: Float): List<Control> {
        val rows = (buttons.size + columns - 1) / columns
        val cellWidth = width / columns
        val cellHeight = height / rows
        return buttons.mapIndexed { index, button ->
            button.at(x + index % columns * cellWidth, y + index / columns * cellHeight,
                cellWidth - .012f, cellHeight - .014f)
        }
    }

    private fun toolbar(buttons: List<Button>, portrait: Boolean) = grid(
        buttons, if (portrait) 4 else 6, .02f, .02f, .96f, if (portrait) .25f else .28f
    )
    private fun movement(portrait: Boolean) = Control(
        type = ControlType.DPAD, x = .03f, y = if (portrait) .69f else .55f,
        width = if (portrait) .4f else .32f, height = if (portrait) .28f else .42f,
        action = InputAction.Combo(emptyList()), trigger = ControlTrigger.HOLD, opacity = .78f
    )
    private fun actionLayout(portrait: Boolean, actions: List<Button>, extra: List<Button> = emptyList()): List<Control> =
        toolbar(extra + listOf(key("Esc", 1), key("Invio", 28), keyboard), portrait) + movement(portrait) +
            grid(actions, 2, .53f, if (portrait) .65f else .54f, .45f, if (portrait) .33f else .44f)

    private fun doom(portrait: Boolean) = actionLayout(portrait,
        listOf(key("Fuoco", 29, true, "fire"), key("Usa", 57, icon = "gamepad"), key("Corsa", 42, true, "run"), key("Laterale", 56, true),
            key("Sposta ←", 51, true), key("Sposta →", 52, true)),
        (1..7).map { key("Arma $it", it + 1) } + key("Mappa", 15, icon = "map"))

    private fun wolfenstein(portrait: Boolean) = actionLayout(portrait,
        listOf(key("Fuoco", 29, true, "fire"), key("Apri", 57), key("Corsa", 54, true, "run"), key("Laterale", 56, true)),
        (1..4).map { key("Arma $it", it + 1) })

    private fun prince(portrait: Boolean) = actionLayout(portrait,
        listOf(key("Azione", 42, true, "shield"), key("Tempo", 57), key("Salto ↖", 0x147, true), key("Salto ↗", 0x149, true)),
        listOf(combo("Abilita tasti", 29, 37), combo("Salva", 29, 34), combo("Carica", 29, 38)))

    private fun keen(portrait: Boolean) = actionLayout(portrait,
        listOf(key("Salta", 29, true, "rocket"), key("Pogo", 56), key("Fuoco", 57, true, "bolt"), key("Stato", 28)))

    private fun midnight(portrait: Boolean): List<Control> {
        val top = toolbar(listOf(key("Luxor", 46), key("Morkin", 47), key("Corleth", 48), key("Rorthron", 49),
            key("Eroi", 50), key("Nuova", 30), key("Carica", 32), key("Salva", 31, icon = "save"), key("Esc", 1), keyboard), portrait)
        // A compass, not arrow keys: the DOS port uses 1=N, 2=NE, ... 8=NW.
        val compass = grid(listOf(key("NO · 8", 9), key("N · 1", 2), key("NE · 2", 3),
            key("O · 7", 8), key("Avanza", 16), key("E · 3", 4),
            key("SO · 6", 7), key("S · 5", 6), key("SE · 4", 5)),
            3, .02f, if (portrait) .68f else .54f, .47f, if (portrait) .3f else .44f)
         val actions = grid(listOf(key("Guarda", 18, icon = "eye"), key("Pensa", 19), key("Scegli", 20), key("Notte", 22, icon = "night"),
            key("Sì", 34), key("No", 36)), 2, .53f, if (portrait) .68f else .54f, .45f, if (portrait) .3f else .44f)
        return top + compass + actions
    }

    private fun citadel(portrait: Boolean): List<Control> {
        val sections = listOf("Almanacco", "Mappa", "Eroi", "Guida", "Qui e ora", "Compagnie", "Cronaca", "Alleanza")
            .mapIndexed { index, label -> key(label, 59 + index, icon = if (label == "Mappa") "map" else null) }
        val top = toolbar(sections + listOf(key("Indice", 1), key("Invio", 28), keyboard), portrait)
         return top + grid(listOf(mouse("Clic", 0), mouse("Destro", 1), key("Parla", 20), key("Tempo", 50),
            key("Stop", 57), key("Vai", 25), key("+1 ora", 67), key("Vista 1", 2)),
            4, .02f, if (portrait) .75f else .66f, .96f, if (portrait) .23f else .32f)
    }
}
