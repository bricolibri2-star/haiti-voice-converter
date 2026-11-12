# Build and downloadable artifacts

The project targets Android SDK 34 and Java 17. GitHub Actions workflow `Android build and source ZIP` builds a debug APK and packages the checked-out source as a ZIP.

To trigger it, open **Actions → Android build and source ZIP → Run workflow** on branch `copilot/android-build-artifacts`. On success, download the `konveti-voice-studio-build` artifact. It contains:

- `app-debug.apk` — unsigned debug APK for installation/testing;
- `konveti-voice-studio-source.zip` — source archive for this branch revision.

Local build prerequisites: JDK 17, Android SDK platform 34/build-tools 34.0.0, and Gradle 8.7. Build with:

```bash
gradle -p android --no-daemon clean :app:assembleDebug
```

APK output: `android/app/build/outputs/apk/debug/app-debug.apk`.

## Product status and safety

This workflow packages the source and build output; it does not add voice-conversion capability. The Android screen is currently a UI prototype and does not invoke conversion. The backend requires a real `VOICE_CONVERSION_COMMAND` engine adapter. No compatible, licensed model or adapter is included in this repository. Do not describe the APK/ZIP as a functioning voice converter until a model/engine is integrated and end-to-end testing succeeds. Use only reference voices with documented authorization.
