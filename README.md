# event

A personal Android event counter. Each tap records one timestamped occurrence. Events and occurrence history are stored locally in SQLite; a JSON backup can be exported and imported to move data between devices.

## Build

Open this folder in Android Studio and run the `app` configuration, or build a debug APK with `gradlew :app:assembleDebug`.

## Package a manual update on Windows

From PowerShell in the repository folder, run:

```powershell
.\scripts\package-update.ps1 -VersionName "1.1" -Changelog "Describe the update"
```

The script increments `versionCode`, sets `versionName`, builds a debug APK with the local Android debug signing key, copies it to `dist/`, calculates its SHA-256, and writes a draft manifest to `dist/update.json`. It does not upload or publish anything.

## Gitee in-app updates

The app reads `updates/update.json` from the public `main` branch, downloads the APK, verifies its SHA-256, and then opens Android's package installer.

When publishing an update:

1. Run the local packaging script above.
2. Upload `dist/event-<version>.apk` as an attachment to a public Gitee Release. Use a tag such as `v1.1`.
3. Put the Release attachment's HTTPS download URL into the `apkUrl` field in `dist/update.json`.
4. Copy the completed draft to `updates/update.json`, then merge/push that manifest to `main`.
5. On the phone, open **Settings → App updates → Check for updates**.

Do not put APKs in the repository's raw file URL: Gitee may require login to download them. Release attachments are intended for software downloads and support files up to 100 MB each. Keep the signing key backed up and unchanged so Android can install updates over the current app. `.keystore` and `.jks` files are ignored by Git. Event data remains on the phone and is not uploaded.

## Data migration

In **Settings**, export a JSON backup on the old phone, transfer it to the new phone, and import it there. Import merges by stable IDs, so importing the same backup again does not duplicate records.
