# RideConnect Android App

RideConnect is a rider group tracker app for live ride coordination, location sharing, and group safety status.

## Project structure

- `android_app/`: Android (Kotlin + Jetpack Compose) application source.
- `stitch_rideconnect_group_tracker/`: exported design HTML screens.

## Local setup

1. Open `android_app/` in Android Studio.
2. Configure Firebase for Android:
   - Add your package `com.rideconnect.app`.
   - Download `google-services.json` and place it at `android_app/app/google-services.json`.
3. Configure Mapbox secrets:
   - Copy `android_app/secrets.properties.example` to `android_app/secrets.properties`.
   - Set `MAPBOX_PUBLIC_ACCESS_TOKEN=...` with your public token (`pk...`).
   - Set `MAPBOX_DOWNLOADS_TOKEN=...` with your secret downloads token (`sk...`) that has `DOWNLOADS:READ`.
4. Sync Gradle files in Android Studio after adding tokens.

## Security checklist before launch

- Ensure `.gitignore` keeps `google-services.json`, `secrets.properties`, and keystore files out of git.
- Rotate any previously exposed Google Maps key if you used one before.
- Keep the Mapbox downloads token secret. It should only live in `secrets.properties` or your local Gradle properties.
- Set strict Firebase Realtime Database rules:
  - Rules template is in `android_app/firebase.database.rules.json`.
- In Firebase Console, deploy the rules (or use Firebase CLI).

## Build and release checklist

- Increase `versionCode` and `versionName` in `android_app/app/build.gradle.kts`.
- Add release signing config in Android Studio.
- Build a release artifact (`AAB`) and test on at least 2 physical devices.
- Validate permission flow (allow, deny, deny permanently).
- Test network failure and Firebase auth failure flows.

## Notes

- The app now uses Mapbox Compose instead of Google Maps Compose.
- The app reads the Mapbox public token from a generated Android string resource backed by `secrets.properties`.
- Do not hardcode API keys or secret download tokens in source files.

## Launch checklist

### 1. Secrets and config

- Confirm `android_app/secrets.properties` exists locally and is not committed.
- Confirm `android_app/app/google-services.json` matches the Firebase project you will ship.
- Confirm `MAPBOX_PUBLIC_ACCESS_TOKEN` and `MAPBOX_DOWNLOADS_TOKEN` are valid.
- In Mapbox, make sure the secret downloads token has `DOWNLOADS:READ`.

### 2. Firebase deployment

- Deploy Realtime Database rules from `android_app/firebase.database.rules.json`.
- Confirm the deployed rules include the secure ride structure:
  - `rides/{rideId}/meta`
  - `rides/{rideId}/members`
  - `rides/{rideId}/invites`
  - `rides/{rideId}/riders`
  - `emailIndex`
- Confirm users can only read/write their own profile data.

### 3. Multi-account app testing

Use at least 2 real Firebase accounts.

- Account A logs in and starts a ride.
- Account A invites Account B by email.
- Account B logs in and joins using the ride id.
- Confirm Account B can join only when invited.
- Confirm a non-invited account is rejected.
- Confirm both riders appear on the map.
- Confirm pending invites show readable labels instead of raw UIDs.
- Confirm SOS is disabled until a ride is active.
- Confirm SOS updates the active rider state when triggered.
- Confirm returning signed-in users go directly to the main app instead of login.

### 4. Failure-path testing

- Deny location permission and verify the app remains usable.
- Turn network off and verify login / join / start ride failures are understandable.
- Try password reset from the login screen.
- Try logout from settings and confirm the next app open requires login.
- Try starting a ride with an unknown invite email and confirm the error is clear.

### 5. Release build prep

- Increase `versionCode` and `versionName` in `android_app/app/build.gradle.kts`.
- Create and store a release keystore safely.
- Build an `AAB` release artifact from Android Studio.
- Test the release build, not only the debug APK.
- Review app name, icons, screenshots, and privacy copy before publishing.

### 6. Store readiness

- Add a privacy policy, especially because the app handles live location.
- Prepare Play Store listing text and screenshots.
- Document what ride/location data is stored and who can access it.
- Decide whether background tracking is needed before launch; if yes, add the required service/permission flow before release.

