# Changelog

All notable changes to this project will be documented in this file.

## [1.1.0] - 2026-10-16

- **Live Haptic Vibration Control:** Toggling the vibration switch in the app now immediately dispatches the BLE command (`0x06`) to the Venty, allowing haptic vibration to be enabled or disabled on the fly without remaining off.
- **Max Wakeups Guard:** Configurable safety limit for consecutive keep-alive resets (default: 0 / unlimited; 1 to 10 resets). Prevents continuous heating in pockets if the device was forgotten.
- **GitHub Security Policy:** Added official `.github/SECURITY.md` for coordinated vulnerability reporting and responsible disclosure.
- Bumped version to 1.1.0 (versionCode 13).

## [1.0.10] - 2026-10-10

- Prominently display third-party disclaimer footer on dashboard.
- Integrated Disclaimer & Warranty Warning in About dialog.
- Version bump to 1.0.10 (versionCode 12).

## [1.0.8] - 2026-10-10

### Highlights & Improvements
- **Production Signed Release:** Built and signed with release keystore (`release.jks`) via APK Signature Scheme v2.
- **Obtainium Support:** Direct update integration via GitHub releases.
- **Android Adaptive Icon:** Native adaptive launcher icon (`mipmap-anydpi-v26`) with tuned 60% safe zone for app drawers and circular masks.
- **Modern Dark UI:**
  - Removed unstable actual temperature readout for a cleaner telemetry overview (Target Temp, Battery %, Shutoff Countdown, Heater State).
  - Cleaned up About dialog with direct links.
  - Edge-to-edge support with dark system bars.
- **BLE Watchdog Engine:** Automatic connection and 120s auto-shutoff timer bypass via background foreground service.

## [1.0.0] - 2026-10-10

### Initial Release
- Initial release with BLE background keep-alive service.
