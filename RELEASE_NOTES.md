# v0.3.1 — Interface polish

- Credits are grouped under About → Contributions and in the README.
- Cleaner release and repository presentation.
- Existing package actions and safeguards are unchanged.

## Download

Install `BloatwareExorcism-0.3.1-test.apk`. Android 10 or later is required. This development-signed test build can update v0.3.0.

The full Android source is attached as `BloatwareExorcism-project.zip`; hashes are in `SHA256SUMS.txt`.

## Verification

Android compilation and lint passed. The original build-time checks did not use a physical device or emulator. Llkk77 subsequently reported successful tests on a POCO M6 PRO; broader review and Android Studio testing are pending documentation. Export Holy Backup before modifying packages; it records package states, not personal app data or firmware images. The rescue module remains experimental.

## Repository transparency update — 2026-10-22

- Source published as a navigable file tree, with original app behavior preserved.
- Security, permissions, root review notes, testing checklist and build/release instructions added.
- CI and manual dedicated-key signing workflows added; signing requires maintainer-owned secrets.
- Existing v0.3.1 APK unchanged. SHA-256: `004d83c55742676bd65e226e939297d23da1d40838c2fd61a08d13cc07253027`.
- No independent audit, reproducible-build proof, external store approval or new device-test result is claimed.
