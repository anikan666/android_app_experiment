package com.sarangi.audio.capture

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AudioCaptureService : Service() {

    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val NOTIFICATION_CHANNEL_ID = "sarangi_audio"
        const val NOTIFICATION_ID = 1001

        private val _audioFrames = MutableSharedFlow<ShortArray>(extraBufferCapacity = 10)
        val audioFrames: SharedFlow<ShortArray> = _audioFrames.asSharedFlow()

        private val _isRecording = MutableSharedFlow<Boolean>(replay = 1)
        val isRecording: SharedFlow<Boolean> = _isRecording.asSharedFlow()
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startRecording()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopRecording()
        scope.cancel()
        super.onDestroy()
    }

    private fun startRecording() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            .coerceAtLeast(4096)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            stopSelf()
            return
        }

        audioRecord?.startRecording()

        recordingJob = scope.launch {
            _isRecording.emit(true)
            val buffer = ShortArray(2048)
            while (isActive) {
                val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (readCount > 0) {
                    _audioFrames.emit(buffer.copyOf(readCount))
                }
            }
        }
    }

    private fun stopRecording() {
        recordingJob?.cancel()
        scope.launch { _isRecording.emit(false) }
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Sarangi Audio",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Sarangi is listening during your practice session"
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Sarangi")
            .setContentText("Listening during practice...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
