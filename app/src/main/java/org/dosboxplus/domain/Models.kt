package org.dosboxplus.domain

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable data class GameEntry(
    val id: String = UUID.randomUUID().toString(), val title: String,
    val treeUri: String, val command: String, val arguments: String = "",
    val drive: String = "C", val profileId: String? = null, val dosboxOptions: Map<String, String> = emptyMap()
)

@Serializable data class Profile(
    val version: Int = 1, val id: String = UUID.randomUUID().toString(), val name: String,
    val portrait: List<Control> = emptyList(), val landscape: List<Control> = emptyList()
)

@Serializable data class Control(
    val id: String = UUID.randomUUID().toString(), val type: ControlType,
    val x: Float, val y: Float, val width: Float, val height: Float, val zIndex: Int = 0,
    val label: String = "", val opacity: Float = 0.68f,
    val trigger: ControlTrigger = ControlTrigger.TAP, val action: InputAction,
    val image: String? = null
)
@Serializable enum class ControlType { BUTTON, DPAD, JOYSTICK, MOUSE, KEYBOARD }
@Serializable enum class ControlTrigger { TAP, HOLD }
@Serializable sealed class InputAction {
    @Serializable data class Key(val scanCode: Int) : InputAction()
    @Serializable data class Combo(val scanCodes: List<Int>) : InputAction()
    @Serializable data class Macro(val steps: List<MacroStep>) : InputAction()
    @Serializable data class MouseButton(val button: Int) : InputAction()
    @Serializable data class JoystickButton(val button: Int) : InputAction()
}
@Serializable data class MacroStep(val scanCode: Int, val down: Boolean, val delayMs: Long = 0)
