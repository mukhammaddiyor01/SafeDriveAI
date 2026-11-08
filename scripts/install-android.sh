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
device_serial="${ANDROID_SERIAL:-}"
if [ -z "$device_serial" ] && ! device_serial="$(adb -d get-serialno)"; then
  printf '%s\n' 'Bitta USB telefon ulang, USB debugging’ni yoqing va telefonda kompyuterga ruxsat bering.' >&2
  printf '%s\n' 'Qurilmalar ro‘yxati uchun adb devices -l ni bajaring. Bir nechta telefon bo‘lsa ANDROID_SERIAL ni belgilang.' >&2
  exit 1
fi
if ! device_state="$(adb -s "$device_serial" get-state)" || [ "$device_state" != device ]; then
  printf 'Qurilma tayyor emas (%s). Kabel va telefondagi USB debugging ruxsatini tekshiring.\n' "$device_serial" >&2
  exit 1
fi
adb -s "$device_serial" install -r "$apk_path"
adb -s "$device_serial" shell am start -n uz.safedrive.safedrive_ai/.MainActivity
