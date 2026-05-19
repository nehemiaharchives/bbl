#!/usr/bin/env bash

set -euo pipefail

usage() {
  echo "Usage: $0 <translation-key>" >&2
  echo "Example: $0 irvtel" >&2
}

if [[ $# -ne 1 ]]; then
  usage
  exit 1
fi

translation="$1"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "$script_dir"
echo "Rebuilding bbl pack lucene-kmp index for: $translation"
echo
exec ./gradlew :cli:packer:packBblTranslation -Pbblpack.translation="$translation"
