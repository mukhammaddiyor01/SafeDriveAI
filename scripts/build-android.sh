#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd "$(dirname "$0")/.." && pwd)"
source "$project_root/scripts/env.sh"
"$project_root/scripts/prepare-face-model.sh"
cd "$project_root/mobile"
flutter pub get
flutter analyze
flutter test
flutter build apk --release --target-platform android-arm64
mkdir -p "$project_root/releases"
cp build/app/outputs/flutter-apk/app-release.apk "$project_root/releases/SafeDriveAI.apk"
printf '\nAPK: %s\n' "$project_root/releases/SafeDriveAI.apk"
