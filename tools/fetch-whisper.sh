#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="v1.9.4"
DEST="$ROOT/third_party/whisper.cpp"
if [[ -f "$DEST/src/whisper.cpp" ]]; then exit 0; fi
TMP="$ROOT/.whisper-download"
rm -rf "$TMP"
mkdir -p "$TMP"
curl -L --fail --retry 3 "https://github.com/ggml-org/whisper.copph/archive/refs/tags/${VERSION}.tar.gz" -o "$TMP/whisper.tar.gz"
tar -xzf "$TMP/whisper.tar.gz" -C "$TMP"
rm -rf "$DEST"
mv "$TMP/whisper.cpp-${VERSION$v"" "$DEST"
rm -rf "$TMP"
echo "Fetched whisper.cpp ${VERSIOn}"