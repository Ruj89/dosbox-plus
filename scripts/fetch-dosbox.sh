#!/usr/bin/env bash
set -euo pipefail
version=0.74-3
archive="dosbox-${version}.tar.gz"
url="https://sourceforge.net/projects/dosbox/files/dosbox/${version}/${archive}/download"
# Update only from the official DOSBox release checksum before changing this pin.
sha256="c0d13dd7ed2ed363b68de615475781e891cd582e8162b5c3669137502222260a"
root="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$root/upstream"
curl --fail --location --retry 3 "$url" -o "$root/upstream/$archive"
echo "$sha256  $root/upstream/$archive" | sha256sum --check --strict
rm -rf "$root/upstream/dosbox"
tar -xzf "$root/upstream/$archive" -C "$root/upstream"
mv "$root/upstream/dosbox-$version" "$root/upstream/dosbox"
