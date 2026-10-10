# Changelog

All notable changes to this project will be documented in this file.

## [1.0.10] - 2026-10-10

### Highlights & Improvements
- **Production Signed Release:** Built and signed with release keystore (`release.jks`) via APK Signature Scheme v2.
- **Obtainium Support:** Direct update integration via GitHub releases.
- **Android Adaptive Icon:** Native adaptive launcher icon (`mipmap-anydpi-v26`) with tuned 60% safe zone for app drawers and circular masks.
- **Modern Dark UI:**
  - Removed unstable actual temperature readout for a cleaner telemetry overview (Target Temp, Battery %, Shutoff Countdown, Heater State).
  - Prominent third-party disclaimer footer on dashboard & About dialog.
  - Cleaned up About dialog with direct links.
  - Edge-to-edge support with dark system bars.
- **BLE Watchdog Engine:** Automatic connection and 120s auto-shutoff timer bypass via background foreground service.

## [1.0.0] - 2026-10-10

### Initial Release
- Initial release with BLE background keep-alive service.
