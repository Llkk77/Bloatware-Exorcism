# v0.3.0 — Material You

**Bloatware Exorcism — by ChatGPT**

An offline MIUI/HyperOS system-package manager for Global and China ROMs, with KernelSU root access and a focus on reversible changes.

## Changes

- Material Design 3 interface with locally bundled official Material Web components.
- Dark dynamic colors on compatible Android versions and an Exorcism blue fallback.
- Bundled Roboto, tonal surfaces, ripple feedback, smooth transitions and reduced-motion support.
- Adaptive bottom navigation, navigation rail and expanded side navigation.
- Explicit one-tap actions for manually selected ordinary packages, with an automatic Holy Backup before the batch.
- Advanced package review, protected-package restrictions, restoration, validation reports and experimental rescue support retained.

## Download

Install `BloatwareExorcism-0.3.0-test.apk`. Android 10 or later is required; Android 16 with KernelSU is the initial priority. Authorize root access in KernelSU before applying changes.

The development certificate matches v0.2.2. This is a test build, not a production release-signed APK.

## Verification and limits

Android compilation and lint passed. Policy tests, synthetic dumpsys fixtures, UI bridge tests and automated browser accessibility/layout checks passed. No physical Android device or emulator was used. Root behavior, dynamic system colors and rescue behavior still require device testing. No performance improvement or firmware safety is claimed.

Holy Backup records package states, not personal app data or firmware images. Export backups before changing the system.

## Source and licensing

MIT project license. Material and Roboto license notices are included. Private signing keys, local SDK paths, build caches and dependencies must not be uploaded to the public repository or attached to this release.
