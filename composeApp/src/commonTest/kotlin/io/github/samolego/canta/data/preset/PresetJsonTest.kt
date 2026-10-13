package io.github.samolego.canta.data.preset

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PresetJsonTest {

    @Test
    fun importsCurrentAndLegacyFormats() = runTest {
        val current = importPresetFromJson("""{"name": "P", "apps": ["com.a", "com.b"]}""")
        val legacy =
            importPresetFromJson("""{"name": "P", "apps": [{"packageName": "com.a"}, {"packageName": "com.b"}]}""")

        assertEquals(setOf("com.a", "com.b"), current?.apps)
        assertEquals(setOf("com.a", "com.b"), legacy?.apps)
    }

    @Test
    fun dropsUnsafeAndMissingPackages() = runTest {
        val json = """{"name": "P", "apps": ["com.a", "com.a;reboot", "com.b ${'$'}(id)", "com.missing"]}"""
        val existing = setOf("com.a", "com.a;reboot", "com.b ${'$'}(id)")

        val preset = importPresetFromJson(json) { it in existing }

        assertEquals(setOf("com.a"), preset?.apps)
    }

    @Test
    fun rejectsPresetsItCannotRead() = runTest {
        assertNull(importPresetFromJson("""{"apps": ["com.a"]}"""), "no name")
        assertNull(importPresetFromJson("""{"name": "P"}"""), "no apps")
        assertNull(importPresetFromJson("""["com.a"]"""), "not an object")
        assertNull(importPresetFromJson("not json"))
    }

    @Test
    fun exportedPresetsImportIdentically() = runTest {
        val preset = CantaPresetData(
            name = "Débloat \"all\"",
            description = "line 1\nline 2",
            createdDate = 1_700_000_000_000,
            apps = setOf("com.b", "com.a"),
            version = "2.0",
            uuid = "original",
        )

        val imported = assertNotNull(importPresetFromJson(exportPresetToJson(preset)))

        assertEquals(preset.copy(uuid = imported.uuid), imported)
        assertTrue(imported.uuid.isNotEmpty() && imported.uuid != preset.uuid, "an import is a new preset")
    }
}
