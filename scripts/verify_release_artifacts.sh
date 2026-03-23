#!/bin/bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEBUG_APK="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
RELEASE_APK="$ROOT_DIR/app/build/outputs/apk/release/app-release.apk"
RELEASE_AAB="$ROOT_DIR/app/build/outputs/bundle/release/app-release.aab"
TRACKED_APK="$ROOT_DIR/release/army-diet.apk"

for artifact in "$DEBUG_APK" "$RELEASE_APK" "$RELEASE_AAB" "$TRACKED_APK"; do
    if [ ! -f "$artifact" ]; then
        echo "Missing artifact: $artifact"
        exit 1
    fi
done

cmp -s "$RELEASE_APK" "$TRACKED_APK" || {
    echo "Tracked APK does not match release APK"
    exit 1
}

echo "Verified release artifacts and tracked APK sync"
