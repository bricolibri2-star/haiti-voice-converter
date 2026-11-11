# Offline Android architecture and initial Kotlin contracts

The next implementation step is the device-side model/runtime, not a remote API call. `ModelManager.kt` checks a locally installed model file and its SHA-256; `OfflineVoiceEngine.kt` specifies the conversion interface and validated options. No inference implementation or model weights are included, and the UI is still a prototype. This is not yet a functional offline APK.

See [OFFLINE_IMPLEMENTATION.md](OFFLINE_IMPLEMENTATION.md) for the model manifest contract, the required local audio pipeline and mode-airplane acceptance criteria.
