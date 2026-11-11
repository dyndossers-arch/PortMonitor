# Port Monitor (Android)

Native Android project in Java. Minimum Android 8.0 (API 26), tested target profile Android 12 (API 32).

## Features
- User-managed list of TCP host/IP + port entries
- Per-entry timeout (1,000–15,000 ms)
- 30-second background polling via foreground service
- Immediate manual check
- ONLINE/OFFLINE/UNKNOWN status colors
- Notifications only on state transitions after the initial check
- Local persistence and boot receiver

## Build APK
1. Install Android Studio (current stable) and Android SDK Platform 35.
2. Open this folder as a project.
3. Let Gradle sync.
4. Build > Build Bundle(s) / APK(s) > Build APK(s).
5. APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Important Android behavior
- The foreground service displays an ongoing notification while monitoring.
- On Xiaomi/MIUI, enable Autostart for Port Monitor and set Battery saver to No restrictions.
- Android may stop the service if the user force-stops the app; reopen it and tap Start.
- This checks TCP connection establishment, not application-level health or ICMP ping.
- Android 13+ asks for notification permission.
- A port is marked OFFLINE when TCP connect fails or times out; transient network loss can cause a brief false alarm.

## Scope
The project is provided as source. A compiled APK must be built with Android SDK/Gradle on a configured build machine.


## Cloud build (no Android Studio required locally)
1. Create a GitHub repository and upload the contents of this project.
2. Open the repository's Actions tab.
3. Select **Build Port Monitor APK** and press **Run workflow**.
4. When the run completes, open it and download the `PortMonitor-debug-apk` artifact.
5. Extract the artifact ZIP; install `app-debug.apk` on your Android phone.

This produces a debug-signed APK suitable for personal testing. It is not a Play Store release.
