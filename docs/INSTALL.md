# Install and run Starfall

## Install a shared APK

1. Get the APK from a release published by the project owner at
   [StarFall Releases](https://github.com/agamjotsingh08/StarFall/releases).
   If there is no release attachment, build from source using the README.
2. Transfer the APK to an Android 8.0+ phone or tablet and open it.
3. Allow installation from the chosen browser/file manager if prompted, then install.
4. Open Starfall, choose a ship and difficulty, and tap Deploy.

To install with Android SDK Platform Tools:

```sh
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.starfall.game.debug/com.starfall.game.MainActivity
```

`adb` must be on PATH, or use its full path inside the SDK's `platform-tools` folder.
If multiple devices are connected, insert `-s DEVICE_SERIAL` after `adb`.
Debug APKs built on different computers may use different signing keys. If Android
reports a signature mismatch, use an APK signed with the original key to preserve
saved progress. Uninstalling the old app also resolves the conflict but erases its data.

## Test with Android Studio

1. Open the project and configure JDK 21 and SDK 35 as described in the README.
2. Open **Tools → Device Manager → Create Virtual Device**.
3. Choose a phone profile, select an API 35 system image matching your computer's
   supported architecture, download it if necessary, and finish.
4. Start that device. Select `app` and the running device in the toolbar; click Run.
5. Repeat with a tablet profile to inspect the larger layout. On a computer with
   limited RAM, run one emulator at a time.

A phone connected over USB also works: enable Developer options and USB debugging,
then accept the debugging prompt on the phone and select it in Studio.
Official references: [Android Emulator](https://developer.android.com/studio/run/emulator)
and [Android Debug Bridge](https://developer.android.com/tools/adb).

The project draws a consistent portrait arena and fills extra screen area with the
background. Drag with the mouse on an emulator; use a physical touchscreen to judge
the feel of steering while tapping Dash or Nova with a second finger.

## Common build problems

| Symptom | Fix |
| --- | --- |
| Java not found / unsupported Java version | Set `JAVA_HOME` and the Studio Gradle JDK to JDK 21. Restart the terminal after changing environment variables. |
| SDK location not found | Set `ANDROID_HOME` or let Studio create `local.properties`. Do not commit a personal SDK path. |
| Missing platform/build tools | Install Platform 35 and Build Tools 35.0.0 in SDK Manager; accept SDK licenses. |
| Dependency download fails | Check network/proxy access to Google's Maven repository, Maven Central and Gradle distribution servers. |
| No device / unauthorized | Start an emulator, or unlock the USB phone and accept the debugging authorization. Check `adb devices`. |
| Slow emulator | Run one virtual device, close heavy builds while testing, enable supported hardware acceleration, or use a phone. |
| PowerShell blocks helper scripts | Use `.\gradlew.bat assembleDebug testDebugUnitTest lintDebug` directly; no policy change is necessary. |

## Publishing later

These are future maintainer steps, not actions performed by the build scripts.

1. Review source, documentation and `.gitignore`; choose a repository license and
   check asset notices before public distribution.
2. Run the validation commands and test on real devices. Confirm the app version in
   `app/build.gradle` and update `CHANGELOG.md`.
3. Commit/push only when the owner authorizes it. Avoid uploading `.tools`, build
   folders, machine configuration, signing keys or credentials.
4. For a public installable build, use Android Studio's **Generate Signed App Bundle
   or APK** flow with an owner-controlled release key. Keep the key and passwords
   outside Git and retain them for future updates.
5. Create a GitHub Release with the version, change notes and signed APK attachment.
   Include its SHA-256 checksum. A source-only release ZIP cannot be installed as an app.
6. Check the download and install it on a device before announcing it.

For private testing, sharing `dist/starfall-debug.apk` is sufficient; label it a debug
build. A Play Store launch needs separate release configuration and store preparation.
