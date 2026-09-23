package org.dosboxplus

import org.dosboxplus.domain.*
import org.junit.Assert.*
import org.junit.Test

class ControlGeometryTest {
    private val button = Control(type = ControlType.BUTTON, x = .2f, y = .3f,
        width = .2f, height = .1f, action = InputAction.Key(57))

    @Test fun successiveDragDeltasAccumulate() {
        var moved = button
        repeat(10) { moved = moved.moveTo(moved.x + .01f, moved.y + .02f) }
        assertEquals(.3f, moved.x, .00001f)
        assertEquals(.5f, moved.y, .00001f)
    }

    @Test fun movementKeepsWholeControlOnCanvas() {
        val moved = button.moveTo(2f, -1f)
        assertEquals(.8f, moved.x, .00001f)
        assertEquals(0f, moved.y, .00001f)
        assertEquals(button.width, moved.width)
    }

    @Test fun enlargingAtBottomRightKeepsControlReachable() {
        val resized = button.moveTo(.8f, .9f).resizeTo(.5f, .6f)
        assertEquals(.5f, resized.x, .00001f)
        assertEquals(.4f, resized.y, .00001f)
        assertEquals(resized, ProfileCodec.decode(ProfileCodec.encode(Profile(name = "resize", portrait = listOf(resized)))).portrait.single())
    }

    @Test fun resizingCannotProduceNegativeOrOversizedControls() {
        val resized = button.resizeTo(-2f, 5f)
        assertEquals(.04f, resized.width, .00001f)
        assertEquals(1f, resized.height, .00001f)
        assertEquals(0f, resized.y, .00001f)
    }
}
