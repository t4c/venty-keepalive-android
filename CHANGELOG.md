# Changelog

All notable changes to this project will be documented in this file.

## [1.0.8] - 2026-10-10

- Official signed release build with production Keystore (`release.jks`).
- Obtainium support for automated release tracking and seamless sideload updates.
- Adaptive icon with 60% safe zone tuning for Android app drawers and launchers.
- Version code bumped to 10.

## [1.0.7] - 2026-10-10

- Support modern Android Adaptive Icons (`mipmap-anydpi-v26`) with clean foreground/background separation for native launcher app drawers.
- Bump versionCode to 8.

## [1.0.6] - 2026-10-10

- Scale launcher icon to fill full drawer bounds (no tiny inset).
- Bump versionCode to 7.

## [1.0.5] - 2026-10-10

- Pixel art Venty launcher icon with 180°C green LED display.
- Bump versionCode to 6.

## [1.0.4] - 2026-10-10

- New crisp high-contrast Venty pixel art launcher icon.
- Bump versionCode to 5.

## [1.0.3] - 2026-10-10

- Fix text clipping and padding for links in About dialog.
- Version bump to 1.0.3.

## [1.0.2] - 2026-10-10

- Pixel art custom app icon for launcher and notifications.
- Clean link formatting in About dialog (`www.ghcif.de` and `github.com/...`).
- Version bump to 1.0.2.

## [1.0.1] - 2026-10-10

- Clean vertical link layout in About dialog without prefixes.
- Updated version code & name.

## [1.0.0] - 2026-10-10

### Initial Release
- **BLE Auto-Connect:** Automatically discovers and connects to Storz & Bickel Venty devices (`S&B VY...`).
- **Adaptive Keep-Alive Bounce:** Resets the 120s auto-shutoff timer before expiration without disrupting target temperature or active heater modes (Heating, Boost, Superboost).
- **Background Foreground Service:** Reliable background daemon with persistent notification and wakelock for uninterrupted operation when screen is locked.
- **Haptic Mute Option:** Automatically disables vibration on connect to avoid buzzing during keep-alive resets.
- **Jetpack Compose Dark UI:**
  - Modern edge-to-edge dark theme with status and navigation bar insets.
  - Live display of target temperature, battery percentage, shutoff countdown, heater mode, and reset counter.
  - Connected device badge and telemetry footer.
  - Configurable reset threshold (10s – 60s).
  - About dialog with developer credentials and links.
