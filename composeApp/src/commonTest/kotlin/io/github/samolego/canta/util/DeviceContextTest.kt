package io.github.samolego.canta.util

import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceContextTest {
    private fun ctx(vararg props: Pair<String, String>) =
        deviceContextOf(mapOf("ro.product.manufacturer" to "Xiaomi", "ro.build.version.sdk" to "34") + props)

    private fun rom(vararg props: Pair<String, String>) = ctx(*props).let { it.rom to it.romVersion }

    @Test
    fun brandAndSdk() {
        assertEquals(DeviceContext("xiaomi", "stock", 0, 34), ctx())
        assertEquals(DeviceContext("unknown", "stock", 0, 0), deviceContextOf(emptyMap()))
        assertEquals("unknown", deviceContextOf(mapOf("ro.product.manufacturer" to "  ")).brand)
        assertEquals(0, deviceContextOf(mapOf("ro.build.version.sdk" to "abc")).sdk)
        assertEquals("google", deviceContextOf(mapOf("ro.product.manufacturer" to " Google ")).brand)
    }

    @Test
    fun hyperos() {
        assertEquals("hyperos" to 1, rom("ro.mi.os.version.name" to "OS1.0"))
        assertEquals("hyperos" to 2, rom("ro.mi.os.version.name" to "OS2.0.200"))
        // HyperOS phones still carry the MIUI props; HyperOS wins.
        assertEquals("hyperos" to 1, rom("ro.mi.os.version.name" to "OS1.0", "ro.miui.ui.version.name" to "V816"))
    }

    @Test
    fun miui() {
        assertEquals("miui" to 14, rom("ro.miui.ui.version.name" to "V14"))
        assertEquals("miui" to 12, rom("ro.miui.ui.version.name" to "V125"))
        assertEquals("miui" to 8, rom("ro.miui.ui.version.name" to "V816"))
        assertEquals("miui" to 13, rom("ro.miui.ui.version.name" to "V130", "ro.miui.ui.version.code" to "13"))
        assertEquals("miui" to 10, rom("ro.miui.ui.version.name" to "V125", "ro.miui.ui.version.code" to "10"))
    }

    @Test
    fun otherRoms() {
        assertEquals("oneui" to 6, rom("ro.build.version.oneui" to "60100"))
        assertEquals("oneui" to 5, rom("ro.build.version.oneui" to "50101"))
        assertEquals("coloros" to 13, rom("ro.build.version.opporom" to "V13.1"))
        assertEquals("realmeui" to 2, rom("ro.build.version.realmeui" to "V2.0", "ro.build.version.opporom" to "V13.1"))
        assertEquals("funtouch" to 13, rom("ro.vivo.os.version" to "13.0"))
        assertEquals("originos" to 3, rom("ro.vivo.os.version" to "3.0", "ro.vivo.os.name" to "OriginOS"))
        assertEquals("magicos" to 7, rom("ro.build.version.magic" to "MagicOS_7.1.0"))
        assertEquals("emui" to 12, rom("ro.build.version.emui" to "EmotionUI_12.0.0"))
        assertEquals("oxygenos" to 13, rom("ro.oxygen.version" to "13.1"))
        assertEquals("oxygenos" to 14, rom("ro.rom.version" to "OxygenOS_14.0.0"))
        assertEquals("nothingos" to 2, rom("ro.build.nothing.version" to "2.5.2"))
        assertEquals("stock" to 0, rom("ro.rom.version" to "something else"))
    }

    @Test
    fun getpropParsingKeepsOnlyKnownKeys() {
        val out = """
            [ro.product.manufacturer]: [Xiaomi]
            [ro.product.model]: [23049PCD8G]
            [ro.build.fingerprint]: [xiaomi/x/y:14/z]
            [ro.mi.os.version.name]: [OS1.0]
            [ro.build.version.sdk]: [34]
            [ro.empty.value]: []
            garbage line
            [multi.line]: [first
            second]
        """.trimIndent()
        val props = parseGetprop(out)
        assertEquals(setOf("ro.product.manufacturer", "ro.mi.os.version.name", "ro.build.version.sdk"), props.keys)
        assertEquals(DeviceContext("xiaomi", "hyperos", 1, 34), deviceContextOf(props))
        assertEquals(mapOf("ro.empty.value" to ""), parseGetprop(out, setOf("ro.empty.value")))
    }

    @Test
    fun labels() {
        assertEquals("HyperOS 1", DeviceContext("xiaomi", "hyperos", 1, 34).romLabel())
        assertEquals("One UI 6", DeviceContext("samsung", "oneui", 6, 34).romLabel())
        assertEquals("Android", DeviceContext("google", "stock", 0, 34).romLabel())
    }
}
