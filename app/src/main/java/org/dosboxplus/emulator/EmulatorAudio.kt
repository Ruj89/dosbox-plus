package org.dosboxplus.emulator

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicBoolean

class EmulatorAudio {
    private val active = AtomicBoolean(false)
    private var worker: Thread? = null

    fun start() {
        if (!active.compareAndSet(false, true)) return
        worker = Thread({ runAudio() }, "dosbox-audio").also { it.start() }
    }

    fun stop() {
        active.set(false)
        worker?.join(1_000)
        worker = null
    }

    private fun runAudio() {
        val rate = 44_100
        val minimum = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setBufferSizeInBytes(maxOf(minimum, rate / 5 * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        val samples = ShortArray(2_048)
        try {
            track.play()
            while (active.get()) {
                val count = DosboxNative.readAudio(samples)
                if (count > 0) track.write(samples, 0, count, AudioTrack.WRITE_BLOCKING)
                else Thread.sleep(4)
            }
        } finally {
            track.pause()
            track.flush()
            track.release()
        }
    }
}
