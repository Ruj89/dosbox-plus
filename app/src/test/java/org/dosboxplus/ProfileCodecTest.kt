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

    @Test fun imagesSurviveProfileExportAndOldProfilesKeepText() {
        val button = Control(type = ControlType.BUTTON, x = .1f, y = .1f, width = .2f, height = .2f,
            label = "Fuoco", action = InputAction.Key(29), image = "icon:fire")
        val embedded = button.copy(id = "custom", image = ControlImages.prefix + "iVBORw0KGgo=")
        val profile = Profile(name = "Immagini", portrait = listOf(button), landscape = listOf(embedded))
        assertEquals(profile, ProfileCodec.decode(ProfileCodec.encode(profile)))
        val oldJson = ProfileCodec.encode(profile.copy(portrait = listOf(button.copy(image = null))))
            .replace(Regex(",\\s*\"image\": null"), "")
        assertNull(ProfileCodec.decode(oldJson).portrait.single().image)
        assertThrows(IllegalArgumentException::class.java) {
            ProfileCodec.decode(ProfileCodec.encode(profile.copy(portrait = listOf(button.copy(image = "icon:unknown")))))
        }
    }

    @Test fun exportedConfigurationPreservesBothLayoutsAndCustomLabels() {
        val button = Control(type = ControlType.BUTTON, x = .1f, y = .2f, width = .2f, height = .3f,
            label = "Salta ↑ / azione personalizzata", zIndex = 4, opacity = .35f,
            trigger = ControlTrigger.HOLD, action = InputAction.Key(88))
        val profile = Profile(name = "Configurazione giocatore", portrait = listOf(button),
            landscape = listOf(button.copy(x = .7f, action = InputAction.Key(0x148))))
        assertEquals(profile, ProfileCodec.decode(ProfileCodec.encode(profile)))
    }

    @Test fun rejectsControlsOutsideCanvasAndDuplicateIds() {
        val button = Control(id = "button", type = ControlType.BUTTON, x = .2f, y = .2f,
            width = .2f, height = .2f, action = InputAction.Key(57))
        listOf(
            listOf(button.copy(x = .9f)),
            listOf(button.copy(width = 2f)),
            listOf(button.copy(height = 0f)),
            listOf(button.copy(opacity = 1.1f)),
            listOf(button, button)
        ).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) {
                ProfileCodec.decode(ProfileCodec.encode(Profile(name = "invalid", landscape = invalid)))
            }
        }
    }

    @Test fun keyboardIncludesFullLettersFunctionKeysAndDistinctKeypad() {
        val keys = KeyboardKeys.all
        assertEquals(keys.size, keys.map { it.scanCode }.distinct().size)
        ('A'..'Z').forEach { letter -> assertTrue(keys.any { it.label == letter.toString() }) }
        (1..12).forEach { number -> assertTrue(keys.any { it.label == "F$number" }) }
        assertNotEquals(keys.single { it.label == "↑ Su" }.scanCode, keys.single { it.label == "Num 8" }.scanCode)
        assertEquals(87, keys.single { it.label == "F11" }.scanCode)
        assertEquals(88, keys.single { it.label == "F12" }.scanCode)
    }
}
