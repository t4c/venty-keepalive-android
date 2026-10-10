# Changelog

All notable changes to this project will be documented in this file.

## [1.0.9] - 2026-10-10

- Added official Disclaimer & Warranty Warning to README and About dialog.
- Version bump to 1.0.9 (versionCode 11).

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
