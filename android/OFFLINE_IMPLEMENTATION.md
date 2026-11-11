# Offline Android build path

The project now has a Gradle app module and an offline conversion coordinator. The coordinator copies user-selected document URIs into bounded, app-private temporary files, requires explicit consent and a local model, calls the `OfflineVoiceEngine` contract, checks that the result has a RIFF/WAVE header, and deletes temporary input copies on success or failure.

## Still not a working converter

There is no implementation of `OfflineVoiceEngine` and no real voice model in this repository. ONNX Runtime is a runtime library only; it cannot convert speech without a model whose input/output tensor contract and preprocessing are known. `model.json.example` is a template and is intentionally not installable. Do not replace its hash/license placeholders with guessed values.

## Next implementation gate

Select a legally redistributable, consented model that supports the desired reference-conditioned voice conversion. Document tensor names, shapes, sample rates, normalization, chunking and output format. Then implement and test the matching Android ONNX adapter; a generic `session.run` is not enough to define voice conversion. Build and test on target devices, including an airplane-mode test after model installation.

## Build prerequisites

Use JDK 17, Android SDK 34 and a compatible Gradle 8.x installation. From this directory, build with `gradle :app:assembleDebug`. No Gradle wrapper or APK artifact is committed yet. A successful Gradle build would validate packaging only, not the existence or quality of the voice engine.
