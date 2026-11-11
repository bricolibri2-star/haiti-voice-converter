# Android offline implementation plan and scaffold

The new Kotlin files define two boundaries:

- `ModelManager` inspects a model in app-private `filesDir/offline-model/` and verifies the model file against the SHA-256 value in `model.json`.
- `OfflineVoiceEngine` and `OfflineConversionRepository` define the local conversion contract and options for fidelity, pitch and language.

These are scaffolding, **not an inference engine**. There is no model/runtime adapter in this repository yet; the application must not report a conversion as successful until a real adapter creates a valid output WAV.

## Model integrity

`ModelManager` expects a manifest with `model_id`, `runtime`, `model_filename`, and `sha256`, alongside the model file. The filename is constrained to a simple basename and the actual file hash is checked. This detects accidental corruption, but a hash stored beside a model is not a trusted signature: production distribution must pin the expected hash in a trusted release or verify a signed manifest. Check the model license before bundling or downloading it.

## Offline pipeline to implement

1. Copy selected source/reference content URIs into app-private temporary storage using bounded streaming; do not rely on display names or filesystem paths from the picker.
2. Validate and decode in a bounded worker. Use app-private temporary files, cleanup in `finally`, cancellation, disk-space checks and progress reporting.
3. Check `ModelManager.inspectInstalledModel()` and fail clearly if the model is absent or invalid.
4. Invoke a real Android-compatible runtime through an `OfflineVoiceEngine` implementation. It must not make network calls; validate output format and nonzero duration before success.
5. Export the result through Android's document picker/MediaStore only after successful inference; provide playback and an explicit cleanup action.
6. Run acceptance tests with network disabled after setup: convert source + authorized reference(s), listen, export, restart, and repeat without any network request.

Model choice is still a prerequisite. Traditional RVC checkpoints are not automatically reference-conditioned mobile models, and merely adding multiple reference files does not guarantee voice conversion. Choose and benchmark a model/runtime on target phones before implementing its adapter. Do not embed an API token in the APK.

## Current build status

The repository currently has a prototype UI and these contracts only. It does not yet include complete Android Gradle build files, a voice model, a model installer, or a functioning local inference implementation. Therefore no functional offline APK can be built from this scaffold alone.
