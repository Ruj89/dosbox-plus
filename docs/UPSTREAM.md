# Upstream dosbox-libretro

`scripts/fetch-libretro.sh` downloads commit
`4024bf0048c261db58ef98cb5e16de291c429f4e` of `libretro/dosbox-libretro`,
checks its SHA-256 value, and expands it under `app/src/main/jni`. That directory
is intentionally untracked reproducible upstream source.
The fetch script then applies `patches/dosbox-libretro-android.patch`, which
disables the obsolete ARMv7 dynarec that is incompatible with modern Android
Clang and records the pinned version without requiring Git in the image.

The local `libretro_host.cpp` implements the Android frontend callbacks and is
compiled in the same NDK build. Release source bundles must contain both this
repository and the pinned core source, as required by GPL-2.0-or-later.
