#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd "$(dirname "$0")/.." && pwd)"
model_path="$project_root/mobile/android/app/src/main/assets/face_landmarker.task"
expected='64184e229b263107bc2b804c6625db1341ff2bb731874b0bcc2fe6544e0bc9ff'
mkdir -p "$(dirname "$model_path")"
if [ ! -f "$model_path" ]; then
  temporary=$(mktemp)
  trap 'rm -f "$temporary"' EXIT
  curl -fsSL --retry 2     https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task     -o "$temporary"
  printf '%s  %s\n' "$expected" "$temporary" | shasum -a 256 -c -
  mv "$temporary" "$model_path"
fi
printf '%s  %s\n' "$expected" "$model_path" | shasum -a 256 -c -
