package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class VoiceManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    // --- RECORDING STATE ---
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var recordingStartTime = 0L
    private var amplitudeJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

    private val _recordingAmplitude = MutableStateFlow(0f)
    val recordingAmplitude: StateFlow<Float> = _recordingAmplitude.asStateFlow()

    // --- PLAYBACK STATE ---
    private var mediaPlayer: MediaPlayer? = null
    private var playbackProgressJob: Job? = null

    private val _playingMessageId = MutableStateFlow<String?>(null)
    val playingMessageId: StateFlow<String?> = _playingMessageId.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f) // 0f to 1f
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _playbackCurrentSeconds = MutableStateFlow(0)
    val playbackCurrentSeconds: StateFlow<Int> = _playbackCurrentSeconds.asStateFlow()

    fun startRecording(): Boolean {
        return try {
            stopPlayback()

            val audioDir = File(context.cacheDir, "voice_notes")
            if (!audioDir.exists()) audioDir.mkdirs()

            val file = File(audioDir, "voice_${System.currentTimeMillis()}.m4a")
            currentRecordingFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            recordingStartTime = System.currentTimeMillis()
            _isRecording.value = true
            _recordingDurationSeconds.value = 0

            // Amplitude and timer job
            amplitudeJob = scope.launch {
                while (isActive && _isRecording.value) {
                    val elapsed = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
                    _recordingDurationSeconds.value = elapsed
                    try {
                        val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                        // Normalize 0..32767 to 0..1
                        _recordingAmplitude.value = (maxAmp / 32767f).coerceIn(0.05f, 1f)
                    } catch (_: Exception) {
                        _recordingAmplitude.value = 0.2f
                    }
                    delay(100)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error starting recording: ${e.message}")
            cancelRecording()
            false
        }
    }

    data class RecordingResult(val fileUri: String, val durationSec: Int)

    fun stopRecording(): RecordingResult? {
        if (!_isRecording.value) return null
        amplitudeJob?.cancel()
        amplitudeJob = null
        _isRecording.value = false

        val durationSec = kotlin.math.max(1, _recordingDurationSeconds.value)
        val file = currentRecordingFile

        return try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.w("VoiceManager", "Error stopping recorder: ${e.message}")
                }
                release()
            }
            mediaRecorder = null

            if (file != null && file.exists() && file.length() > 0) {
                RecordingResult(Uri.fromFile(file).toString(), durationSec)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error finalizing recording: ${e.message}")
            file?.delete()
            null
        }
    }

    fun cancelRecording() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        _isRecording.value = false
        _recordingDurationSeconds.value = 0
        try {
            mediaRecorder?.apply {
                try { stop() } catch (_: Exception) {}
                release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null
        currentRecordingFile?.delete()
        currentRecordingFile = null
    }

    // --- PLAYBACK FUNCTIONS ---

    fun togglePlayVoice(messageId: String, audioUriString: String) {
        if (_playingMessageId.value == messageId) {
            // Already active; toggle pause/resume or stop
            if (mediaPlayer?.isPlaying == true) {
                pausePlayback()
            } else {
                resumePlayback()
            }
            return
        }

        startPlayback(messageId, audioUriString)
    }

    private fun startPlayback(messageId: String, audioUriString: String) {
        stopPlayback()
        try {
            val player = MediaPlayer()
            val uri = Uri.parse(audioUriString)
            if (audioUriString.startsWith("content://") || audioUriString.startsWith("file://")) {
                player.setDataSource(context, uri)
            } else {
                player.setDataSource(audioUriString)
            }

            player.prepare()
            player.setOnCompletionListener {
                stopPlayback()
            }
            player.start()

            mediaPlayer = player
            _playingMessageId.value = messageId
            _playbackProgress.value = 0f

            val totalDurationMs = player.duration.coerceAtLeast(1)

            playbackProgressJob = scope.launch {
                while (isActive && mediaPlayer != null && _playingMessageId.value == messageId) {
                    try {
                        val currentMs = mediaPlayer?.currentPosition ?: 0
                        _playbackProgress.value = (currentMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
                        _playbackCurrentSeconds.value = currentMs / 1000
                    } catch (_: Exception) {}
                    delay(100)
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error playing audio: ${e.message}")
            stopPlayback()
        }
    }

    fun pausePlayback() {
        try {
            mediaPlayer?.pause()
        } catch (_: Exception) {}
    }

    fun resumePlayback() {
        try {
            mediaPlayer?.start()
        } catch (_: Exception) {}
    }

    fun stopPlayback() {
        playbackProgressJob?.cancel()
        playbackProgressJob = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        _playingMessageId.value = null
        _playbackProgress.value = 0f
        _playbackCurrentSeconds.value = 0
    }

    fun release() {
        cancelRecording()
        stopPlayback()
    }
}
