package com.haitivoice.converter

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** Describes a locally installed model after its file integrity has been checked. */
data class InstalledVoiceModel(
    val directory: File,
    val modelFile: File,
    val modelId: String,
    val runtime: String,
    val sha256: String,
)

sealed interface ModelStatus {
    data object Missing : ModelStatus
    data class Ready(val model: InstalledVoiceModel) : ModelStatus
    data class Invalid(val reason: String) : ModelStatus
}

/**
 * Finds and integrity-checks a model installed in app-private storage.
 * This does not download, install, or execute a model. The manifest's digest is
 * only useful for integrity if the manifest itself is trusted/pinned or signed.
 */
class ModelManager(context: Context) {
    private val modelDirectory = File(context.filesDir, MODEL_DIRECTORY)

    fun inspectInstalledModel(): ModelStatus {
        val manifestFile = File(modelDirectory, MANIFEST_FILENAME)
        if (!manifestFile.isFile) return ModelStatus.Missing

        return try {
            val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
            val modelId = manifest.getString("model_id")
            val runtime = manifest.getString("runtime")
            val filename = manifest.getString("model_filename")
            val expectedHash = manifest.getString("sha256").lowercase()

            if (!SAFE_FILENAME.matches(filename)) return ModelStatus.Invalid("Nom de fichier modèle invalide.")
            if (!SHA256.matches(expectedHash)) return ModelStatus.Invalid("Empreinte SHA-256 invalide.")
            if (modelId.isBlank() || runtime.isBlank()) return ModelStatus.Invalid("Métadonnées du modèle incomplètes.")

            val canonicalDirectory = modelDirectory.canonicalFile
            val modelFile = File(canonicalDirectory, filename).canonicalFile
            if (modelFile.parentFile != canonicalDirectory || !modelFile.isFile || modelFile.length() == 0L) {
                return ModelStatus.Invalid("Fichier de modèle absent ou invalide.")
            }
            if (sha256(modelFile) != expectedHash) return ModelStatus.Invalid("L’empreinte du modèle ne correspond pas.")

            ModelStatus.Ready(InstalledVoiceModel(canonicalDirectory, modelFile, modelId, runtime, expectedHash))
        } catch (_: Exception) {
            ModelStatus.Invalid("Le manifeste local du modèle est illisible ou incomplet.")
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val MODEL_DIRECTORY = "offline-model"
        const val MANIFEST_FILENAME = "model.json"
        val SAFE_FILENAME = Regex("[A-Za-z0-9._-]{1,128}")
        val SHA256 = Regex("[a-f0-9]{64}")
    }
}
