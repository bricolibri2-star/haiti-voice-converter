package com.haitivoice.converter

import android.content.ContentResolver
import android.net.Uri
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/** Uploads source/reference audio to the configured HTTPS FastAPI backend. */
class VoiceConversionClient(private val resolver: ContentResolver) {
    data class Result(val requestId: String, val downloadUrl: String)

    fun convert(
        baseUrl: String,
        token: String,
        source: Uri,
        references: List<Uri>,
        consent: Boolean,
        fidelity: Float = 0.75f,
        pitch: Float = 0f,
    ): Result {
        require(consent) { "Confirmez l’autorisation d’utiliser la voix de référence." }
        require(references.isNotEmpty() && references.size <= 20) { "Ajoutez entre 1 et 20 références vocales." }
        require(fidelity in 0f..1f && pitch in -12f..12f) { "Réglages audio invalides." }
        val root = baseUrl.trim().trimEnd('/')
        require(root.startsWith("https://", ignoreCase = true)) { "L’adresse du serveur doit utiliser HTTPS." }
        val boundary = "Konveti-${UUID.randomUUID()}"
        val connection = URL("$root/convert").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 30_000
            connection.readTimeout = 30 * 60 * 1000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.setRequestProperty("Accept", "application/json")
            if (token.isNotBlank()) connection.setRequestProperty("Authorization", "Bearer ${token.trim()}")
            connection.outputStream.buffered().use { out ->
                fun field(name: String, value: String) {
                    out.write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n".toByteArray())
                }
                fun part(name: String, uri: Uri, index: Int) {
                    val filename = "audio_$index.${extension(uri)}"
                    out.write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"; filename=\"$filename\"\r\nContent-Type: application/octet-stream\r\n\r\n".toByteArray())
                    val input = resolver.openInputStream(uri) ?: throw IOException("Impossible de lire un audio sélectionné.")
                    input.use { it.copyTo(out) }
                    out.write("\r\n".toByteArray())
                }
                field("language", "ht")
                field("voice_name", "Kreyol")
                field("fidelity", fidelity.toString())
                field("pitch", pitch.toString())
                field("consent", "true")
                part("source_audio", source, 0)
                references.forEachIndexed { index, uri -> part("reference_audio", uri, index + 1) }
                out.write("--$boundary--\r\n".toByteArray())
            }
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = try { JSONObject(body).optString("detail", "Erreur du serveur ($code)") }
                catch (_: Exception) { "Erreur du serveur ($code)" }
                throw IOException(message)
            }
            val json = JSONObject(body)
            val path = json.getString("result_url")
            require(path.startsWith("/result/")) { "Lien de résultat serveur invalide." }
            return Result(json.getString("request_id"), "$root$path")
        } finally {
            connection.disconnect()
        }
    }

    private fun extension(uri: Uri): String {
        val name = resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
        return name?.substringAfterLast('.', "wav")?.lowercase()?.takeIf { it.matches(Regex("[a-z0-9]{1,8}")) } ?: "wav"
    }
}
