# Device and Android Studio test record

## Current evidence

Llkk77 reported that all tests already performed on a POCO M6 PRO completed successfully. A broader review and Android Studio test campaign is planned. The table below is a blank record for that campaign, not a completed result.

| Environment | Record before testing |
|---|---|
| App | Version, versionCode, source commit, APK SHA-256, signing certificate SHA-256 |
| Device | Exact model and variant; distinguish POCO M6 Pro variants |
| Firmware | Android version, full MIUI/HyperOS version, build fingerprint and region |
| Root | KernelSU version, kernel and root authorization status |
| Emulator | Android Studio/AVD image and API level; state whether real `su` exists |
| Rescue | Module version and whether installed/enabled |

Use a spare device or a recoverable test environment. Export package-state backups first and keep an external recovery method available. Do not disable essential packages or deliberately cause boot failure to fill a checklist. A standard AVD can test UI/denied-root behavior but does not reproduce Xiaomi firmware or KernelSU.

| Test | Expected result | Actual result / evidence |
|---|---|---|
| Launch, navigation, font scale and rotation | UI works without clipping; no unexpected crash | Pending |
| Root denied/unavailable | Mutation is refused with a clear message | Pending |
| Scan and inspect | Package details match independent device inspection | Pending |
| Protected/unknown package selection | Protected or unclassified mutations are refused | Pending |
| Disable one reviewed nonessential package | Only the selected current-user package changes | Pending |
| Restore disabled package | Original enabled state is confirmed | Pending |
| Current-user uninstall and restore | Original system APK remains; install-existing restores availability | Pending |
| Export/import/compare Holy Backup | Snapshot parses and preserves intended package states | Pending |
| Cancel advanced action | No package state changes | Pending |
| Normal reboot after a small change | System boots; state and core functions checked | Pending |
| Wi-Fi, calls, camera, notifications, Bluetooth/NFC | Record pass/fail/not applicable individually | Pending |
| Optional rescue status/arm/manual rollback | Transaction state and restoration verified independently | Pending |
| Invalid import and malformed package names | Rejected without shell execution or state changes | Pending |
| Update signing compatibility | Same certificate updates; migration warning for a new certificate | Pending |

Record failures as well as successes. Use the app's validation export described in [VALIDATION.md](VALIDATION.md), redact private information, and attach real screenshots of Home, package inspection, backup, restoration and About. Store reviewed captures under `docs/screenshots/` and add them to the README when available. Promotional images are not test evidence.
