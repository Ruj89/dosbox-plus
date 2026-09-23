# Upstream dosbox-libretro

`scripts/fetch-libretro.sh` downloads commit
`4024bf0048c261db58ef98cb5e16de291c429f4e` of `libretro/dosbox-libretro`,
checks its SHA-256 value, and expands it under `app/src/main/jni`. That directory
is intentionally untracked reproducible upstream source.
The fetch script then applies `patches/dosbox-libretro-android.patch`, which
disables the obsolete ARMv7 dynarec that is incompatible with modern Android
Clang and records the pinned version without requiring Git in the image.
It also makes `GFX_Events` honor the frontend exit request so DOSBox unwinds
its configuration and frees mounted drives and memory before deleting the
libco coroutine.

The local `libretro_host.cpp` implements the Android frontend callbacks and is
compiled in the same NDK build. Release source bundles must contain both this
repository and the pinned core source, as required by GPL-2.0-or-later.

## Session lifecycle

The pinned core is not reentrant: `retro_deinit` does not reset `mainThread`,
and a later `retro_init` refuses to create an emulator coroutine. The JNI host
therefore loads `libretro.so` with `dlopen` for each session and closes it after
`retro_unload_game`/`retro_deinit` on the same worker thread. It must not have a
link-time dependency on `retro`, or that reference would prevent unloading.
`APP_MODULES` still packages both shared libraries in each APK.

The host explicitly selects `RETRO_DEVICE_KEYBOARD` on port 0 after loading.
This calls the core's `MAPPER_Init`, which installs the keyboard callback;
without it, touchscreen keyboard events were silently dropped. The JNI status
API distinguishes starting, running, stopped and failed sessions for the UI.

`scripts/test-core-session.sh` exercises the real pinned core on Linux using
an original, tiny DOS program that animates using BIOS ticks and exits on Esc.
It checks four successive launches, changing video, keyboard input, explicit
frontend shutdown and DOS-requested shutdown. This is a host-side regression
test; device/ABI-specific behavior still needs Android execution.
