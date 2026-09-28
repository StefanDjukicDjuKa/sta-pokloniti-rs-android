#!/usr/bin/env sh
set -e
cd "$(dirname "$0")"

if [ ! -f local.properties ]; then
  if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME" ]; then
    SDK="$ANDROID_HOME"
  elif [ -d "$HOME/Library/Android/sdk" ]; then
    SDK="$HOME/Library/Android/sdk"
  elif [ -d "$HOME/Android/Sdk" ]; then
    SDK="$HOME/Android/Sdk"
  else
    echo "Android SDK nije pronađen. Instaliraj Android Studio i otvori projekat jednom."
    exit 1
  fi
  printf 'sdk.dir=%s\n' "$SDK" > local.properties
fi

./gradlew assembleDebug
mkdir -p OUTPUT
cp app/build/outputs/apk/debug/app-debug.apk OUTPUT/STA_POKLONITI_RS.apk
echo "GOTOVO: $(pwd)/OUTPUT/STA_POKLONITI_RS.apk"
