# Usage

[← README](../README.md)

Requires Android 8.0 or newer and a Samsung Tizen TV. The app is English-only.
Download the APK from the [latest release](https://github.com/Readiz/tizen-youtube-installer/releases/latest).

## 1. TV select

Connect the phone and TV to the same Wi-Fi, then tap **Find TV**.
Choose a TV in the popup, even when only one is found.
**Enter another TV IP** is available at the bottom for manual entry, including
when no TV is found. **Selected:** shows the chosen TV beneath the button.

<img src="images/tv-picker.png" alt="TV choices and the manual IP button" width="280">

## 2. TV setup

Open **TV setup guide** in the app for the illustrated instructions and your
phone’s Wi-Fi IP address.

1. On the TV, open **Home / Smart Hub → Apps**. On newer TVs, open **App Settings**.
2. Enter **12345** on the remote. If there are no number keys, press **123**
   to use the on-screen keypad.
3. Turn **Developer Mode ON** and set **Host PC IP** to the phone address
   shown in the app. Confirm with **OK**.
4. Hold the remote power button until the TV turns off and restarts, then
   find it in the app. If it still cannot connect, unplug the TV for
   10–15 seconds and reconnect it.

Keep Host PC IP set to your phone. Update it if the phone’s address changes.

<p>
  <img src="images/tv-setup.png" alt="TV setup guide and Developer Mode menu" width="260">
  <img src="images/tv-remote.png" alt="Remote 123 button and on-screen keypad guide" width="260">
</p>

Reference: [Samsung Developer — TV device setup](https://developer.samsung.com/smarttv/develop/getting-started/using-sdk/tv-device.html).
The app’s diagrams are original illustrations.

## 3. Select app version

Tap **Choose version** to select a package from
[SushyDev/tizen-youtube](https://github.com/SushyDev/tizen-youtube).

- **Tizen 5.5:** 2020 or newer TV models.
- **Tizen 5.0:** 2019 TV models.

The years refer to the TV’s release/model year, not its purchase year.
These choices follow upstream package guidance; compatibility still depends
on the TV and app. **Selected:** shows the chosen version beneath the button.

<img src="images/versions.png" alt="Tizen app version choices with TV model years" width="280">

### Custom installs

Tap **Custom** beside Select app version.

- **Find app** opens a GitHub repository input. Enter `owner/repo` or
  `https://github.com/owner/repo`, then tap **Find app** to choose a WGT
  from that repository’s latest public release.
- The additional **Custom** button opens the phone’s WGT file picker.
  Local files are limited to 64 MiB.

## 4. Install

Tap **Install**. Check the file, publisher and TV in the confirmation popup,
then confirm that you trust the source.

A Samsung account may be required. If prompted, sign in to Samsung in the
browser and return to the installer.

TV search and app lookup progress appear beneath their respective steps.
Installation progress and details remain in step 4; **Stop** cancels the
current operation.

Keep the installer and its app data to preserve the signing keys needed for
future updates. For key storage and recovery details, see [Security](SECURITY.md).
