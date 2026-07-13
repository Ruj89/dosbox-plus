# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:17-jdk-jammy AS build
ARG ANDROID_SDK_VERSION=11076708
ARG ANDROID_PLATFORM=35
ARG ANDROID_NDK=27.2.12479018
ARG GRADLE_VERSION=8.10.2
ENV ANDROID_HOME=/opt/android-sdk ANDROID_SDK_ROOT=/opt/android-sdk PATH=/opt/gradle/bin:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:$PATH
RUN apt-get update && apt-get install -y --no-install-recommends curl unzip ca-certificates bash && rm -rf /var/lib/apt/lists/* \
 && mkdir -p $ANDROID_HOME/cmdline-tools /opt/gradle \
 && curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_VERSION}_latest.zip" -o /tmp/sdk.zip \
 && unzip -q /tmp/sdk.zip -d $ANDROID_HOME/cmdline-tools && mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest \
 && curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/gradle.zip \
 && unzip -q /tmp/gradle.zip -d /opt && mv /opt/gradle-${GRADLE_VERSION}/* /opt/gradle/ \
 && yes | sdkmanager --licenses >/dev/null \
 && sdkmanager "platform-tools" "platforms;android-${ANDROID_PLATFORM}" "build-tools;35.0.0" "cmake;3.22.1" "ndk;${ANDROID_NDK}"
RUN apt-get update && apt-get install -y --no-install-recommends patch && rm -rf /var/lib/apt/lists/*
RUN sdkmanager "build-tools;34.0.0"
WORKDIR /src
COPY scripts/fetch-libretro.sh scripts/fetch-libretro.sh
COPY patches/dosbox-libretro-android.patch patches/dosbox-libretro-android.patch
RUN ./scripts/fetch-libretro.sh
COPY . .
RUN --mount=type=cache,target=/root/.gradle gradle --no-daemon --console=plain test assembleDebug

FROM scratch AS artifacts
COPY --from=build /src/app/build/outputs/apk/debug/ /
