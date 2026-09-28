#!/usr/bin/env sh
set -e
GRADLE_VERSION=8.9
BASE="$HOME/.gradle/sta-pokloniti-dist"
GRADLE_DIR="$BASE/gradle-$GRADLE_VERSION"
ZIP="${TMPDIR:-/tmp}/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$GRADLE_DIR/bin/gradle" ]; then
  echo "[STA POKLONITI RS] Prvo pokretanje - preuzimam Gradle $GRADLE_VERSION..."
  mkdir -p "$BASE"
  if command -v curl >/dev/null 2>&1; then
    curl -L "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -O "$ZIP"
  else
    echo "Potrebni su curl ili wget."
    exit 1
  fi
  rm -rf "$GRADLE_DIR"
  unzip -q "$ZIP" -d "$BASE"
fi

exec "$GRADLE_DIR/bin/gradle" "$@"
