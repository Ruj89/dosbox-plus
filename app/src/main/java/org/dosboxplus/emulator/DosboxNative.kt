package org.dosboxplus.emulator

object DosboxNative {
    init { System.loadLibrary("dosboxplus") }
    external fun start(configPath: String, systemPath: String, savePath: String): Boolean
    external fun stop()
    external fun state(): Int
    const val STATE_IDLE = 0
    const val STATE_STARTING = 1
    const val STATE_RUNNING = 2
    const val STATE_STOPPED = 3
    const val STATE_FAILED = 4
    external fun sendKey(scanCode: Int, down: Boolean)
    external fun sendMouseButton(button: Int, down: Boolean)
    external fun sendMouseMotion(dx: Int, dy: Int)
    external fun frameInfo(): Long
    external fun copyFrame(pixels: IntArray, width: Int, height: Int): Boolean
    external fun readAudio(samples: ShortArray): Int
}
