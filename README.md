# DOSBox Plus

Android 8+ frontend for the GPL `dosbox-libretro` core, with an importable DOS game
library and touch-input profiles. Games are never bundled.

The built-in HUD editor provides separate portrait/landscape canvases, visual
drag/resize controls, key assignment, opacity, hold behavior, layering, and
undo/redo. Profiles can be global or assigned independently to a game.

## Getting started

The home screen guides you through adding your first game. Tap **Add your first game**:

1. **Folder**: choose an already extracted folder containing all the game files.
2. **Launch**: assign a name and choose the `.exe`, `.bat`, or `.com` file from the list.
   A manual path and optional arguments are available under **Advanced options**.
   Under **Game controls**, you can choose a ready-made configuration or keep
   the common controls.
3. **Ready**: launch the game or customize its controls.

The bottom navigation bar separates **Games**, **Controls**, and **Help**. Under Controls,
you can create a shared layout; the Controls button on a game card instead opens
that game's personal configuration.

The rationale and sources behind the interface design choices are documented in
[docs/UX-DESIGN.md](docs/UX-DESIGN.md).

## Controls editor

* Open **Controls → Customize controls** or **Controls** under a game. Each configuration
  contains two independent layouts: **Landscape** and **Portrait**.
* With **+ Button**, search for a key (letters, numbers, F1–F12,
  modifiers, symbols, navigation keys, or numeric keypad). Then tap
  the preview where you want to place it, or drag the button.
* Tap a control to select it, then **Edit** on phones; on larger screens,
  the properties appear next to the preview. You can freely change
  **Button text** (label / placeholder), assigned key, position, size, opacity,
  and press behavior. Drag **↘** to resize it; **Duplicate**,
  **Send to back**, and **Bring to front** let you organize the layout.
  Fine adjustments are available under **Size, position, and layers**.
* **Undo / Redo** treat a drag operation or a slider adjustment as a single change.
  **Test** highlights the touched control and shows the assigned action in the preview.
* In the **File** menu, **Export configuration…** saves both layouts as JSON,
  including changes that are still open in the editor. **Import configuration…**
  loads a JSON file through the Android file picker; the import can be undone
  and does not change the profile of other games. Invalid files display an error
  without replacing the current configuration.
* Press **Save** to apply the profile to the game or to the global controls.

### Ready-made configurations

In the editor, tap **Preset** (or **File → Ready-made configurations**), search
for a game, select it to view instructions and a preview, then press
**Use configuration**.

The selection replaces both orientations as a single undoable change;
press **Save** to apply it. Each game receives an independent copy that can be
edited and exported using the normal JSON format.

Included presets:

* **The Lords of Midnight · Classic DOS**: Chris Wild's conversion,
  `MIDNIGHT.COM`, with numeric compass, Move, Look, Think, Choose, Night, and heroes.
* **Lords of Midnight: The Citadel**: 3D DOS edition, with mouse clicks,
  map, F1–F8 sections, and time control.
* **DOOM / DOOM II**, **Wolfenstein 3D**, **Prince of Persia · DOS**,
  **Commander Keen 4**, and **Universal base**.

In classic Lords of Midnight, time advances in turns: **Night (U)** ends
the day. In The Citadel, **Time (M)** pauses or resumes the clock.
The presets assume the games' original controls; you can adapt them if you have remapped them.
The sources for the key assignments are collected in
[docs/CONTROL-PRESETS.md](docs/CONTROL-PRESETS.md).

Symbol names refer to the physical keys of the US DOS keyboard layout;
the character produced depends on the layout configured in DOS. Arrow keys and
numeric keypad keys have distinct assignments, including in exported files.

## Reproducible build

The Docker image pins Java 17, Android platform/NDK, CMake, and Gradle. To
build and sign debug APKs with a persistent key, run:

```sh
bash scripts/build-apk.sh
```

The script creates the Docker volume `dosbox-plus-signing` and mounts `/root/.android`
during signing. Android generates `debug.keystore` on first use and reuses
the same key for subsequent builds: keep the volume even when rebuilding the image.
The APKs are copied to `dist/`.

If you already have the `debug.keystore` used for an installed APK, copy it into
the volume **before the first build**; otherwise, the new key will prevent you
from updating that installation. For example, from the project directory:

```sh
docker volume create dosbox-plus-signing
docker run --rm --mount type=volume,source=dosbox-plus-signing,target=/root/.android \
  --mount "type=bind,source=$(realpath debug.keystore),target=/tmp/previous.keystore,readonly" \
  alpine sh -c 'cp /tmp/previous.keystore /root/.android/debug.keystore && chmod 600 /root/.android/debug.keystore'
```

To keep a complete log and make Kotlin diagnostics visible:

```sh
bash scripts/build-apk.sh 2>&1 | tee build.log
```

The build downloads a pinned `dosbox-libretro` source archive and verifies
its checksum. JVM tests are run while creating the image;
to run them again after the build:

```sh
docker build --target build -t dosbox-plus-build .
docker run --rm dosbox-plus-build gradle --no-daemon test
```

To verify restarts, video, and keyboard behavior on the real core, on Linux with GCC,
Make, and binutils, and after downloading the upstream source:

```sh
bash scripts/test-core-session.sh
```

The script also accepts the path to a local copy of the core as its first argument.
It uses a DOS test program included in the source tree, so no games are required.

## Source and licensing

See [docs/UPSTREAM.md](docs/UPSTREAM.md). The JNI frontend hosts the libretro
core directly and supplies software video, stereo audio, keyboard, and mouse
callbacks. UI profiles and SAF staging remain separate from the core.
