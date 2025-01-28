package org.hildan.orfeus.zwanger

import io.ktor.util.*
import kotlinx.datetime.*
import kotlinx.serialization.*

@Serializable
data class OrfeusZwangerCredentials(
    @SerialName("username")
    val username: String,
    @SerialName("password")
    val password: String,
    @SerialName("zwno")
    val clientNumber: String = password,
    @SerialName("praktijk")
    val practice: String,
    @SerialName("devicekey")
    val deviceKey: String,
)

@Serializable
data class EchosResponse(
    val formdata: EchosFormData,
)

@Serializable
data class EchosFormData(
    val form: String,
    val count: String,
    val formcaption: String,
    val rows: List<FormRow>,
)

@Serializable
data class FormRow(
    val value: List<FormColumn>,
)

@Serializable
data class FormColumn(
    val id: String,
    @SerialName("val")
    val value: String,
)

@Serializable
data class EchoData(
    @SerialName("Foto")
    val photo: EchoPhoto,
) {
    fun decode() = photo.base64Data.removePrefix("data:image/jpeg;base64,").decodeBase64Bytes()
}

@Serializable
data class EchoPhoto(
    @SerialName("notitie")
    val title: String,
    @SerialName("datetime")
    val dateTime: LocalDateTime,
    @SerialName("foto")
    val base64Data: String, // data:image/jpeg;base64,xxxxxxxxx
)