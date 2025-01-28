package org.hildan.orfeus.zwanger

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.cache.storage.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.*
import java.nio.file.*
import kotlin.io.path.*
import kotlin.time.Duration.Companion.days

private val credentialsFile = Path("credentials.properties")
private val lastUpdateStateFile = Path("lastUpdate.txt")
private val outputDir = Path("echo-photos")

private val httpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(DefaultRequest) {
        header(HttpHeaders.Accept, ContentType.Application.Json)
        header(HttpHeaders.Referrer, "https://webapp.orfeus.nl/orfeus-zwanger-3/")
        userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
    }
    install(HttpCookies)
    install(HttpCache) {
        val cacheDir = Path("./http-cache").createDirectories()
        publicStorage(FileStorage(cacheDir.toFile()))
    }
}

suspend fun main() {
    val localProperties = credentialsFile.readProperties()
    val credentials = OrfeusZwangerCredentials(
        username = localProperties.getProperty("username"),
        password = localProperties.getProperty("password"),
        practice = localProperties.getProperty("practice"),
        deviceKey = localProperties.getProperty("deviceKey"),
    )
    if (credentials.username.isBlank() || credentials.password.isBlank()) {
        error("Please set the credentials in credentials.properties")
    }

    val lastUpdate = lastUpdateStateFile.takeIf { it.exists() }?.readText()?.trim()?.let(LocalDateTime::parse)
        ?: Instant.DISTANT_PAST.toLocalDateTime(TimeZone.currentSystemDefault())

    httpClient.downloadEchos(
        credentials = credentials,
        since = lastUpdate,
        targetDir = outputDir.createDirectories()
    )

    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    lastUpdateStateFile.writeText(now.toString())
}

suspend fun HttpClient.downloadEchos(
    credentials: OrfeusZwangerCredentials,
    since: LocalDateTime,
    targetDir: Path,
) {
    post("https://webapp.orfeus.nl/mybaby-rest/login") {
        contentType(ContentType.Application.Json)
        setBody(credentials)
    }
    val echosForm = get("https://webapp.orfeus.nl/mybaby-rest/getformdata/echos/${credentials.practice}/${credentials.clientNumber}/")
        .body<EchosResponse>()
    val echos = echosForm.formdata.rows
        .map { it.readEchoMetadata() }
        .filter { it.dateTime >= since }
    println("${echos.size} new echos found since $since")

    echos.forEachIndexed { index, echoMeta ->
        println("Downloading echo '${echoMeta.title}'...")
        val echoData = get("https://webapp.orfeus.nl/mybaby-rest/getfoto/${credentials.practice}/${credentials.clientNumber}/${echoMeta.id}")
            .body<EchoData>()
        val formattedDateTime = echoMeta.dateTime.format(LocalDateTime.Formats.ISO).replace(':', '-')
        val imagePath = targetDir.resolve("$formattedDateTime-echo-$index.jpeg")
        imagePath.writeBytes(echoData.decode())
        imagePath.setJpegDateTaken(echoMeta.dateTime)
    }
}

private fun FormRow.readEchoMetadata(): EchoMetadata = EchoMetadata(
    id = columnValue("PDFS_ID"),
    title = columnValue("PDFS_NOTITIE"),
    dateTime = LocalDateTime.parse(columnValue("PDFS_NEWDT")),
)

private fun FormRow.columnValue(id: String): String = value.first { it.id == id }.value

data class EchoMetadata(
    val id: String, // PDFS_ID
    val title: String, // PDFS_NOTITIE
    val dateTime: LocalDateTime, // PDFS_NEWDT
)
