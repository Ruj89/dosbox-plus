# DOSBox Plus

Android 8+ frontend for the GPL `dosbox-libretro` core, with an importable DOS game
library and touch-input profiles. Games are never bundled.

The built-in HUD editor provides separate portrait/landscape canvases, visual
drag/resize controls, key assignment, opacity, hold behavior, layering and
undo/redo. Profiles can be global or assigned independently to a game.

## Reproducible build

The Docker image pins Java 17, Android platform/NDK, CMake and Gradle. Run:

```sh
docker build --target artifacts --output type=local,dest=dist .
```

Per conservare un log completo e rendere visibili le diagnostiche Kotlin:

```sh
docker build --progress=plain --target artifacts --output type=local,dest=dist . 2>&1 | tee build.log
```

The debug APK is written to `dist/`. The build downloads a pinned
`dosbox-libretro` source archive and verifies its checksum. To run only
the faster JVM tests after the image has been built:

```sh
docker build --target build -t dosbox-plus-build .
docker run --rm dosbox-plus-build gradle --no-daemon test
```

## Source and licensing

See [docs/UPSTREAM.md](docs/UPSTREAM.md). The JNI frontend hosts the libretro
core directly and supplies software video, stereo audio, keyboard and mouse
callbacks. UI profiles and SAF staging remain separate from the core.
