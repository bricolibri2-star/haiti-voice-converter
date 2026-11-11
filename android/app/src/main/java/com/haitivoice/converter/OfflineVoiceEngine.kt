package com.haitivoice.converter

import java.io.File

/** Parameters passed to a device-side voice-conversion runtime. */
data class OfflineConversionOptions(
    val fidelity: Float = 0.75f,
    val pitchSemitones: Float = 0f,
    val languageTag: String = "ht",
) {
    init {
        require(fidelity in 0f..1f) { "Fidelity must be between 0 and 1." }
        require(pitchSemitones in -12f..12f) { "Pitch must be between -12 and 12 semitones." }
        require(languageTag in setOf("ht", "fr")) { "Supported language tags are ht and fr." }
    }
}

fun interface ConversionProgressListener {
    fun onProgress(progress: Float, status: String)
}

/**
 * Contract for a real on-device conversion runtime. Implementations must not
 * use network access and should process the source and references locally.
 */
interface OfflineVoiceEngine {
    val engineId: String

    suspend fun convert(
        sourceAudio: File,
        referenceAudio: List<File>,
        model: InstalledVoiceModel,
        outputWav: File,
        options: OfflineConversionOptions,
        progress: ConversionProgressListener,
    )
}

/**
 * Resolves the selected document URIs to app-private temporary files, validates
 * audio, invokes OfflineVoiceEngine, and exports the output. This is deliberately
 * an interface: no compatible mobile voice-conversion engine/model is included
 * in the repository yet, so callers must not present a fake conversion result.
 */
interface OfflineConversionRepository {
    suspend fun convert(
        sourceAudio: File,
        referenceAudio: List<File>,
        options: OfflineConversionOptions,
        progress: ConversionProgressListener,
    ): File
}

class OfflineEngineNotInstalledException : IllegalStateException(
    "A compatible on-device voice-conversion engine and licensed model must be installed first."
)
