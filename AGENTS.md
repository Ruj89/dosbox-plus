```markdown
# DOSBox Plus

## Repository Overview

DOSBox Plus is an Android 8+ frontend for the GPL `dosbox-libretro` core, featuring an importable DOS game library and touch-input profiles. Games are never bundled. The built-in HUD editor provides separate portrait/landscape canvases, visual drag/resize controls, key assignment, opacity, hold behavior, layering, and undo/redo. Profiles can be global or assigned independently to a game.

## Reproducible Build

The Docker image pins Java 17, Android platform/NDK, CMake, and Gradle. To build the debug APK into `dist/`:

```sh
bash scripts/build-apk.sh
```

To build the image and run JVM tests with detailed logs:

```sh
docker build --progress=plain --target build -t dosbox-plus-build . 2>&1 | tee build.log
```

The build downloads a pinned `dosbox-libretro` source archive and verifies its checksum. To rerun JVM tests after building:

```sh
docker build --target build -t dosbox-plus-build .
docker run --rm dosbox-plus-build gradle --no-daemon test
```

## Source and Licensing

See [docs/UPSTREAM.md](docs/UPSTREAM.md). The JNI frontend hosts the libretro core directly and supplies software video, stereo audio, keyboard, and mouse callbacks. UI profiles and SAF staging remain separate from the core.

## Recent Commits

No remote commits found. Ensure a remote is configured with `git remote add origin <repository-url>` and fetch changes with `git fetch origin`.
```
