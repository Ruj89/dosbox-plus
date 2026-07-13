package org.dosboxplus

import kotlinx.coroutines.test.runTest
import org.dosboxplus.domain.InputAction
import org.dosboxplus.input.InputDispatcher
import org.dosboxplus.input.NativeInput
import org.junit.Assert.assertEquals
import org.junit.Test

class InputDispatcherTest {
    @Test fun holdSendsBalancedKeyEvents() {
        val events = mutableListOf<NativeInput>()
        val dispatcher = InputDispatcher { events.add(it); Unit }
        val action = InputAction.Combo(listOf(29, 56))
        dispatcher.press(action)
        dispatcher.release(action)
        assertEquals(listOf(
            NativeInput.Key(29, true), NativeInput.Key(56, true),
            NativeInput.Key(56, false), NativeInput.Key(29, false)
        ), events)
    }

    @Test fun tapSendsDownAndUp() = runTest {
        val events = mutableListOf<NativeInput>()
        InputDispatcher { events.add(it); Unit }.execute(InputAction.Key(57))
        assertEquals(listOf(NativeInput.Key(57, true), NativeInput.Key(57, false)), events)
    }
}
