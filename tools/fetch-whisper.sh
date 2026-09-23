#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="v1.9.4"
DEST="$ROOT/third_party/whisper.cpp"
TMP="$ROOT/.whisper-download"
if [[ -f "$DEST/src/whisper.cpp" ]]; then exit 0; fi
rm -rf "$TMP"
mkdir -p "$TMP"
curl -L --fail --retry 3 "https://github.com/ggml-org/whisper.cpp/archive/refs/tags/${VERSION}.tar.gz" -o "$TMP/whisper.tar.gz"
tar -xzf "$TMP/whisper.tar.gz" -C "$TMP"
rm -rf "$DEST"
mv "$TMP/whisper.cpp-${VERSION#v}" "$DEST"
rm -rf "$TMP"
echo "Fetched whisper.cpp ${VERSION}"
