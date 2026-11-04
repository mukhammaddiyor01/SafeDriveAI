#!/usr/bin/env bash
# Source this file to use project-local tools when present.
safedrive_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [ -d "$safedrive_root/.tools/flutter" ]; then
  export PATH="$safedrive_root/.tools/flutter/bin:$PATH"
  export PUB_CACHE="$safedrive_root/.tools/pub-cache"
  export GRADLE_USER_HOME="$safedrive_root/.tools/gradle"
fi
if [ -d "$safedrive_root/.tools/jdk/Contents/Home" ]; then
  export JAVA_HOME="$safedrive_root/.tools/jdk/Contents/Home"
fi
if [ -d "$safedrive_root/.tools/android-sdk" ]; then
  export ANDROID_HOME="$safedrive_root/.tools/android-sdk"
  export ANDROID_SDK_ROOT="$ANDROID_HOME"
  export PATH="$ANDROID_HOME/platform-tools:$PATH"
fi
