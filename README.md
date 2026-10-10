# Venty Keep Alive (Android)

Native Android app & background foreground service watchdog for the Storz & Bickel **Venty** vaporizer.

Prevents the automatic 120-second auto-shutoff timeout during vaping sessions by executing an adaptive BLE state bounce before the timer expires.

---

## Features

- **Background Foreground Service:** Runs reliably with an ongoing notification and partial wakelock. Never killed by Android Doze Mode or low-memory killer while your screen is locked or the device is in your pocket.
- **Adaptive Keep-Alive Bounce:**
  - Detects active heating modes (`Heating`, `Boost`, `Superboost`).
  - Transparently executes state bounce without dropping your set target temperature or active boost mode.
- **Auto Haptic Mute:** Automatically disables haptic vibration on connect so your Venty won't buzz in your hand or pocket when executing keep-alive resets.
- **Live Telemetry (Jetpack Compose Dark UI):**
  - Target temperature setpoint
  - Battery level percentage (%)
  - Auto-shutoff countdown timer in seconds
  - Active heater mode & reset counter
  - Configurable reset threshold slider (10s – 60s)
  - Automatic detection and badge displaying your connected Venty device name

---

## Installation & Updates

### Option 1: Obtainium (Recommended)

Get automatic background updates directly from GitHub releases via [Obtainium](https://github.com/ImranR98/Obtainium):

1. Install **Obtainium** on your Android device.
2. Tap **Add App** (+).
3. Paste the repository URL:
   ```text
   https://github.com/t4c/venty-keepalive-android
   ```
4. Tap **Add** / **Install**. Obtainium tracks new releases and notifies you whenever an update drops.

---

### Option 2: Direct APK Download (Sideload)

Because this app is open source and not distributed through the Google Play Store, it must be installed manually (sideloaded).

### 1. Download
Download `VentyKeepAlive-v1.0.0.apk` from the official [GitHub Releases](https://github.com/t4c/venty-keepalive-android/releases).

### 2. Enable Unknown Sources
1. Open the downloaded `.apk` file (from browser downloads or file manager).
2. If Android prompts **"For security reasons, your phone is not allowed to install unknown apps from this source"**:
   - Tap **Settings**.
   - Enable **"Allow from this source"** / **"Trust this source"**.
   - Return to the installation prompt and tap **Install**.

### 3. Google Play Protect Warning ("Unknown Developer")
Since this is an initial release without a commercial Play Store developer reputation, Google Play Protect will show a warning:
> *"Blocked by Play Protect: Developer of this app is unknown. Apps from unknown developers can sometimes be harmful."*

**How to bypass this dialog:**
1. Do **not** tap "OK" or "Cancel".
2. Tap on the small **"More details"** (or *"See details"*) dropdown at the bottom of the prompt.
3. Tap on **"Install anyway"** (or *"Install anyway (unsafe)"*).
4. The installation will complete immediately.

### 4. Grant Permissions
When you tap **"START KEEP ALIVE"** for the first time, allow:
- **Nearby devices / Bluetooth:** Required to discover and communicate with your Venty via BLE (no location tracking used).
- **Notifications:** Required for the Android Foreground Service to keep the connection alive while your screen is off.

---

## Build from Source

Requirements: JDK 17, Android SDK Platform 35.

```bash
git clone https://github.com/t4c/venty-keepalive-android.git
cd venty-keepalive-android
./gradlew assembleDebug
```
Output APK: `app/build/outputs/apk/debug/app-debug.apk`

---

## Credits & Acknowledgments

Special credit goes to the [reactive-volcano-app](https://github.com/imtbl/reactive-volcano-app) project.

Their reverse-engineered BLE packet structures, opcode definitions (`protocol.ts`), and command mask mappings (`StatusWriteMask`, `HeaterMode`) were essential in understanding the Venty communication protocol and building this stand-alone Android implementation.

---

## Author

**t4c**  
GitHub: [https://github.com/t4c](https://github.com/t4c)
