# event

A personal Android event counter. Each tap records one timestamped occurrence. Events and occurrence history are stored locally in SQLite; a JSON backup can be exported and imported to move data between devices.

## Build

Open this folder in Android Studio and run the `app` configuration, or build a debug APK with `gradlew :app:assembleDebug`.

## Gitee in-app updates

The app reads `updates/update.json` from the public `main` branch and downloads the APK from `updates/`. The APK is verified with SHA-256 before Android's package installer opens.

When publishing an update:

1. Increase `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Build the APK with the same signing key used for the installed app.
3. Add the versioned APK to `updates/` and update `updates/update.json` with its HTTPS URL and SHA-256.
4. Push the changes to Gitee.

Keep the signing keystore in a private backup; `.keystore` and `.jks` files are ignored by Git. The public repository contains the app source and install packages, but the app's SQLite data stays on the phone and is not uploaded.

## Data migration

In **Settings**, export a JSON backup on the old phone, transfer it to the new phone, and import it there. Import merges by stable IDs, so importing the same backup again does not duplicate records.
