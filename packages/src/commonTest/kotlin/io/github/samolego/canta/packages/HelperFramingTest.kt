@file:OptIn(ExperimentalSerializationApi::class)

package io.github.samolego.canta.packages

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class HelperFramingTest {

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    private val details = PackageDetails(
        packageName = "a",
        label = "A",
        versionCode = 3,
        disabled = true,
        installed = true,
    )

    // package_name (1) = "a", label (2) = "A", version_code (4) = 3,
    // disabled (5) = true, installed (8) = true; zero-valued fields omitted.
    private val detailsBytes = bytes(
        0x0A, 0x01, 0x61,
        0x12, 0x01, 0x41,
        0x20, 0x03,
        0x28, 0x01,
        0x40, 0x01,
    )

    @Test
    fun packageDetailsWireFormatIsStable() {
        assertContentEquals(detailsBytes, ProtoBuf.encodeToByteArray(PackageDetails.serializer(), details))
        assertEquals(details, ProtoBuf.decodeFromByteArray(PackageDetails.serializer(), detailsBytes))
    }


    @Test
    fun detailsListRoundTripsThroughLineFraming() {
        val list = PackageDetailsList(PROTOCOL_VERSION, listOf(details, PackageDetails(packageName = "b")))
        val line = HelperFraming.encodeLine(PackageDetailsList.serializer(), list)
        assertEquals(false, line.contains('\n'))
        assertEquals(list, HelperFraming.decodeLine(PackageDetailsList.serializer(), "$line\r\n"))
    }
}
