package org.dosboxplus.emulator

object DosboxNative {
    init { System.loadLibrary("dosboxplus") }
    external fun start(configPath: String, systemPath: String, savePath: String): Boolean
    external fun stop()
    external fun sendKey(scanCode: Int, down: Boolean)
    external fun sendMouseButton(button: Int, down: Boolean)
    external fun sendMouseMotion(dx: Int, dy: Int)
    external fun frameInfo(): Long
    external fun copyFrame(pixels: IntArray): Boolean
    external fun readAudio(samples: ShortArray): Int
}
