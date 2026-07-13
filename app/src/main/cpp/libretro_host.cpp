#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <atomic>
#include <chrono>
#include <cstdarg>
#include <cstdint>
#include <deque>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

#include "libretro.h"

namespace {
std::atomic<bool> running{false};
std::atomic<bool> shutdown_requested{false};
std::thread core_thread;
std::string system_dir;
std::string save_dir;
std::string content_dir;
retro_pixel_format pixel_format = RETRO_PIXEL_FORMAT_XRGB8888;
retro_keyboard_event_t keyboard_callback = nullptr;

std::mutex frame_mutex;
std::vector<jint> frame;
unsigned frame_width = 0;
unsigned frame_height = 0;
uint64_t frame_generation = 0;

std::mutex audio_mutex;
std::deque<int16_t> audio_samples;

struct KeyEvent { unsigned key; bool down; };
std::mutex input_mutex;
std::deque<KeyEvent> key_events;
std::atomic<int16_t> mouse_dx{0};
std::atomic<int16_t> mouse_dy{0};
std::atomic<bool> mouse_buttons[3];

void log_message(enum retro_log_level level, const char *format, ...) {
    int priority = level == RETRO_LOG_ERROR ? ANDROID_LOG_ERROR :
                   level == RETRO_LOG_WARN ? ANDROID_LOG_WARN : ANDROID_LOG_INFO;
    va_list args;
    va_start(args, format);
    __android_log_vprint(priority, "DOSBoxLibretro", format, args);
    va_end(args);
}

bool environment(unsigned command, void *data) {
    switch (command) {
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
            *static_cast<const char **>(data) = system_dir.c_str(); return true;
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
            *static_cast<const char **>(data) = save_dir.c_str(); return true;
        case RETRO_ENVIRONMENT_GET_CONTENT_DIRECTORY:
            *static_cast<const char **>(data) = content_dir.c_str(); return true;
        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE:
            static_cast<retro_log_callback *>(data)->log = log_message; return true;
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
            pixel_format = *static_cast<retro_pixel_format *>(data); return true;
        case RETRO_ENVIRONMENT_SET_KEYBOARD_CALLBACK:
            keyboard_callback = static_cast<retro_keyboard_callback *>(data)->callback; return true;
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
        default:
            return false;
    }
}

void video_refresh(const void *data, unsigned width, unsigned height, size_t pitch) {
    if (!data || !width || !height) return;
    std::lock_guard<std::mutex> lock(frame_mutex);
    frame.resize(static_cast<size_t>(width) * height);
    if (pixel_format == RETRO_PIXEL_FORMAT_RGB565) {
        for (unsigned y = 0; y < height; ++y) {
            auto row = reinterpret_cast<const uint16_t *>(static_cast<const uint8_t *>(data) + y * pitch);
            for (unsigned x = 0; x < width; ++x) {
                uint16_t value = row[x];
                uint32_t r = ((value >> 11) & 0x1f) * 255 / 31;
                uint32_t g = ((value >> 5) & 0x3f) * 255 / 63;
                uint32_t b = (value & 0x1f) * 255 / 31;
                frame[y * width + x] = static_cast<jint>(0xff000000u | r << 16 | g << 8 | b);
            }
        }
    } else {
        for (unsigned y = 0; y < height; ++y) {
            auto row = reinterpret_cast<const uint32_t *>(static_cast<const uint8_t *>(data) + y * pitch);
            for (unsigned x = 0; x < width; ++x)
                frame[y * width + x] = static_cast<jint>(0xff000000u | (row[x] & 0x00ffffffu));
        }
    }
    frame_width = width;
    frame_height = height;
    ++frame_generation;
}

size_t audio_batch(const int16_t *data, size_t frames) {
    std::lock_guard<std::mutex> lock(audio_mutex);
    const size_t count = frames * 2;
    constexpr size_t max_samples = 49716 * 4;
    while (audio_samples.size() + count > max_samples && !audio_samples.empty()) audio_samples.pop_front();
    audio_samples.insert(audio_samples.end(), data, data + count);
    return frames;
}

void input_poll() {
    std::deque<KeyEvent> pending;
    { std::lock_guard<std::mutex> lock(input_mutex); pending.swap(key_events); }
    if (keyboard_callback) for (const auto &event : pending) keyboard_callback(event.down, event.key, 0, 0);
}

int16_t input_state(unsigned, unsigned device, unsigned, unsigned id) {
    if (device != RETRO_DEVICE_MOUSE) return 0;
    if (id == RETRO_DEVICE_ID_MOUSE_X) return mouse_dx.exchange(0);
    if (id == RETRO_DEVICE_ID_MOUSE_Y) return mouse_dy.exchange(0);
    if (id == RETRO_DEVICE_ID_MOUSE_LEFT) return mouse_buttons[0] ? 1 : 0;
    if (id == RETRO_DEVICE_ID_MOUSE_RIGHT) return mouse_buttons[1] ? 1 : 0;
    if (id == RETRO_DEVICE_ID_MOUSE_MIDDLE) return mouse_buttons[2] ? 1 : 0;
    return 0;
}

unsigned scan_code_to_retro(int code) {
    static const unsigned table[128] = {
        0, RETROK_ESCAPE, RETROK_1, RETROK_2, RETROK_3, RETROK_4, RETROK_5, RETROK_6,
        RETROK_7, RETROK_8, RETROK_9, RETROK_0, RETROK_MINUS, RETROK_EQUALS, RETROK_BACKSPACE, RETROK_TAB,
        RETROK_q, RETROK_w, RETROK_e, RETROK_r, RETROK_t, RETROK_y, RETROK_u, RETROK_i,
        RETROK_o, RETROK_p, RETROK_LEFTBRACKET, RETROK_RIGHTBRACKET, RETROK_RETURN, RETROK_LCTRL, RETROK_a, RETROK_s,
        RETROK_d, RETROK_f, RETROK_g, RETROK_h, RETROK_j, RETROK_k, RETROK_l, RETROK_SEMICOLON,
        RETROK_QUOTE, RETROK_BACKQUOTE, RETROK_LSHIFT, RETROK_BACKSLASH, RETROK_z, RETROK_x, RETROK_c, RETROK_v,
        RETROK_b, RETROK_n, RETROK_m, RETROK_COMMA, RETROK_PERIOD, RETROK_SLASH, RETROK_RSHIFT, RETROK_KP_MULTIPLY,
        RETROK_LALT, RETROK_SPACE, RETROK_CAPSLOCK, RETROK_F1, RETROK_F2, RETROK_F3, RETROK_F4, RETROK_F5,
        RETROK_F6, RETROK_F7, RETROK_F8, RETROK_F9, RETROK_F10, RETROK_NUMLOCK, RETROK_SCROLLOCK, RETROK_KP7,
        RETROK_KP8, RETROK_KP9, RETROK_KP_MINUS, RETROK_KP4, RETROK_KP5, RETROK_KP6, RETROK_KP_PLUS, RETROK_KP1,
        RETROK_KP2, RETROK_KP3, RETROK_KP0, RETROK_KP_PERIOD
    };
    if (code == 72) return RETROK_UP;
    if (code == 75) return RETROK_LEFT;
    if (code == 77) return RETROK_RIGHT;
    if (code == 80) return RETROK_DOWN;
    if (code >= 0 && code < 128 && table[code]) return table[code];
    return static_cast<unsigned>(code);
}

void run_core(std::string content_path) {
    retro_set_environment(environment);
    retro_set_video_refresh(video_refresh);
    retro_set_audio_sample(nullptr);
    retro_set_audio_sample_batch(audio_batch);
    retro_set_input_poll(input_poll);
    retro_set_input_state(input_state);
    retro_init();
    retro_game_info game{};
    game.path = content_path.c_str();
    if (!retro_load_game(&game)) {
        running = false;
        retro_deinit();
        return;
    }
    running = true;
    auto next = std::chrono::steady_clock::now();
    while (running && !shutdown_requested) {
        retro_run();
        next += std::chrono::microseconds(16667);
        std::this_thread::sleep_until(next);
    }
    retro_unload_game();
    retro_deinit();
    running = false;
}

std::string java_string(JNIEnv *env, jstring value) {
    const char *chars = env->GetStringUTFChars(value, nullptr);
    std::string result(chars ? chars : "");
    if (chars) env->ReleaseStringUTFChars(value, chars);
    return result;
}
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_dosboxplus_emulator_DosboxNative_start(JNIEnv *env, jobject, jstring config, jstring system, jstring save) {
    if (core_thread.joinable()) return JNI_FALSE;
    std::string content = java_string(env, config);
    system_dir = java_string(env, system);
    save_dir = java_string(env, save);
    auto slash = content.find_last_of('/');
    content_dir = slash == std::string::npos ? system_dir : content.substr(0, slash);
    shutdown_requested = false;
    {
        std::lock_guard<std::mutex> lock(frame_mutex);
        frame.clear(); frame_width = 0; frame_height = 0; frame_generation = 0;
    }
    {
        std::lock_guard<std::mutex> lock(audio_mutex);
        audio_samples.clear();
    }
    core_thread = std::thread(run_core, content);
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_org_dosboxplus_emulator_DosboxNative_stop(JNIEnv *, jobject) {
    shutdown_requested = true;
    running = false;
    if (core_thread.joinable()) core_thread.join();
    std::lock_guard<std::mutex> lock(audio_mutex);
    audio_samples.clear();
}

extern "C" JNIEXPORT void JNICALL
Java_org_dosboxplus_emulator_DosboxNative_sendKey(JNIEnv *, jobject, jint code, jboolean down) {
    std::lock_guard<std::mutex> lock(input_mutex);
    key_events.push_back({scan_code_to_retro(code), down == JNI_TRUE});
}

extern "C" JNIEXPORT void JNICALL
Java_org_dosboxplus_emulator_DosboxNative_sendMouseButton(JNIEnv *, jobject, jint button, jboolean down) {
    if (button >= 0 && button < 3) mouse_buttons[button] = down == JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_org_dosboxplus_emulator_DosboxNative_sendMouseMotion(JNIEnv *, jobject, jint dx, jint dy) {
    mouse_dx = static_cast<int16_t>(mouse_dx.load() + dx);
    mouse_dy = static_cast<int16_t>(mouse_dy.load() + dy);
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_dosboxplus_emulator_DosboxNative_frameInfo(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(frame_mutex);
    return (static_cast<jlong>(frame_generation & 0xffff) << 48) |
           (static_cast<jlong>(frame_width & 0xffffff) << 24) | frame_height;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_dosboxplus_emulator_DosboxNative_copyFrame(JNIEnv *env, jobject, jintArray target) {
    std::lock_guard<std::mutex> lock(frame_mutex);
    if (env->GetArrayLength(target) < static_cast<jsize>(frame.size())) return JNI_FALSE;
    env->SetIntArrayRegion(target, 0, static_cast<jsize>(frame.size()), frame.data());
    return JNI_TRUE;
}

extern "C" JNIEXPORT jint JNICALL
Java_org_dosboxplus_emulator_DosboxNative_readAudio(JNIEnv *env, jobject, jshortArray target) {
    std::lock_guard<std::mutex> lock(audio_mutex);
    const size_t count = std::min(static_cast<size_t>(env->GetArrayLength(target)), audio_samples.size());
    std::vector<jshort> result(count);
    for (size_t i = 0; i < count; ++i) { result[i] = audio_samples.front(); audio_samples.pop_front(); }
    env->SetShortArrayRegion(target, 0, static_cast<jsize>(count), result.data());
    return static_cast<jint>(count);
}
