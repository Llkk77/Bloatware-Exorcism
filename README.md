<p align="center">
  <img src="banner.jpg" alt="Bloatware Exorcism — Clean your Android. Reclaim control." width="100%">
</p>

<p align="center">
  <img src="icon.jpg" alt="Bloatware Exorcism icon" width="80" height="80">
</p>

# Bloatware Exorcism

Offline MIUI/HyperOS bloatware management with KernelSU, reversible package changes and a Material Design 3 interface.

## Downloads

- [APK and version releases](https://github.com/Llkk77/Bloatware-Exorcism/releases)
- [Complete Android project ZIP](BloatwareExorcism-project.zip)
- [v0.3.1 release notes](RELEASE_NOTES.md)
- [Verification and limitations](VERIFICATION.md)

The complete, buildable source tree is provided in **BloatwareExorcism-project.zip**. Extract it and open the `BloatwareExorcism` folder in Android Studio. The archive includes the Gradle wrapper, native Java code, local Material Web interface, catalog, tests, documentation and third-party license notices. Private signing keys are excluded.

## Features

| Feature | Function |
|---|---|
| Exorcise | Disable or remove manually selected classified system packages for the current Android user |
| Detect Demons | Inspect installed packages and declared components |
| Holy Backup | Save, export, import and compare package-state snapshots |
| Resurrection | Restore recorded package changes |
| Deep Exorcism | Review advanced packages and audit native services; optional experimental KernelSU rescue module |

## Version 0.3.1

- Official Material Web components bundled offline, with Roboto typography and tonal surfaces.
- Dark dynamic colors on compatible Android devices; Exorcism blue fallback.
- Ripple feedback, smooth transitions, reduced-motion handling and adaptive navigation.
- One-tap ordinary selected-package actions with automatic backup; advanced packages retain review.
- No INTERNET permission, ads or analytics.

<p align="center">
  <img src="features.jpg" alt="Illustration of package detection, removal, backup and restoration" width="100%">
</p>

*Promotional illustration, not an app screenshot or measured results. Holy Backup saves package states, not full system backups. The depicted counts are illustrative; performance and storage gains are not verified.*

## Requirements and installation

Android 10 or later. Initial focus: MIUI/HyperOS Global and China ROMs, Android 16 and KernelSU.

1. Download and install the test APK from Releases.
2. Grant root access through KernelSU.
3. Export a Holy Backup before changing packages.
4. Review the selection, apply the desired action and check device functions afterward.

The certificate matches v0.2.2. This is a development-signed test build.

## Build from the source archive

Requirements: JDK 17, Android SDK Platform 36 and Build Tools 36.0.0. Gradle 8.11.1 and Android Gradle Plugin 8.10.1 are configured in the project.

```sh
unzip BloatwareExorcism-project.zip
cd BloatwareExorcism
sh gradlew assembleDebug lintDebug
```

Material dependencies and the font are already bundled in the APK assets. Node is only needed to rebuild the UI dependency bundle; see the included README and `docs/MATERIAL3.md`.

## Validation and recovery limits

Compilation, lint, policy/fixture tests and controlled browser UI/accessibility checks passed. No physical Android device or emulator was used. KernelSU behavior, firmware safety, dynamic colors and boot rescue remain unvalidated on real devices. No performance gain is claimed.

Holy Backup records package states, not personal app data or firmware images. Root removal uses the current-user package operation and retains original system files; it is not partition cleanup. Core/protected packages remain blocked. The rescue module is experimental and cannot guarantee recovery.

## License

MIT. Material, Lit and Roboto notices are included in the source archive.

## Contributions

- Llkk77
- ChatGPT

<p align="center">
  <img src="footer.jpg" alt="The Exorcism Continues" width="100%">
</p>
