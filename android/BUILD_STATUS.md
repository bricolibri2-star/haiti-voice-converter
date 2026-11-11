# Android project structure

This project is a mobile-first prototype. The repository now includes the structure and contracts needed to move toward a real offline APK, but the actual voice model and runtime integration are still missing.

## Current status

- UI prototype exists.
- Local model contract and integrity checks exist.
- Offline conversion contracts exist.
- No real Android model bundle is present.
- No valid, tested local inference implementation is included.
- No functional offline APK can be built yet from this repository alone.

## Required before claiming offline APK support

1. Add a real ONNX or TFLite model bundle to the app assets or to an application-owned installation flow.
2. Validate the model checksum and reputation before installation.
3. Implement the Android runtime invocation for source + reference audio.
4. Run conversion in airplane mode on test devices.
5. Validate output audio quality with native Haitian reference speakers.
6. Package the signed release APK after a clean test pass.

## Runtime recommendation

Use ONNX Runtime Mobile as the primary runtime path for the first offline Android build, because it is broadly compatible with Android and suitable for local inference. A TFLite route is viable only if the model is converted to TFLite and validated on target devices.

## Important warning

Do not present this repo as a finished offline APK. It is a structured groundwork for the next implementation phase, not a shipped product.
