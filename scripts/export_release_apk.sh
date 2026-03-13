#!/bin/bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_APK="$ROOT_DIR/app/build/outputs/apk/release/app-release.apk"
TARGET_APK="$ROOT_DIR/release/army-diet.apk"

cd "$ROOT_DIR"

./gradlew assembleRelease

mkdir -p "$ROOT_DIR/release"
cp "$OUTPUT_APK" "$TARGET_APK"

echo "Exported release APK to $TARGET_APK"
