package io.github.samolego.canta.packages

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.io.encoding.Base64

/**
 * Line framing for helper output: each message is written to stdout as one
 * line of base64-encoded protobuf. Text framing is required because some ADB
 * transports (ya-webadb's `spawnWaitText`) only return decoded text, which
 * would corrupt raw binary output.
 */
@OptIn(ExperimentalSerializationApi::class)
object HelperFraming {

    fun <T> encodeLine(serializer: KSerializer<T>, message: T): String =
        Base64.encode(ProtoBuf.encodeToByteArray(serializer, message))

    fun <T> decodeLine(serializer: KSerializer<T>, line: String): T =
        ProtoBuf.decodeFromByteArray(serializer, Base64.decode(line.trim()))
}
