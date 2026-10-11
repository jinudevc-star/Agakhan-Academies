<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/38b20e64-3340-47ab-83eb-49d983913aef

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. (Optional) To use the Gemini maintenance assistant, create `.env` and set `GEMINI_API_KEY` (see `.env.example`). The register does not need an API key.
5. Select an emulator or connect a device, then run the app.

For a fully offline build, first let Gradle download all required dependencies, then enable Gradle offline mode in Android Studio. You can use the installed app offline without doing this setup again.

## Offline use

The asset register is stored in a local Room database. Asset search, edits, maintenance checks, CSV import, report export, and scheduled reminders work without an internet connection. New or edited records remain marked as pending sync until the configured campus server is reachable. Gemini AI and campus-server synchronization are optional network features; if synchronization fails, register changes are still saved locally.

## AKAH SOP Register — AssetTrack QR

Campus asset register and SOP maintenance dashboard for Aga Khan Academy, Hyderabad, with QR code scanning, status updates, and an AI maintenance assistant (Gemini). Android app (Kotlin / Jetpack Compose).

`app/google-services.json` is not committed. Firebase is not required for the local/offline register.
