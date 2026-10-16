# Changelog

All notable changes to this project will be documented in this file.

## [1.1.0] - 2026-10-16

- **Live Haptic Vibration Control:** Umlegen des Schalters in der App sendet das BLE-Kommando (`0x06`) jetzt sofort an den Venty, sodass Vibrationen live deaktiviert und wieder reaktiviert werden können.
- **Max Wakeups Guard:** Einstellbares Limit für Keep-Alive-Resets (Default: 0 / unendlich; 1 bis 10 Resets). Verhindert endloses Durchglühen des Venty in der Hosentasche, falls das Ausschalten vergessen wurde.
- **GitHub Security Policy:** Offizielle `.github/SECURITY.md` für koordinierte Vulnerability Reports und Responsible Disclosure.
- Version bump auf 1.1.0 (versionCode 13).

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
