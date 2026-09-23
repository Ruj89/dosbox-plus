#include "libretro_core.h"
#include <chrono>
#include <cstdarg>
#include <cstdio>
#include <set>
#include <stdexcept>
#include <thread>

namespace {
const char *directory;
bool shutdown_requested;
retro_keyboard_event_t keyboard;
unsigned iteration, frames;
unsigned key_events;
bool exit_with_key;
std::set<uint32_t> colors;

void log_message(retro_log_level level, const char *format, ...) {
    if (level < RETRO_LOG_WARN) return;
    va_list args;
    va_start(args, format);
    vfprintf(stderr, format, args);
    va_end(args);
}

bool environment(unsigned command, void *data) {
    switch (command) {
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
        case RETRO_ENVIRONMENT_GET_CONTENT_DIRECTORY:
            *static_cast<const char **>(data) = directory; return true;
        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE:
            static_cast<retro_log_callback *>(data)->log = log_message; return true;
        case RETRO_ENVIRONMENT_SET_KEYBOARD_CALLBACK:
            keyboard = static_cast<retro_keyboard_callback *>(data)->callback; return true;
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
            return *static_cast<retro_pixel_format *>(data) == RETRO_PIXEL_FORMAT_XRGB8888;
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
            *static_cast<bool *>(data) = false; return true;
        case RETRO_ENVIRONMENT_GET_VARIABLE:
            static_cast<retro_variable *>(data)->value = nullptr; return false;
        case RETRO_ENVIRONMENT_SHUTDOWN:
            shutdown_requested = true; return true;
        case RETRO_ENVIRONMENT_SET_GEOMETRY:
        case RETRO_ENVIRONMENT_SET_VARIABLES:
        case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS:
        case RETRO_ENVIRONMENT_SET_CONTROLLER_INFO:
        case RETRO_ENVIRONMENT_SET_SUPPORT_NO_GAME:
            return true;
        default: return false;
    }
}

void video(const void *data, unsigned width, unsigned height, size_t) {
    if (!data || !width || !height) return;
    ++frames;
    // The synthetic DOS program fills mode 13h with a color from the BIOS clock.
    if (width == 320 && height == 200) colors.insert(*static_cast<const uint32_t *>(data));
}
size_t audio(const int16_t *, size_t count) { return count; }
int16_t input(unsigned, unsigned, unsigned, unsigned) { return 0; }
void poll() {
    if (exit_with_key && keyboard && (iteration == 120 || iteration == 122)) {
        ++key_events;
        keyboard(iteration == 120, RETROK_ESCAPE, 0, 0);
    }
}

void session(const char *library, const char *config, bool natural_exit) {
    shutdown_requested = false;
    keyboard = nullptr;
    frames = 0;
    key_events = 0;
    colors.clear();
    exit_with_key = natural_exit;
    LibretroCore core(library);
    core.set_environment(environment);
    core.set_video_refresh(video);
    core.set_audio_sample(nullptr);
    core.set_audio_sample_batch(audio);
    core.set_input_poll(poll);
    core.set_input_state(input);
    core.init();
    retro_game_info game{};
    game.path = config;
    if (!core.load_game(&game)) throw std::runtime_error("Second/subsequent load rejected");
    core.set_controller_port_device(0, RETRO_DEVICE_KEYBOARD);
    if (!keyboard) throw std::runtime_error("Keyboard callback was not installed");
    for (iteration = 0; iteration < (natural_exit ? 240u : 120u) && !shutdown_requested; ++iteration) {
        core.run();
        std::this_thread::sleep_for(std::chrono::milliseconds(16));
    }
    core.unload_game();
    core.deinit();
    keyboard = nullptr;
    if (frames < 30 || colors.size() < 8) throw std::runtime_error("Video/BIOS clock is frozen");
    if (natural_exit && !shutdown_requested) {
        fprintf(stderr, "frames=%u colors=%zu keys=%u iterations=%u\n", frames, colors.size(), key_events, iteration);
        throw std::runtime_error("Keyboard did not reach the running DOS program");
    }
    printf("Session: %u frames, %zu changing colors, exit=%s\n", frames, colors.size(), natural_exit ? "keyboard" : "frontend");
}
}

int main(int argc, char **argv) {
    if (argc != 4) return 2;
    directory = argv[3];
    try {
        // Real JNI sessions also use a new worker thread for every launch.
        for (int index = 0; index < 4; ++index) {
            std::exception_ptr error;
            std::thread worker([&] { try { session(argv[1], argv[2], index % 2 != 0); } catch (...) { error = std::current_exception(); } });
            worker.join();
            if (error) std::rethrow_exception(error);
        }
    } catch (const std::exception &error) {
        fprintf(stderr, "FAIL: %s\n", error.what());
        return 1;
    }
    puts("PASS: repeated launch, moving video, keyboard and both shutdown paths");
}
