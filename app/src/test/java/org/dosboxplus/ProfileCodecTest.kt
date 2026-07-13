package org.dosboxplus

import org.dosboxplus.domain.*
import org.junit.Assert.*
import org.junit.Test

class ProfileCodecTest {
    @Test fun profileRoundTrips() {
        val profile = Profile(name = "FPS", landscape = listOf(Control(type = ControlType.BUTTON, x = .1f, y = .2f, width = .1f, height = .1f, action = InputAction.Combo(listOf(29, 56, 111)))))
        assertEquals(profile, ProfileCodec.decode(ProfileCodec.encode(profile)))
    }
    @Test fun onlyDosExecutablesAreCandidates() = assertEquals(listOf("GAME.EXE", "setup.bat"), LaunchCommand.candidates(listOf("README.TXT", "setup.bat", "GAME.EXE")))
    @Test fun commandUsesRequestedDrive() = assertTrue(LaunchCommand.command(GameEntry(title="x", treeUri="u", command="GAME.EXE")).startsWith("C:\n"))
    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownProfileVersion() {
        ProfileCodec.decode("""{"version":2,"id":"x","name":"x","portrait":[],"landscape":[]}""")
    }
    @Test fun controlStyleRoundTrips() {
        val control = Control(type = ControlType.BUTTON, x = .2f, y = .3f, width = .1f, height = .1f, opacity = .45f, trigger = ControlTrigger.HOLD, action = InputAction.Key(57))
        val decoded = ProfileCodec.decode(ProfileCodec.encode(Profile(name = "HUD", portrait = listOf(control))))
        assertEquals(control, decoded.portrait.single())
    }
}
