# Security policy

Bloatware Exorcism is an experimental Android root application. Root access can change package availability and leave a device unusable. A successful test on one firmware does not establish safety on another.

## Reporting a vulnerability

Use this repository's **Security → Advisories → Report a vulnerability** when the private reporting option is available. Do not publish exploit details, personal device reports or credentials in public Issues. If private reporting is unavailable, open an Issue requesting a private reporting channel without including sensitive details. No response-time commitment or independent security certification is claimed.

## Supported versions

Security reports should target the current source on `main` and identify the exact commit and APK SHA-256. Older development builds are retained as historical test releases; no supported-version security guarantee is offered.

## Privileges and data

| Capability | Purpose | Source |
|---|---|---|
| `android.permission.QUERY_ALL_PACKAGES` | Discover installed system packages and inspect their declared components | `app/src/main/AndroidManifest.xml` |
| KernelSU / `su` authorization | Run package inspection, current-user disable/uninstall/restore commands and optional rescue operations | `Shell.java`, `Engine.java`, `Rescue.java` |
| Android document picker | Import/export snapshots and validation reports after a user selects a document | `MainActivity.java` |

Root authorization is separate from Android manifest permissions. The manifest declares no INTERNET, camera, microphone, location or broad storage permission. The UI, font and Material dependencies are bundled locally. Android app backup is disabled; uninstalling the app can remove its internal snapshots and history. Export Holy Backup before uninstalling or changing signing certificates. Exports contain device/build and package information: review them before sharing.

## Controls visible in the source

- `Policy.validPackage` restricts package names before they enter shell commands. Actions and numeric enabled states are selected from fixed values.
- `Engine.allowed` checks the catalog, system-app status, protected names, persistent applications, system UIDs and dynamically protected roles before package mutation.
- Mutation batches are limited to 30 packages and target the active Android user. The engine records original states and checks the observed state after operations.
- The WebView blocks network loads and navigation, disables file/content access, and exposes a native bridge for the bundled UI. Debug builds enable WebView debugging; release builds do not.
- Rescue transactions use validated fields, a package allowlist, fingerprint matching, locking and root-owned state. The rescue module is optional and experimental.

These are implementation observations, not a complete audit or a proof of safety.

## Known limitations and review priorities

1. Root commands run through `su -c`. Review every caller and its validation when adding new commands; never accept arbitrary shell input from UI or imports.
2. A protected list and catalog cannot encode every firmware dependency. A package that passes policy checks can still be required by a particular ROM.
3. Restoring one history entry checks static protected names and the catalog; its checks differ from `Engine.allowed`. Review the restoration path, imported records and dynamic roles before broad deployment.
4. Killing a timed-out `su` process does not prove all child commands stopped. Rescan uncertain operations before retrying.
5. Holy Backup stores package states, not private app data, complete partitions or firmware images. Current-user uninstall does not reclaim the original system partition files.
6. Rescue needs accessible `/data`, working KernelSU boot scripts, suitable SELinux permissions and a responsive PackageManager. It cannot guarantee recovery from bootloops or firmware damage.
7. Llkk77 reported successful tests on a POCO M6 PRO. Exact ROM/Android/KernelSU versions, per-operation evidence and wider Android Studio testing are pending documentation. No independent root-code audit has been completed.
8. VirusTotal results and passing CI are additional evidence only. Neither establishes safety, and reproducible byte-for-byte release builds have not been demonstrated.

## Review and test evidence

See [the root review notes](docs/ROOT_REVIEW.md), [the device test checklist](docs/DEVICE_TESTING.md), [verification history](VERIFICATION.md), and [build/release instructions](docs/BUILD_AND_RELEASE.md). Do not promote a package/build to validated solely from synthetic tests or an emulator without equivalent root/firmware behavior.
