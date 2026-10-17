# Architecture / 0.2

## Product boundary

**tizen-youtube-installer** is an independent Android installer focused on
SushyDev/tizen-youtube. Its main flow is TV selection → TV setup → app version selection → installation.
The English-only home screen contains four steps: TV select, TV setup, Select app version and Install.
TV search, app lookup and installation each own their progress and cancellation controls;
installation logs remain in step 4. Button emphasis follows the action: a filled Install button, tonal selection buttons, neutral helpers and a distinct Stop control. TV name entities are decoded only for display, including saved names.
TV discovery always opens a chooser, even for a single result;
manual entry is available through Enter another TV IP at the bottom and after an unsuccessful search.
Default package selection opens a short Tizen-version popup; the year note refers to the TV release year. The default source is
SushyDev/tizen-youtube. Custom → Find app opens a GitHub repository input, validates
owner/repo or an HTTPS GitHub URL, and lists that repository’s release files. Input
drafts survive recreation; cancelling or invalid input preserves the selected app.
Another Custom button opens local WGT files. The Install
step confirms the exact file, source and TV before granting source consent.
Step 2 opens the illustrated first-time TV guide. The information icon opens app details.
[Apps2Samsung for Android](https://github.com/Apps2Samsung/Apps2Samsung) is the
closest existing alternative: phone-based discovery, certificate handling,
signing and SDB installation are established capabilities. This project uses
Kotlin/JCA and a JVM core; that choice carries no measured performance or
reliability advantage over Apps2Samsung's .NET/MAUI implementation.

The phone is the installer, signer and SDB developer host. No TV-side Homebrew,
Node/Web Service, loopback developer-host switch or private-key handoff is needed.
A real target TV is required to install an application. The development-only
simulator exercises the protocol, not the TV's package manager or compatibility.

## Interface references

The button hierarchy draws on the official [Google Home](https://play.google.com/store/apps/details?id=com.google.android.apps.chromecast.app)
and [SmartThings](https://play.google.com/store/apps/details?id=com.samsung.android.oneconnect) screenshots,
plus [Material common-button guidance](https://github.com/material-components/material-components-android/blob/master/docs/components/CommonButton.md).
The app retains its plain four-step flow: Install uses a strong fill, TV/version
choices use a soft tonal fill, helpers use compact neutral buttons, and Stop has
a distinct outlined cancellation treatment. Icon controls are circular; disabled
controls have separate muted fills and text. Reference screenshots are not shipped
as app assets. This is a native Android adaptation, not a copy of either app.

## Development direction

The next iteration should reduce effort in the default tizen-youtube flow while
keeping other sources optional. These are planned priorities,
not features claimed to be available today:

1. Establish a comparison baseline with Apps2Samsung on the same phone, TV and
   WGT version. Record first-time setup separately from repeat installs: manual
   input, decisions/taps, elapsed time, failures, recovery steps, and actual TV
   launch/playback where applicable. Record both APK versions, TV firmware,
   package hashes and certificate state. Preserve existing signing identities;
   use a dedicated test app/device when ownership would otherwise conflict.
2. Keep tizen-youtube available from its built-in source with one tap. Keep
   local WGT import secondary under Custom and preserve publisher identity,
   release details and source consent. Repository entry is not part of the UI.
3. Make WGT selection understandable with available TV/package metadata and
   upstream guidance. Explain ambiguous choices and unsupported cases; do not
   infer compatibility solely from a filename or silently pick a package.
4. Make connection, sign-in and installation failures actionable, preserving
   the selected source and signing identity. An uncertain install result must
   remain uncertain until checked, rather than triggering an automatic retry.

Author-key continuity, explicit renewal/recovery and bounded archive handling
remain release constraints alongside usability work. A large app catalog,
remote control and a general TV management suite are outside the current focus.
Shared protocol fixes should be documented for possible upstream contribution.

Success means measured reductions in user effort without weaker provenance,
key continuity or installation evidence. We have not run a comparative APK
test on the same TV. Documentation review does not establish that Apps2Samsung
lacks an equivalent repository workflow or that this client is easier to use.
Evidence and prior work are tracked in [SOURCES.md](SOURCES.md#2026-10-01--android-installer-positioning-review).

## Data flow

```text
Built-in SushyDev/tizen-youtube source
  -> GET /repos/SushyDev/tizen-youtube/releases/latest
  -> choose .wgt asset (explicit user choice if ambiguous)
  -> pin release ID + asset ID + download URL + size + SHA-256
  -> download -> verify size + hash -> parse bounded WGT + config.xml
  -> phone -> TV:26101 -> DUID
  -> installed-app registry / SDB applist
  -> existing encrypted key pair, or browser login + Samsung issuance
  -> persist key BEFORE any TV writes
  -> sign all original files; replace signatures, never patch manifest
  -> send device-profile.xml + package.wgt
  -> shell:0 vd_appinstall <validated package ID> <fixed staging path>
  -> verify completion, no automatic replay
  -> save TV DUID/package/repository/author fingerprint receipt
```

## Local WGT input

The Android document picker returns a content URI. The app reads bounded bytes,
validates the WGT, and saves an immutable copy by SHA-256 in its private cache.
Selection metadata survives activity recreation. Before signing, the service
rechecks the pinned size/hash and parses the widget again; a cleared cache asks
for reselection. A locally calculated hash does not authenticate a publisher.
Every installation requires fresh consent for the exact selected source, file and TV.
Cancelling the document picker preserves the existing selection.

Local files enter the same signing/SDB/evidence pipeline through `runLocal`.
Receipts use `local-wgt:<packageId>` as an explicitly local source. GitHub/local
source changes cannot overwrite an existing receipt automatically; same-source
updates reuse the original Author. No fabricated GitHub metadata is used for
local files, and GitHub's digest requirement remains unchanged.

## Modules

`core` is JVM-only. `HttpTransport`, `LanAccess`, `PairVault`, `InstallHistory`,
and `CertificateProvider` are injectable boundaries. `RepoInstaller` is the same
class used by the real Android service, local integration tests and offline demo.
The simulator depends on core main. Core tests depend on simulator main; neither
main module depends on the other's test output.

`app` depends only on core. The Android APK has no demo entry point, synthetic
certificates or simulator classes. Production targets are explicit RFC1918 IPv4
addresses over the selected Wi-Fi Network. The separate JVM simulator remains
available to core tests and developer scripts.

## Ownership / key continuity

Certificates are currently per TV, not a single global cross-TV author identity.
All apps installed by this app on that TV reuse its stored pair. A receipt is
ownership evidence but not authoritative evidence about the TV's current state.
Unknown installation/ownership blocks mutation. Uninstall/reinstall, clearing app
data or device migration needs a future explicit encrypted recovery workflow.
Expiry/renewal is an explicit release gate: never silently rotate Author keys.

## Constraints and trade-offs

Native Kotlin/JCA avoids Termux/Node/native SDB binaries. This duplicates some
upstream protocol logic; SOURCE references and independent signature/transport tests
help detect drift but do not eliminate that maintenance cost.

The current signer is in-memory with 64 MiB download and 128 MiB inflated ZIP limits;
large archives have multiple simultaneous copies. This is not a measured memory
ceiling. A streaming signer/download cache is an explicit next iteration.

Source checksum protects transfer integrity, not trustworthiness of a repository.
An attacker controlling an upstream repo can publish a new malicious asset with a
valid GitHub digest. User source trust remains required.
