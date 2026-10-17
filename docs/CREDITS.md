# Credits

[← README](../README.md)

This independent installer is made possible by the following projects and
their authors and contributors.

## Implementation sources

- **[Tizen Homebrew](https://github.com/SushyDev/tizen-homebrew) — SushyDev and contributors.**
  Its SDB transport, file transfer and installation sequence, WGT signature
  format, certificate flow and GitHub release selection informed the Kotlin
  implementation. The XML signature layout is adapted from its signing code.
- **[tizen.js](https://github.com/reisxd/tizen.js) — Reis Can (reisxd) and contributors.**
  A source for Samsung CSR fields, certificate authority retrieval and
  certificate issuance API conventions.

Both projects declare GPL-3.0-only. See [License details](LICENSING.md),
[NOTICE](../NOTICE) and the file-level records in [Source traceability](SOURCES.md).

## Related work and guidance

- **[Apps2Samsung](https://github.com/Apps2Samsung/Apps2Samsung) — its authors and contributors.**
  Direct prior work on a standalone Android installer with TV discovery,
  certificate provisioning, signing and an in-process SDB engine. It is the
  primary product comparison and also informed the TV setup guide.
- **[TizenBrew](https://github.com/reisxd/TizenBrew) and
  [TizenBrew Installer](https://github.com/reisxd/TizenBrewInstaller) — reisxd and contributors.**
  References for developer setup, Samsung certificate requirements and SDB
  installation commands.
- **[tizen-youtube](https://github.com/SushyDev/tizen-youtube) — SushyDev and contributors.**
  The default app source. Its documentation guides WGT version selection;
  the TV app itself is the work of its upstream authors.
- **[Samsung Developer](https://developer.samsung.com/smarttv/develop/getting-started/using-sdk/tv-device.html).**
  Official TV developer setup documentation. The installer’s setup diagrams
  are original illustrations.

Apps2Samsung already provides the broader phone-based installation workflow.
This project focuses on a short tizen-youtube installation flow; no comparative
speed, reliability or usability advantage has been measured. See
[Development direction](ARCHITECTURE.md#development-direction) and the
[prior-work review](SOURCES.md#2026-10-01--android-installer-positioning-review)
for the detailed comparison and additional Android/Termux precedents.

Please visit and support the original projects. No endorsement by their
maintainers is implied.
