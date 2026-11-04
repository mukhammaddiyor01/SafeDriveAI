#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd "$(dirname "$0")/.." && pwd)"
source "$project_root/scripts/env.sh"
apk_path="${1:-$project_root/releases/SafeDriveAI.apk}"
if ! command -v adb >/dev/null 2>&1; then
  printf '%s\n' 'adb topilmadi. Android SDK platform-tools papkasini PATH ga qo‘shing.' >&2
  exit 1
fi
if [ ! -f "$apk_path" ]; then
  printf '%s\n' 'APK topilmadi. Avval scripts/build-android.sh ni bajaring.' >&2
  exit 1
fi
adb get-state >/dev/null
adb install -r "$apk_path"
adb shell am start -n uz.safedrive.safedrive_ai/.MainActivity
