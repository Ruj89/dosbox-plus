#pragma once

#include <dlfcn.h>
#include <stdexcept>
#include <string>
#include "libretro.h"

// The pinned DOSBox core is not reentrant: retro_deinit leaves process globals,
// including libco's mainThread, alive. Load a fresh library for each session.
// This host must NOT link against libretro.so (which would keep it loaded).
class LibretroCore {
public:
    explicit LibretroCore(const char *path = "libretro.so") {
        handle = dlopen(path, RTLD_NOW | RTLD_LOCAL);
        if (!handle) throw std::runtime_error(std::string("Cannot load DOSBox core: ") + dlerror());
        try {
            bind(set_environment, "retro_set_environment");
            bind(set_video_refresh, "retro_set_video_refresh");
            bind(set_audio_sample, "retro_set_audio_sample");
            bind(set_audio_sample_batch, "retro_set_audio_sample_batch");
            bind(set_input_poll, "retro_set_input_poll");
            bind(set_input_state, "retro_set_input_state");
            bind(set_controller_port_device, "retro_set_controller_port_device");
            bind(init, "retro_init");
            bind(deinit, "retro_deinit");
            bind(load_game, "retro_load_game");
            bind(unload_game, "retro_unload_game");
            bind(run, "retro_run");
        } catch (...) {
            dlclose(handle);
            throw;
        }
    }

    ~LibretroCore() { dlclose(handle); }
    LibretroCore(const LibretroCore &) = delete;
    LibretroCore &operator=(const LibretroCore &) = delete;

    decltype(&retro_set_environment) set_environment = nullptr;
    decltype(&retro_set_video_refresh) set_video_refresh = nullptr;
    decltype(&retro_set_audio_sample) set_audio_sample = nullptr;
    decltype(&retro_set_audio_sample_batch) set_audio_sample_batch = nullptr;
    decltype(&retro_set_input_poll) set_input_poll = nullptr;
    decltype(&retro_set_input_state) set_input_state = nullptr;
    decltype(&retro_set_controller_port_device) set_controller_port_device = nullptr;
    decltype(&retro_init) init = nullptr;
    decltype(&retro_deinit) deinit = nullptr;
    decltype(&retro_load_game) load_game = nullptr;
    decltype(&retro_unload_game) unload_game = nullptr;
    decltype(&retro_run) run = nullptr;

private:
    void *handle = nullptr;

    template <typename T> void bind(T &target, const char *name) {
        dlerror();
        target = reinterpret_cast<T>(dlsym(handle, name));
        if (const char *error = dlerror()) throw std::runtime_error(std::string(name) + ": " + error);
        if (!target) throw std::runtime_error(std::string("Missing core function: ") + name);
    }
};
