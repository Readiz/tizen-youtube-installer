# Verification report — tizen-youtube Installer

## Current build — 0.3.17 / 2026-10-17

Native Android app, versionCode **26**, versionName **0.3.17-experimental**.
The four-step flow is **TV select → TV setup → Select app version → Install**.
The app is English-only. Selected TV and app summaries use **Selected:**;
TV names decode entities for display, and each operation shows progress in its own step.

## Button design and UI verification

- Install uses a strong filled button. TV/version selection uses a soft tint;
  helpers use neutral pill buttons, icon actions use circular backgrounds,
  and Stop has a stop icon with a distinct cancellation color and border.
- TV choices use separate rounded rows with a name, IP, icon and chevron.
  The selected row is highlighted. The chooser opens even for one TV, and
  **Enter another TV IP** remains available at the bottom.
- The complete API 35 instrumentation suite passed: **13 tests**. It covers
  English-only UI, selection/restoration, manual IP input, Custom file import,
  TV setup guide return, name decoding, per-step progress, cancellation,
  fresh source confirmation and Samsung browser prompting.
- After the final Stop-padding adjustment, APK/test APK build and lint passed,
  and screenshot instrumentation passed at normal size and **320 dp / 160% font scale**.
  Inspected home, TV/version/custom/confirmation popups and operation progress;
  long popup actions stack, and enlarged labels wrap without clipping controls.
- Android lint: **0 errors / 5 existing warnings** (three synchronous preference
  writes and two newer test-dependency version notices).
- In-place emulator update and the unchanged APK signing certificate were verified.
  Application ID and vault alias are unchanged to preserve installed-app identity.

README screenshots use fixture TV/release data. This UI iteration did not perform
physical TV installation, live Samsung sign-in/certificate issuance or a WGT download.

## Build identity

| Item | Value |
| --- | --- |
| APK SHA-256 | `85ea211741cbabc54f7b640d860b7c5d3bed8f9b85f3489815104df0b8f0eebf` |
| Signing certificate SHA-256 | `49b681fb7cbf592298c16916e020b07f8f9fae16529231474045c18ae393f100` |
| Android test runtime | Android 15 / API 35 emulator |
| Build environment | JDK 17, Gradle 8.11.1, Android SDK 36 / Build Tools 35.0.0 |

## Existing protocol evidence

The installation/signing core was not changed in this UI iteration. Prior core
verification recorded **78 passed, 0 failed**, including the device-profile v1
endpoint correction and six issuer-response regressions. These are prior results,
not a claim that the core suite or physical-TV checks were repeated for this build.

The [recorded public-release smoke test](TIZEN_YOUTUBE_TEST.md) exercised real GitHub
HTTP/downloads, payload-byte preservation, independent XMLDSig validation and
loopback install/reinstall with synthetic certificates. Its TV compatibility
limitations remain applicable. [Build instructions](BUILDING.md) describe how to
repeat the checks; [the hardware checklist](HARDWARE_CHECKLIST.md) covers device testing.

Local verification does not establish current GitHub Actions status, Samsung
certificate issuance, TV package acceptance, launch or playback compatibility.
