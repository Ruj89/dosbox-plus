package org.dosboxplus

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.dosboxplus.domain.InputAction
import org.dosboxplus.input.InputDispatcher
import org.dosboxplus.input.NativeInput
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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

    @Test fun mouseClickRemainsPressedAcrossCorePolls() = runTest {
        val events = mutableListOf<NativeInput>()
        val dispatcher = InputDispatcher { events.add(it); Unit }
        val click = launch { dispatcher.execute(InputAction.MouseButton(0)) }
        runCurrent()
        advanceTimeBy(32)
        assertEquals(listOf(NativeInput.Mouse(0, true)), events)
        click.join()
        assertEquals(listOf(NativeInput.Mouse(0, true), NativeInput.Mouse(0, false)), events)
    }

    @Test fun cancelledMouseClickStillReleasesTheButton() = runTest {
        val events = mutableListOf<NativeInput>()
        val dispatcher = InputDispatcher { events.add(it); Unit }
        val click = launch { dispatcher.execute(InputAction.MouseButton(1)) }
        runCurrent()
        click.cancel()
        click.join()
        assertEquals(listOf(NativeInput.Mouse(1, true), NativeInput.Mouse(1, false)), events)
    }

    @Test fun mouseHoldSendsMouseEvents() {
        val events = mutableListOf<NativeInput>()
        val dispatcher = InputDispatcher { events.add(it); Unit }
        dispatcher.press(InputAction.MouseButton(0))
        dispatcher.release(InputAction.MouseButton(0))
        assertEquals(listOf(NativeInput.Mouse(0, true), NativeInput.Mouse(0, false)), events)
    }

    @Test fun keyAndShortcutRemainObservableAndReleaseOnCancellation() = runTest {
        listOf(InputAction.Key(57) to listOf(57), InputAction.Combo(listOf(29, 34)) to listOf(29, 34)).forEach { (action, codes) ->
            val events = mutableListOf<NativeInput>()
            val dispatcher = InputDispatcher { events.add(it); Unit }
            val tap = launch { dispatcher.execute(action) }
            runCurrent()
            advanceTimeBy(32)
            assertEquals(codes.map { NativeInput.Key(it, true) }, events)
            tap.cancel()
            tap.join()
            assertEquals(codes.map { NativeInput.Key(it, true) } + codes.asReversed().map { NativeInput.Key(it, false) }, events)
        }
    }
}
