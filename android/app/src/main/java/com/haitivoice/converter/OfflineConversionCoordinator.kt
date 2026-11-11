package com.haitivoice.converter

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/** Runs a real local engine using picker URIs, with bounded private copies and cleanup. */
class OfflineConversionCoordinator(
    private val contentResolver: ContentResolver,
    private val modelManager: ModelManager,
    private val engine: OfflineVoiceEngine,
    private val workDirectory: File,
    private val outputDirectory: File,
    private val maxInputBytes: Long = 100L * 1024L * 1024L,
) {
    init {
        require(maxInputBytes > 0) { "maxInputBytes must be positive" }
    }

    suspend fun convert(
        sourceUri: Uri,
        referenceUris: List<Uri>,
        consentConfirmed: Boolean,
        options: OfflineConversionOptions,
        progress: ConversionProgressListener,
    ): File = withContext(Dispatchers.IO) {
        require(consentConfirmed) { "Consentement requis pour utiliser les voix de référence." }
        require(referenceUris.isNotEmpty()) { "Sélectionnez au moins une référence vocale." }
        require(referenceUris.size <= 20) { "La limite est de 20 références." }

        val model = when (val status = modelManager.inspectInstalledModel()) {
            is ModelStatus.Ready -> status.model
            ModelStatus.Missing -> throw OfflineEngineNotInstalledException()
            is ModelStatus.Invalid -> throw IOException("Modèle local invalide : ${status.reason}")
        }

        if (!workDirectory.exists() && !workDirectory.mkdirs()) throw IOException("Impossible de créer le dossier temporaire.")
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) throw IOException("Impossible de créer le dossier de sortie.")

        val jobDirectory = File(workDirectory, UUID.randomUUID().toString())
        if (!jobDirectory.mkdir()) throw IOException("Impossible de créer le dossier de travail.")
        val outputFile = File(outputDirectory, "conversion_${UUID.randomUUID()}.wav")
        try {
            var totalBytes = 0L
            val sourceFile = copyUri(sourceUri, File(jobDirectory, "source.audio")) { copied ->
                totalBytes += copied
                if (totalBytes > maxInputBytes) throw IOException("Les fichiers sélectionnés dépassent la limite locale.")
            }
            val referenceFiles = referenceUris.mapIndexed { index, uri ->
                copyUri(uri, File(jobDirectory, "reference_$index.audio")) { copied ->
                    totalBytes += copied
                    if (totalBytes > maxInputBytes) throw IOException("Les fichiers sélectionnés dépassent la limite locale.")
                }
            }

            progress.onProgress(0.05f, "Audio préparé ; démarrage du moteur local.")
            engine.convert(sourceFile, referenceFiles, model, outputFile, options, progress)
            if (!isPcmOrRiffWav(outputFile)) {
                outputFile.delete()
                throw IOException("Le moteur n’a pas produit un fichier WAV valide.")
            }
            progress.onProgress(1f, "Conversion locale terminée.")
            outputFile
        } catch (error: Throwable) {
            outputFile.delete()
            throw error
        } finally {
            jobDirectory.deleteRecursively()
        }
    }

    private fun copyUri(uri: Uri, destination: File, onBytesCopied: (Long) -> Unit): File {
        val input = contentResolver.openInputStream(uri) ?: throw IOException("Impossible d’ouvrir un fichier audio sélectionné.")
        input.use { stream ->
            destination.outputStream().buffered().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var fileBytes = 0L
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    fileBytes += count
                    if (fileBytes > maxInputBytes) throw IOException("Un fichier dépasse la limite locale.")
                    output.write(buffer, 0, count)
                    onBytesCopied(count.toLong())
                }
                if (fileBytes == 0L) throw IOException("Le fichier audio est vide.")
            }
        }
        return destination
    }

    private fun isPcmOrRiffWav(file: File): Boolean {
        if (!file.isFile || file.length() < 12L) return false
        return try {
            file.inputStream().buffered().use { input ->
                val header = ByteArray(12)
                var offset = 0
                while (offset < header.size) {
                    val count = input.read(header, offset, header.size - offset)
                    if (count < 0) return false
                    offset += count
                }
                header.copyOfRange(0, 4).decodeToString() == "RIFF" &&
                    header.copyOfRange(8, 12).decodeToString() == "WAVE"
            }
        } catch (_: IOException) {
            false
        }
    }
}
