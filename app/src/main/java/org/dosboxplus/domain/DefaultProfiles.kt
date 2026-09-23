package org.dosboxplus.domain

/** The editor starts from the same usable controls as a game with no saved profile. */
object DefaultProfiles {
    fun create(name: String = "Controlli predefiniti"): Profile = Profile(
        name = name,
        landscape = controls(portrait = false),
        portrait = controls(portrait = true)
    )

    private fun controls(portrait: Boolean): List<Control> {
        val y = if (portrait) .72f else .6f
        val h = if (portrait) .11f else .17f
        return listOf(
            Control(type = ControlType.DPAD, x = .04f, y = y, width = .28f,
                height = if (portrait) .23f else .34f, action = InputAction.Combo(emptyList())),
            Control(type = ControlType.BUTTON, x = .76f, y = y, width = .19f, height = h,
                label = "Spazio", trigger = ControlTrigger.HOLD, action = InputAction.Key(57)),
            Control(type = ControlType.BUTTON, x = .54f, y = y + h + .02f, width = .19f, height = h,
                label = "Ctrl", trigger = ControlTrigger.HOLD, action = InputAction.Key(29)),
            Control(type = ControlType.BUTTON, x = .04f, y = .04f, width = .14f, height = h,
                label = "Esc", action = InputAction.Key(1)),
            Control(type = ControlType.KEYBOARD, x = .72f, y = .04f, width = .24f, height = h,
                label = "Tastiera", action = InputAction.Key(57))
        )
    }
}
