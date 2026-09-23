package org.dosboxplus.input

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.dosboxplus.domain.InputAction

sealed interface NativeInput { data class Key(val code: Int, val down: Boolean): NativeInput; data class Mouse(val button: Int, val down: Boolean): NativeInput }
class InputDispatcher(private val sink: (NativeInput) -> Unit) {
    private val _events = MutableSharedFlow<NativeInput>(extraBufferCapacity = 32)
    val events = _events.asSharedFlow()
    private fun send(e: NativeInput) { sink(e); _events.tryEmit(e) }
    suspend fun execute(action: InputAction) = when (action) {
        is InputAction.Key -> tap(action.scanCode)
        is InputAction.Combo -> {
            press(action)
            try { delay(50) } finally { release(action) }
        }
        is InputAction.Macro -> action.steps.forEach { delay(it.delayMs); send(NativeInput.Key(it.scanCode, it.down)) }
        is InputAction.MouseButton -> {
            send(NativeInput.Mouse(action.button, true))
            // Mouse buttons are polled once per core frame, unlike queued keys.
            // Keep a click observable and always release it on cancellation.
            try { delay(50) } finally { send(NativeInput.Mouse(action.button, false)) }
        }
        is InputAction.JoystickButton -> tap(action.button)
    }
    private suspend fun tap(code: Int) {
        send(NativeInput.Key(code, true))
        // Games polling their keyboard state need to observe a down frame,
        // not both transitions within the same emulation tick.
        try { delay(50) } finally { send(NativeInput.Key(code, false)) }
    }
    fun press(action: InputAction) {
        if (action is InputAction.MouseButton) send(NativeInput.Mouse(action.button, true))
        else keys(action).forEach { send(NativeInput.Key(it, true)) }
    }
    fun release(action: InputAction) {
        if (action is InputAction.MouseButton) send(NativeInput.Mouse(action.button, false))
        else keys(action).asReversed().forEach { send(NativeInput.Key(it, false)) }
    }
    private fun keys(action: InputAction): List<Int> = when (action) {
        is InputAction.Key -> listOf(action.scanCode)
        is InputAction.Combo -> action.scanCodes
        else -> emptyList()
    }
}
