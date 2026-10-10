# Changelog

All notable changes to this project will be documented in this file.

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
