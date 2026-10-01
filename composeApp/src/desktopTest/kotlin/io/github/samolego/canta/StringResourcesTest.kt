package io.github.samolego.canta

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Compose resources don't unescape Android's `\'` and `\"`: they'd show up
 * literally in the UI. Guards against them creeping back in (e.g. from a
 * translation sync with the wrong escaping settings, see crowdin.yml).
 */
class StringResourcesTest {

    @Test
    fun noAndroidStyleQuoteEscapes() {
        val resources = File("src/commonMain/composeResources")
        val offenders = resources.walk()
            .filter { it.name == "strings.xml" }
            .flatMap { file ->
                file.readLines().withIndex()
                    .filter { (_, line) -> "\\'" in line || "\\\"" in line }
                    .map { (index, _) -> "${file.parentFile.name}/strings.xml:${index + 1}" }
            }
            .toList()
        assertTrue(resources.isDirectory, "resources not found at ${resources.absolutePath}")
        assertTrue(offenders.isEmpty(), "Backslash-escaped quotes (shown literally): $offenders")
    }
}
