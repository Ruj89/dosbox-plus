package org.dosboxplus

import org.dosboxplus.domain.*
import org.junit.Assert.*
import org.junit.Test

class DefaultProfilesTest {
    @Test fun starterProfileCanBeSavedInBothOrientations() {
        val profile = DefaultProfiles.create()
        assertEquals(profile, ProfileCodec.decode(ProfileCodec.encode(profile)))
        listOf(profile.portrait, profile.landscape).forEach { controls ->
            assertTrue(controls.any { it.type == ControlType.DPAD })
            assertTrue(controls.any { it.type == ControlType.KEYBOARD })
            assertTrue(controls.any { it.action == InputAction.Key(57) && it.trigger == ControlTrigger.HOLD })
            controls.forEach {
                assertTrue(it.x + it.width <= 1f)
                assertTrue(it.y + it.height <= 1f)
            }
        }
    }

    @Test fun newGamesDoNotShareProfileIdentity() {
        val first = DefaultProfiles.create("Primo")
        val second = DefaultProfiles.create("Secondo")
        assertNotEquals(first.id, second.id)
        assertTrue(first.landscape.map { it.id }.intersect(second.landscape.map { it.id }.toSet()).isEmpty())
    }
}
