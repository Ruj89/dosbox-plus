package org.dosboxplus

import org.dosboxplus.domain.*
import org.junit.Assert.*
import org.junit.Test

class ControlPresetsTest {
    @Test fun everyPresetHasValidNonOverlappingLayoutsAndSupportedKeys() {
        val keyCodes = KeyboardKeys.all.map { it.scanCode }.toSet()
        assertEquals(ControlPresets.all.size, ControlPresets.all.map { it.id }.distinct().size)
        ControlPresets.all.forEach { preset ->
            val profile = preset.createProfile()
            assertEquals(profile, ProfileCodec.decode(ProfileCodec.encode(profile)))
            listOf(profile.portrait, profile.landscape).forEach { controls ->
                assertTrue(controls.isNotEmpty())
                assertTrue(controls.any { it.type == ControlType.KEYBOARD })
                controls.forEach { control ->
                    val codes = when (val action = control.action) {
                        is InputAction.Key -> listOf(action.scanCode)
                        is InputAction.Combo -> action.scanCodes
                        else -> emptyList()
                    }
                    assertTrue("Unsupported key in ${preset.id}: $codes", codes.all { it in keyCodes })
                    controls.filter { it.id != control.id }.forEach { other ->
                        val overlaps = control.x < other.x + other.width && other.x < control.x + control.width &&
                            control.y < other.y + other.height && other.y < control.y + control.height
                        assertFalse("${preset.id}: ${control.label} overlaps ${other.label}", overlaps)
                    }
                }
            }
        }
    }

    @Test fun applyingTemplateKeepsTargetIdentityAndCreatesIndependentControls() {
        val preset = requireNotNull(ControlPresets.find("midnight-classic"))
        val first = preset.createProfile()
        val original = DefaultProfiles.create("Gioco personale")
        val applied = preset.applyTo(original)
        assertEquals(original.id, applied.id)
        assertEquals(original.name, applied.name)
        assertNotEquals(first.id, applied.id)
        assertTrue(first.landscape.map { it.id }.intersect(applied.landscape.map { it.id }.toSet()).isEmpty())
        assertNotEquals(original.landscape, applied.landscape)
        assertNotEquals(original.portrait, applied.portrait)
        assertTrue(original.landscape.any { it.type == ControlType.DPAD })
    }

    @Test fun midnightUsesItsActualCompassAndTurnCommandsInsteadOfArrowKeys() {
        val profile = requireNotNull(ControlPresets.find("midnight-classic")).createProfile()
        listOf(profile.landscape, profile.portrait).forEach { controls ->
            val byLabel = controls.associateBy { it.label }
            mapOf("Avanza" to 16, "Guarda" to 18, "Pensa" to 19, "Scegli" to 20, "Notte" to 22,
                "Nuova" to 30, "Carica" to 32, "Sì" to 34, "No" to 36, "Luxor" to 46,
                "Morkin" to 47, "Corleth" to 48, "Rorthron" to 49).forEach { (label, code) ->
                assertEquals(InputAction.Key(code), byLabel.getValue(label).action)
                assertEquals(ControlTrigger.TAP, byLabel.getValue(label).trigger)
            }
            val compass = listOf("N · 1", "NE · 2", "E · 3", "SE · 4", "S · 5", "SO · 6", "O · 7", "NO · 8")
            compass.forEachIndexed { index, label -> assertEquals(InputAction.Key(index + 2), byLabel.getValue(label).action) }
            assertFalse(controls.any { it.type == ControlType.DPAD })
        }
    }

    @Test fun citadelHasMouseAndItsSeparateTimeAndMapCommands() {
        val controls = requireNotNull(ControlPresets.find("midnight-citadel")).createProfile().landscape.associateBy { it.label }
        assertEquals(InputAction.MouseButton(0), controls.getValue("Clic").action)
        assertEquals(InputAction.Key(50), controls.getValue("Tempo").action)
        assertEquals(InputAction.Key(60), controls.getValue("Mappa").action)
        assertEquals(InputAction.Key(67), controls.getValue("+1 ora").action)
        assertEquals("icon:map", controls.getValue("Mappa").image)
    }

    @Test fun shooterPresetsKeepLabelsAndActionsWithIconsInBothOrientations() {
        listOf("doom", "wolfenstein").forEach { name ->
            val profile = requireNotNull(ControlPresets.find(name)).createProfile()
            listOf(profile.portrait, profile.landscape).forEach { controls ->
                val fire = controls.single { it.label == "Fuoco" }
                assertEquals("icon:fire", fire.image)
                assertEquals(InputAction.Key(29), fire.action)
                assertEquals(ControlTrigger.HOLD, fire.trigger)
            }
        }
    }
}
