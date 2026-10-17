# Verification report — tizen-youtube Installer

## Current build — 0.3.20 / 2026-10-17

Native Android app, versionCode **29**, versionName **0.3.20**.
The four-step flow is **TV select → TV setup → Select app version → Install**.
The app is English-only. Selected TV and app summaries use **Selected:**;
TV names decode entities for display, and each operation shows progress in its own step.

## Current verification — 0.3.20

- Removed the experimental label from About, the file-size diagnostic, current
  download copy and version name. VersionCode is 29; versionName is **0.3.20**.
- APK/test APK build and Android lint passed: **0 errors / 5 existing warnings**.
- Updated the installed emulator app in place and opened About. Inspected the
  rendered text and verified the former label is absent. Package metadata and
  the unchanged APK signing certificate were also verified.
- This is a wording/version update. Installation behavior is unchanged; no new
  behavioral tests or physical TV installation were performed. Previous checks
  are recorded separately below.

## Previous verification — 0.3.19

- Step 4 displays **A Samsung account may be required during installation.**
  directly above Install, using the same subdued text style as the version note.
- APK and test APK build passed. Android lint: **0 errors / 5 existing warnings**.
- Existing screenshot instrumentation passed (**1 test**); inspected the selected
  home screen. Also checked step 4 at **320 dp / 160% font scale**: the notice wraps
  to two lines and stays fully visible above the button. README home image refreshed.
- In-place emulator update, package versionCode 28 and unchanged signing
  certificate were verified. No new behavioral test was added for this copy-only
  change; the 14-test regression and live lookup results below belong to 0.3.18.
- No physical TV installation or live Samsung sign-in was repeated.

## Previous verification — 0.3.18

- Custom → Find app now opens a GitHub repository input. It accepts `owner/repo`
  or an HTTPS GitHub repository URL (including `.git`); malformed input remains
  in the popup with an inline error. Cancel/invalid input keeps the current app
  selection. Drafts survive recreation, and the last submitted repository is
  restored when reopening the input during the current activity session.
- Real emulator lookup of `https://github.com/reisxd/TizenBrew.git` returned
  **v2.0.5 / TizenBrewStandalone-Old.wgt**. Selected that file and checked that
  installation confirmation names **reisxd/TizenBrew**, then cancelled.
  Reopened the input to verify the normalized repository, then used **Choose
  version** and confirmed the default source returned to **SushyDev/tizen-youtube**.
- Install uses a strong filled button. TV/version selection uses a soft tint;
  helpers use neutral pill buttons, icon actions use circular backgrounds,
  and Stop has a stop icon with a distinct cancellation color and border.
- TV choices use separate rounded rows with a name, IP, icon and chevron.
  The selected row is highlighted. The chooser opens even for one TV, and
  **Enter another TV IP** remains available at the bottom.
- The complete API 35 instrumentation suite passed: **14 tests**. It covers
  English-only UI, selection/restoration, manual IP input, Custom repository
  validation/draft restoration/cancellation, local file import,
  TV setup guide return, name decoding, per-step progress, cancellation,
  fresh source confirmation and Samsung browser prompting.
- APK/test APK build and lint passed. Screenshot instrumentation passed at
  normal size and **320 dp / 160% font scale**. Inspected the new repository popup
  in both sizes: the title, input, helper text and actions remain readable.
  Also inspected the real custom repository results and publisher confirmation.
- Android lint: **0 errors / 5 existing warnings** (three synchronous preference
  writes and two newer test-dependency version notices).
- In-place emulator update and the unchanged APK signing certificate were verified.
  Application ID and vault alias are unchanged to preserve installed-app identity.

README screenshots use fixture TV/release data. This UI iteration did not perform
physical TV installation, live Samsung sign-in/certificate issuance or a WGT download.

## Build identity

| Item | Value |
| --- | --- |
| APK SHA-256 | `534674e7104066a87cf984c8730403b522dcbdf2776a2233c07875d88c9a4bec` |
| Signing certificate SHA-256 | `49b681fb7cbf592298c16916e020b07f8f9fae16529231474045c18ae393f100` |
| Android test runtime | Android 15 / API 35 emulator |
| Build environment | JDK 17, Gradle 8.11.1, Android SDK 36 / Build Tools 35.0.0 |

## Existing protocol evidence

The installation/signing logic was not changed in this iteration. Prior core
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
