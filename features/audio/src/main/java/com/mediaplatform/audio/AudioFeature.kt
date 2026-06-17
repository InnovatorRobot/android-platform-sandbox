package com.mediaplatform.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.mediaplatform.services.PlatformService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Captures mono 16-bit PCM from the microphone and delivers raw sample
 * buffers via [onAudioFrame]. Runs capture on a dedicated daemon thread.
 *
 * Permissions: RECORD_AUDIO must be granted before calling [start].
 */
class AudioFeature : PlatformService {

    /** Called on the audio capture thread for each buffer of PCM samples. */
    var onAudioFrame: ((ShortArray) -> Unit)? = null

    private val running     = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var thread:      Thread?      = null

    override fun start() {
        if (running.get()) return

        val sampleRate = 44100
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        // Use at least 4096 shorts (~93 ms at 44.1 kHz — good FFT window size)
        val bufferFrames = maxOf(minBuf * 2, 4096 * 2 /* bytes */)

        @Suppress("MissingPermission")
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferFrames
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return
        }

        audioRecord = record
        running.set(true)
        record.startRecording()

        thread = Thread {
            val buffer = ShortArray(bufferFrames / 2)
            while (running.get()) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    onAudioFrame?.invoke(buffer.copyOf(read))
                }
            }
        }.also {
            it.isDaemon = true
            it.name     = "AudioCapture"
            it.start()
        }
    }

    override fun stop() {
        running.set(false)
        thread?.join(500)
        thread = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    override fun dispose() = stop()
}
