#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${RELEASE_KEYSTORE_BASE64:?Configure the release keystore secret}"
: "${RELEASE_STORE_PASSWORD:?Configure the keystore password secret}"
: "${RELEASE_KEY_ALIAS:?Configure the key alias secret}"
: "${RELEASE_KEY_PASSWORD:?Configure the key password secret}"
: "${RUNNER_TEMP:?Runner temporary directory is required}"
: "${RELEASE_TAG:?A reviewed release tag is required}"
[[ "$RELEASE_TAG" =~ ^v[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9.]+)?$ ]]
sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
: "${sdk_root:?Android SDK is required}"
keystore_path="$RUNNER_TEMP/exorcism-release.jks"
trap 'rm -f "$keystore_path"' EXIT
umask 077
printf '%s' "$RELEASE_KEYSTORE_BASE64" | base64 --decode > "$keystore_path"
mkdir -p dist
apk_path="dist/BloatwareExorcism-${RELEASE_TAG#v}.apk"
"$sdk_root/build-tools/36.0.0/zipalign" -f -p 4 app/build/outputs/apk/release/app-release-unsigned.apk "$RUNNER_TEMP/exorcism-aligned.apk"
"$sdk_root/build-tools/36.0.0/apksigner" sign --ks "$keystore_path" --ks-key-alias "$RELEASE_KEY_ALIAS" --ks-pass env:RELEASE_STORE_PASSWORD --key-pass env:RELEASE_KEY_PASSWORD --out "$apk_path" "$RUNNER_TEMP/exorcism-aligned.apk"
"$sdk_root/build-tools/36.0.0/apksigner" verify --verbose --print-certs "$apk_path" > dist/CERTIFICATE.txt
cd dist
sha256sum *.apk > SHA256SUMS.txt
