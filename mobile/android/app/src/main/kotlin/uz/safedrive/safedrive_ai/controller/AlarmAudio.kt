package uz.safedrive.safedrive_ai.controller

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator

/** Owns alarm audio and restores volumes after the alert ends. */
internal class AlarmAudio(context: Context) {
    private val manager = context.getSystemService(AudioManager::class.java)
    private val speakerTone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
    private var bluetoothTone: ToneGenerator? = null
    private val savedVolumes = mutableMapOf<Int, Int>()
    private var mediaOutputs = emptyList<Int>()
    private var started = false
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
        .setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .setOnAudioFocusChangeListener { change ->
            if (change == AudioManager.AUDIOFOCUS_LOSS) {
                bluetoothTone?.stopTone()
            }
        }.build()

    private fun outputIds(): List<Int> =
        manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { it.id }.sorted()

    private fun raiseVolume(stream: Int) {
        if (manager.isVolumeFixed || savedVolumes.containsKey(stream)) return
        val previous = manager.getStreamVolume(stream)
        try {
            manager.setStreamVolume(stream, manager.getStreamMaxVolume(stream), 0)
            savedVolumes[stream] = previous
        } catch (_: SecurityException) {
            // Android may require notification-policy access under Do Not Disturb.
        }
    }

    fun pulse() {
        if (!started) {
            started = true
            mediaOutputs = outputIds()
            val focusGranted = manager.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            raiseVolume(AudioManager.STREAM_ALARM)
            val bluetooth = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
            }
            if (bluetooth && focusGranted) {
                raiseVolume(AudioManager.STREAM_MUSIC)
                bluetoothTone = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            }
        }
        // Reassert the requested loud level if the user lowered it during an active alarm.
        for (stream in savedVolumes.keys) {
            if (stream != AudioManager.STREAM_MUSIC || outputIds() == mediaOutputs) {
                runCatching { manager.setStreamVolume(stream, manager.getStreamMaxVolume(stream), 0) }
            }
        }
        speakerTone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
        if (outputIds() == mediaOutputs) {
            bluetoothTone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
        } else {
            bluetoothTone?.stopTone()
        }
    }

    fun stop() {
        speakerTone.stopTone()
        bluetoothTone?.release()
        bluetoothTone = null
        for ((stream, volume) in savedVolumes) {
            if (stream != AudioManager.STREAM_MUSIC || outputIds() == mediaOutputs) {
                runCatching { manager.setStreamVolume(stream, volume, 0) }
            }
        }
        savedVolumes.clear()
        if (started) manager.abandonAudioFocusRequest(focus)
        started = false
    }

    fun close() {
        stop()
        speakerTone.release()
    }
}
