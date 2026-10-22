# Build, signing and artifact verification

## Android Studio

Clone this repository and open its root directory in Android Studio. Install JDK 17, Android SDK Platform 36 and Build Tools 36.0.0. Use the included Gradle 8.11.1 wrapper with Android Gradle Plugin 8.10.1.

```sh
git clone https://github.com/Llkk77/BloatwareExorcism-Android-Debloater.git
cd BloatwareExorcism-Android-Debloater
sh gradlew assembleDebug assembleRelease lintDebug
```

`assembleRelease` produces an unsigned release APK. A locally generated debug certificate is for testing and may differ from the certificate of existing test releases. Do not uninstall an existing installation before exporting its backups/history. UI dependencies are already bundled; Node is only needed to rebuild those dependencies.

## CI

The Android CI workflow runs the Java policy/fixture checks, Python fixture/rescue tests, Android compilation and lint. It uploads unsigned release and debug APKs, their SHA-256 hashes and source/run metadata. CI debug APKs are temporary development builds with a runner-generated certificate; they are not the published release identity. An unsigned release APK cannot be installed before signing. A passing workflow does not certify root behavior or firmware safety.

## Dedicated release key

Create and retain a private release keystore locally using Android Studio's **Generate Signed Bundle / APK** flow. Keep encrypted backups under the maintainer's control. Never commit a keystore or send passwords in an Issue or chat.

Configure repository Actions secrets `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD` through GitHub's secret-management UI. The signing workflow fails if any is absent; it does not fall back to a debug key. Existing development-signed APKs remain unchanged.

Changing the signing identity generally prevents an in-place update unless a supported signing-key rotation path is deliberately established. This project does not implement such a migration. Announce the transition, export Holy Backup/reports first, and explain that uninstalling deletes internal application data. The first release signed with the new key must also advance versionCode/versionName before tagging; the workflow does not silently change them.

## Publish a signed prerelease

After review and testing, create a `v<versionName>` tag pointing to the reviewed commit and manually dispatch **Signed Android prerelease** with that tag. The workflow checks the tag against the Gradle version, builds and tests that source, aligns and signs the APK, verifies its certificate, calculates SHA-256, uploads the exact output as an Actions artifact, then publishes those same files to GitHub Releases. The release body includes source/run identity and the APK hash. It refuses an existing release instead of replacing its assets.

The private key stays in a temporary runner file and is removed even if signing fails. Only the signed APK, certificate report, checksums and metadata are uploaded. Changes to workflows/scripts must be reviewed before providing signing secrets. Third-party Actions are pinned to commit SHAs and tracked by Dependabot.

## Verify a download

Download the APK and `SHA256SUMS.txt` from the same release. Run `sha256sum -c SHA256SUMS.txt` in that directory, or compare `Get-FileHash <APK> -Algorithm SHA256` on Windows. Inspect the certificate with Android SDK `apksigner verify --verbose --print-certs <APK>`. Match the hash to both the release body and the workflow artifact. This establishes file identity, not safety. Byte-for-byte reproducible builds are not yet claimed.

## External checks and distribution

The repository must link only to an actual VirusTotal report for the exact APK hash; an absence of detections is not a security guarantee. No F-Droid/IzzyOnDroid review, acceptance or independent audit is claimed. Those submissions and community beta recruitment follow maintainer review and documented device tests.
