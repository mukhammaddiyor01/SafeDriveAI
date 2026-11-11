#!/usr/bin/env bash
# Run the supplied open-eye face crop through the APK already installed by USB.
set -euo pipefail
project_root="$(cd "$(dirname "$0")/.." && pwd)"
source "$project_root/scripts/env.sh"
sample="${1:?Supply a local open-eye face-crop image for the release regression}"
test -f "$sample"
apk="$project_root/releases/SafeDriveAI.apk"
mapping="$project_root/mobile/build/app/outputs/mapping/release/mapping.txt"
test -f "$apk" && test -f "$mapping"
remote_apk=$(adb -d shell pm path uz.safedrive.safedrive_ai | tr -d '\r' | sed -n '1s/^package://p')
test -n "$remote_apk"
local_hash=$(shasum -a 256 "$apk" | awk '{print $1}')
remote_hash=$(adb -d shell sha256sum "$remote_apk" | awk '{print $1}')
if [ "$local_hash" != "$remote_hash" ]; then
  echo 'Install releases/SafeDriveAI.apk before running this check.' >&2
  exit 1
fi
symbol_file=$(mktemp)
fixture="$project_root/mobile/android/app/src/androidTest/assets/side.jpg"
test ! -e "$fixture"
cleanup() {
  rm -f "$fixture" "$symbol_file"
  adb -d uninstall uz.safedrive.safedrive_ai.test >/dev/null 2>&1 || true
}
trap cleanup EXIT
mkdir -p "$(dirname "$fixture")"
cp "$sample" "$fixture"
python3 - "$mapping" > "$symbol_file" <<'PY'
from pathlib import Path
import re
import sys
part = Path(sys.argv[1]).read_text().split(
    "uz.safedrive.safedrive_ai.controller.DenseEyeDetector -> ", 1)[1]
print(part.split(":", 1)[0])
part = part.split("\nuz.", 1)[0]
print(re.search(
    r"java.lang.Boolean observe\(android.graphics.Bitmap,android.graphics.Rect,float\).* -> (\w+)",
    part)[1])
PY
(
  cd "$project_root/mobile/android"
  ./gradlew :app:assembleDebugAndroidTest -PeyeReleaseProbe=true
)
adb -d install -t -r "$project_root/mobile/build/app/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
result=$(adb -d shell am instrument -w   -e modelClass "$(sed -n '1p' "$symbol_file")"   -e observeMethod "$(sed -n '2p' "$symbol_file")"   uz.safedrive.safedrive_ai.test/uz.safedrive.safedrive_ai.controller.ReleaseEyeProbe)
printf '%s\n' "$result"
grep -q 'INSTRUMENTATION_CODE: -1' <<< "$result"
adb -d shell am start -n uz.safedrive.safedrive_ai/.MainActivity
