#!/usr/bin/env bash
set -euo pipefail

commit="4024bf0048c261db58ef98cb5e16de291c429f4e"
sha256="49582f2c3a3abcf32819d25ec428d6350e15742e4642c9da8cdac7b8a76e8d79"
root="$(cd "$(dirname "$0")/.." && pwd)"
destination="$root/app/src/main/jni/dosbox-libretro"
archive="$root/upstream/dosbox-libretro-$commit.tar.gz"

mkdir -p "$root/upstream" "$(dirname "$destination")"
curl --fail --location --retry 3 \
  "https://github.com/libretro/dosbox-libretro/archive/$commit.tar.gz" \
  -o "$archive"
echo "$sha256  $archive" | sha256sum --check --strict
rm -rf "$destination"
mkdir -p "$destination"
tar -xzf "$archive" --strip-components=1 -C "$destination"
patch --batch --forward -p1 -d "$destination" < "$root/patches/dosbox-libretro-android.patch"
