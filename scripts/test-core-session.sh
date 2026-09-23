#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
core="$(realpath "${1:-$root/app/src/main/jni/dosbox-libretro}")"
work="$(mktemp -d "${TMPDIR:-/tmp}/dosbox-session-test.XXXXXX")"
trap 'rm -rf "$work"' EXIT

# Disable GNU-unique symbols on the Linux test build so dlclose can unload it,
# matching Android's Clang/Bionic build. Use the interpreter, as on ARM Android.
make -C "$core" -j"${JOBS:-4}" GIT_VERSION=4024bf0 WITH_DYNAREC= CXX='g++ -std=gnu++14 -fno-gnu-unique'
g++ -std=c++17 -pthread -I"$root/app/src/main/cpp" -I"$core/libretro" \
    "$root/app/src/test/cpp/core_session_test.cpp" -ldl -o "$work/core-session-test"
as --32 "$root/app/src/test/cpp/live_video.S" -o "$work/live.o"
objcopy -O binary -j .text "$work/live.o" "$work/LIVE.COM"
cat > "$work/test.conf" <<EOF
[cpu]
core=normal
cycles=10000
[autoexec]
mount c "$work"
c:
LIVE.COM
exit
EOF
timeout 30s "$work/core-session-test" "$core/dosbox_libretro.so" "$work/test.conf" "$work"
