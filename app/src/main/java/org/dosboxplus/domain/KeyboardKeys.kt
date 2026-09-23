package org.dosboxplus.domain

data class KeyboardKey(val label: String, val scanCode: Int, val group: String)

/** PC scan codes, with Linux extended codes and tagged keypad digits (0x100 + scan code).
 * The latter preserve the arrow bindings (72/75/77/80) used by existing profiles.
 * Keep in sync with scan_code_to_retro in libretro_host.cpp.
 */
object KeyboardKeys {
    val all: List<KeyboardKey> = buildList {
        fun group(name: String, vararg keys: Pair<String, Int>) {
            keys.forEach { (label, code) -> add(KeyboardKey(label, code, name)) }
        }
        group("Lettere", "A" to 30, "B" to 48, "C" to 46, "D" to 32, "E" to 18,
            "F" to 33, "G" to 34, "H" to 35, "I" to 23, "J" to 36, "K" to 37,
            "L" to 38, "M" to 50, "N" to 49, "O" to 24, "P" to 25, "Q" to 16,
            "R" to 19, "S" to 31, "T" to 20, "U" to 22, "V" to 47, "W" to 17,
            "X" to 45, "Y" to 21, "Z" to 44)
        group("Numeri", "0" to 11, "1" to 2, "2" to 3, "3" to 4, "4" to 5,
            "5" to 6, "6" to 7, "7" to 8, "8" to 9, "9" to 10)
        group("Comandi", "Spazio" to 57, "Invio" to 28, "Esc" to 1, "Tab" to 15,
            "Backspace" to 14, "Ctrl sinistro" to 29, "Ctrl destro" to 97,
            "Alt sinistro" to 56, "Alt destro (AltGr)" to 100,
            "Shift sinistro" to 42, "Shift destro" to 54, "Caps Lock" to 58,
            "Num Lock" to 69, "Scroll Lock" to 70, "Stampa schermo" to 99,
            "Pausa / Break" to 119)
        group("Navigazione", "↑ Su" to 72, "↓ Giù" to 80, "← Sinistra" to 75,
            "→ Destra" to 77, "Home" to 102, "Fine (End)" to 107,
            "Pagina su" to 104, "Pagina giù" to 109, "Inserisci" to 110, "Canc (Delete)" to 111)
        (1..12).forEach { add(KeyboardKey("F$it", if (it <= 10) 58 + it else 76 + it, "Funzione")) }
        group("Simboli (layout DOS US)", "-" to 12, "=" to 13, "[" to 26, "]" to 27,
            ";" to 39, "'" to 40, "`" to 41, "\\" to 43, "," to 51, "." to 52, "/" to 53, "< > (ISO)" to 86)
        group("Tastierino numerico", "Num 0" to 0x152, "Num 1" to 0x14f, "Num 2" to 0x150,
            "Num 3" to 0x151, "Num 4" to 0x14b, "Num 5" to 0x14c, "Num 6" to 0x14d,
            "Num 7" to 0x147, "Num 8" to 0x148, "Num 9" to 0x149,
            "Num ." to 83, "Num +" to 78, "Num -" to 74, "Num *" to 55,
            "Num /" to 98, "Num Invio" to 96)
    }

    fun label(scanCode: Int): String = all.firstOrNull { it.scanCode == scanCode }?.label ?: "Codice $scanCode"
}
