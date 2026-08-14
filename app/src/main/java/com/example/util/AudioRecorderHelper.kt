package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import java.io.File

class AudioRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    var currentOutputFile: File? = null

    val lastRecordingPath: String?
        get() = currentOutputFile?.absolutePath

    fun startRecording() {
        val fileName = "speech_record_last.m4a"
        currentOutputFile = File(context.cacheDir, fileName)

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(16000) // 16kHz is standard for speech recognition
            setAudioEncodingBitRate(32000) // 32kbps for smaller file size without losing speech clarity
            setOutputFile(currentOutputFile?.absolutePath)
            
            try {
                prepare()
                start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopRecording(): String? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            
            // Convert to Base64
            currentOutputFile?.let { file ->
                if (file.exists()) {
                    val bytes = file.readBytes()
                    // Cleanup removed to allow playback
                    Base64.encodeToString(bytes, Base64.NO_WRAP)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
