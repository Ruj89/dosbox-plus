#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

image=dosbox-plus-build
volume=dosbox-plus-signing
mkdir -p dist
dist_dir="$(realpath dist)"

docker build --target build -t "$image" .
docker volume create "$volume" >/dev/null
docker run --rm \
    --mount "type=volume,source=$volume,target=/root/.android" \
    --mount "type=bind,source=$dist_dir,target=/dist" \
    "$image" bash -euo pipefail -c '
        gradle --no-daemon --console=plain assembleDebug
        cp app/build/outputs/apk/debug/*.apk /dist/
    '
