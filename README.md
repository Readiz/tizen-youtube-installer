# tizen-youtube-installer

**Install [tizen-youtube](https://github.com/SushyDev/tizen-youtube) on your Samsung TV from your Android phone.**

An independent Android installer focused on tizen-youtube. No PC, Termux, or TV-side Homebrew service needed.

Made possible by the work of **SushyDev, reisxd, the Apps2Samsung authors, and the Tizen homebrew community**. See [Inspiration and credits](#inspiration-and-credits) for implementation sources and related projects.

**Android 8.0+ · English** · [Build an APK](docs/BUILDING.md) · [CI builds](https://github.com/Readiz/tizen-youtube-installer/actions/workflows/android.yml)

Screenshots and steps below describe the 0.3.17 development build.

<p>
  <img src="docs/images/home.png" alt="English home screen with the selected TV and app version" width="260">
  <img src="docs/images/tv-picker.png" alt="TV choices with clear selectable rows and an Enter another TV IP button" width="260">
  <img src="docs/images/versions.png" alt="Popup with the Tizen version choices" width="260">
</p>

## Project focus

[Apps2Samsung for Android](https://github.com/Apps2Samsung/Apps2Samsung) already provides phone-based TV discovery, Samsung certificate handling, signing, and installation without a PC or Termux. It is the closest existing alternative; the core installation flow overlaps substantially with this project.

**tizen-youtube-installer** focuses on one simple flow: **select your TV → set up your TV → select an app version → install**. The source is fixed to SushyDev/tizen-youtube. Local WGT files remain available under **Custom install**. Kotlin/JCA is our implementation choice, not evidence of better speed, reliability, or compatibility. Simplifying this flow is the goal; a usability advantage has not yet been measured in a comparison on the same TV.

For a community catalog, custom package installation, and broader TV management, see Apps2Samsung. See our [development direction](docs/ARCHITECTURE.md#development-direction) for priorities and comparison criteria.

## Get started

After choosing a TV or app version, **Selected:** shows the current choice beneath its button.

Search and lookup progress appears directly under the corresponding step; installation progress and details stay in step 4. Install uses the strongest fill; selection buttons use a soft tint, helper actions use neutral buttons, and Stop has its own clear cancellation style.

1. **TV select.** Connect the phone and TV to the same Wi-Fi, then tap **Find TV**. Choose a TV in the popup, even when only one is found. **Enter another TV IP** is always available at the bottom for manual entry, including when no TV is found.
2. **TV setup.** Open **TV setup guide** for the illustrated Developer Mode and phone-IP instructions. Return here after setting up the TV.
3. **Select app version.** Tap **Choose version**, then choose **Tizen 5.5 for 2020 or newer models** or **Tizen 5.0 for 2019 models** in the popup. The years refer to the Tizen TV’s release year. No repository entry or file list appears on the home screen.
4. **Install.** Tap **Install** in section 4. Check the file, publisher and TV in the confirmation popup, then confirm that you trust the source. If prompted, sign in to Samsung in the browser and return to the installer.

**Custom install:** tap **Custom** beside Select app version. **Find app** fetches the built-in tizen-youtube source and displays full file names; the additional **Custom** button opens the phone’s WGT picker. There is no repository input. Local files are limited to 64 MiB. The app is English-only, including on phones that previously saved Korean as the app language.

<details>
<summary>First-time TV setup: the 12345 menu</summary>

1. On the TV, open **Home / Smart Hub → Apps**. On newer TVs, open **App Settings**.
2. Enter **12345** on the remote. No number keys? Press **123** to use the on-screen keypad.
3. Turn **Developer Mode ON** and set **Host PC IP** to the phone address shown in the app. Confirm with **OK**.
4. Hold the remote power button until the TV turns off and restarts, then find it in the app. If it still cannot connect, unplug the TV for 10–15 seconds and reconnect it.

Keep Host PC IP set to your phone. Update it if the phone's address changes.

<img src="docs/images/tv-setup.png" alt="Dedicated TV setup guide with an original TV menu diagram" width="260">
<img src="docs/images/tv-remote.png" alt="Original 123 remote and on-screen keypad diagrams, followed by the Developer Mode menu" width="260">

The app guide uses original vector diagrams, including the remote **123** button and the on-screen number pad.

Guide reference: [Samsung Developer — TV device setup](https://developer.samsung.com/smarttv/develop/getting-started/using-sdk/tv-device.html).

</details>

> Experimental debug APK; compatibility depends on your TV and the app. Keep this installer and its data to preserve the signing keys needed for future updates.

## Inspiration and credits

The Kotlin implementation adapts protocol and signing conventions from Tizen Homebrew and tizen.js, and draws on the additional references below. Thank you to their original authors and contributors for sharing the work that made this installer possible.

- **[Tizen Homebrew](https://github.com/SushyDev/tizen-homebrew) — SushyDev and contributors.** A foundation for this implementation: its SDB transport, file transfer and installation sequence, WGT signature format, certificate issuance flow, and GitHub release selection informed the Kotlin implementation.
- **[tizen.js](https://github.com/reisxd/tizen.js) — reisxd and contributors.** A source for the Samsung certificate workflow, including CSR fields, certificate authority retrieval, and issuance API conventions.
- **[Apps2Samsung](https://github.com/Apps2Samsung/Apps2Samsung) — the Apps2Samsung authors and contributors.** Direct prior work on a standalone Android installer with certificate provisioning, signing, and an in-process SDB engine. It is our primary product comparison and also informed our TV setup guide.
- **[TizenBrew](https://github.com/reisxd/TizenBrew) and [TizenBrew Installer](https://github.com/reisxd/TizenBrewInstaller) — reisxd and contributors.** References for the Tizen homebrew ecosystem, developer setup, Samsung certificate requirements, and SDB installation commands.
- **[tizen-youtube](https://github.com/SushyDev/tizen-youtube) — SushyDev and contributors.** The app used in our installation example and compatibility testing; its documentation guided WGT version selection and comparison of certificate-based installation paths. The app itself is the work of its upstream authors.

[tizen-youtube's own installation guide](https://github.com/SushyDev/tizen-youtube/blob/main/docs/README.md#apps2samsung-with-a-partner-certificate) already lists Apps2Samsung with a Partner certificate. Tizen Homebrew's TV-hosted design is a separate architectural choice; it does not imply that its authors were unaware of phone-based alternatives. Additional Android SDB and Termux precedents are recorded in [Source traceability](docs/SOURCES.md#2026-10-01--android-installer-positioning-review).

Please visit and support the original projects. File-level source references and recorded blob hashes are in [Source traceability](docs/SOURCES.md); attribution and license notices are in [NOTICE](NOTICE). This is an independent project, with no endorsement implied from the upstream authors.

[Build](docs/BUILDING.md) · [Architecture](docs/ARCHITECTURE.md) · [Security](docs/SECURITY.md) · [Test results](docs/TEST_REPORT.md) · [Credits](NOTICE)

GPL-3.0-only · [License](LICENSE) · Unofficial; not affiliated with Samsung, Google, or YouTube.
