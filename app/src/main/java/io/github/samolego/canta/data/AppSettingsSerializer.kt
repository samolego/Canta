package io.github.samolego.canta.data

import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import io.github.samolego.canta.data.proto.AppSettings
import io.github.samolego.canta.util.LogUtils
import java.io.InputStream
import java.io.OutputStream

object AppSettingsSerializer : Serializer<AppSettings> {
    override val defaultValue: AppSettings = AppSettings
        .newBuilder()
        .setAutoUpdateBloatList(true)
        .setConfirmBeforeUninstall(true)
        .build()

    override suspend fun readFrom(input: InputStream): AppSettings {
        try {
            return AppSettings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            LogUtils.e("AppSettingsSerializer", "Cannot read proto.", exception)
        }
        return defaultValue
    }

    override suspend fun writeTo(t: AppSettings, output: OutputStream) = t.writeTo(output)
}
