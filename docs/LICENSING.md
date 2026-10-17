# License

[← README](../README.md)

This installer is licensed under **GPL-3.0-only**. The full terms are in
[LICENSE](../LICENSE); attribution is in [NOTICE](../NOTICE).

## Why GPL-3.0-only?

The implementation includes adaptations of GPL-licensed upstream code,
including the XML signature layout in
[Wgt.kt](../core/src/main/kotlin/dev/readiz/wgtinstaller/core/Wgt.kt).
Both recorded implementation sources explicitly declare **GPL-3.0-only**:

| Source | Explicit license declaration | License text |
|---|---|---|
| SushyDev/tizen-homebrew | [package.json at f90b005](https://github.com/SushyDev/tizen-homebrew/blob/f90b005c8c354cafc027f12b37fe04b6cf0162f2/package.json) | [LICENSE](https://github.com/SushyDev/tizen-homebrew/blob/f90b005c8c354cafc027f12b37fe04b6cf0162f2/LICENSE) |
| reisxd/tizen.js | [package.json at 45b0010](https://github.com/reisxd/tizen.js/blob/45b00106149d0c8eff74ead4a979c909dd80efd6/package.json) | [LICENSE](https://github.com/reisxd/tizen.js/blob/45b00106149d0c8eff74ead4a979c909dd80efd6/LICENSE) |

The upstream declarations, local SPDX headers and recorded adaptations support
the current license choice. Using a protocol alone does not establish code
derivation; this review also checked the adapted signature implementation
against its recorded source.

“Only” means version 3, without an automatic option to use a later GPL version.
See the [SPDX GPL-3.0-only entry](https://spdx.org/licenses/GPL-3.0-only.html).
A generic GPL-3.0 label does not override the upstream projects’ explicit
declarations.

## Other components

The Kotlin standard library is separately licensed under
[Apache-2.0](https://github.com/JetBrains/kotlin/blob/v2.1.21/license/LICENSE.txt).
Apache confirms that Apache-2.0 code can be included in a
[GPLv3 project](https://www.apache.org/licenses/GPL-compatibility);
its applicable notices still need to be preserved.

Apps2Samsung is credited as related Android prior work and a guide reference,
not as an adapted implementation source. WGT applications downloaded by the
installer are separate upstream works with their own licenses.

## Redistribution

Redistributors must follow the GPL terms, including preserving required
notices and providing the corresponding source when distributing binaries.
For online releases, see section 6(d) of [LICENSE](../LICENSE).

Reviewed on 2026-10-17 against the pinned sources above, local source headers
and the Kotlin 2.1.21 dependency metadata. Matching upstream blob IDs are
recorded in [Source traceability](SOURCES.md#2026-10-17--license-review).
This review establishes the recorded license choice; it is not a complete
audit of every binary’s bundled notices.
