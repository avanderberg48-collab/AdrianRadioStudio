package com.adrianradio.studio

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var isRecording = false
    private var outputFile: String = ""

    private lateinit var btnMic: Button
    private lateinit var btnPlayMusic: Button
    private lateinit var btnStopMusic: Button
    private lateinit var seekMusicVolume: SeekBar
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tts = TextToSpeech(this, this)
        outputFile = "${getExternalFilesDir(null)}/broadcast.mp4"

        btnMic = findViewById(R.id.btnMic)
        btnPlayMusic = findViewById(R.id.btnPlayMusic)
        btnStopMusic = findViewById(R.id.btnStopMusic)
        seekMusicVolume = findViewById(R.id.seekMusicVolume)
        tvStatus = findViewById(R.id.tvStatus)

        checkPermissions()
        setupListeners()
    }

    private fun setupListeners() {
        btnMic.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        btnPlayMusic.setOnClickListener {
            playMusic()
        }

        btnStopMusic.setOnClickListener {
            stopMusic()
        }

        seekMusicVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val vol = progress / 100f
                mediaPlayer?.setVolume(vol, vol)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                speak("Music volume set to ${seekBar.progress} percent")
            }
        })
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            speak("Please allow microphone permission.")
            return
        }
        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile)
                prepare()
                start()
            }
            isRecording = true
            btnMic.text = "STOP BROADCAST"
            btnMic.contentDescription = "Stop broadcast button"
            tvStatus.text = "LIVE - Broadcasting now!"
            speak("You are now live on air!")
        } catch (e: Exception) {
            tvStatus.text = "Error: ${e.message}"
            speak("Error starting broadcast.")
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply { stop(); release() }
        mediaRecorder = null
        isRecording = false
        btnMic.text = "GO LIVE"
        btnMic.contentDescription = "Go live button"
        tvStatus.text = "Broadcast stopped. Recording saved."
        speak("Broadcast stopped. Your recording has been saved.")
    }

    private fun playMusic() {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource("https://stream.zeno.fm/0r0xa792kwzuv")
                    setOnPreparedListener { start(); tvStatus.text = "Music playing"; speak("Music is now playing.") }
                    setOnErrorListener { _, _, _ -> speak("Could not load music stream."); true }
                    prepareAsync()
                }
            } else {
                mediaPlayer?.start()
                speak("Music resumed.")
            }
        } catch (e: Exception) {
            speak("Music error.")
        }
    }

    private fun stopMusic() {
        mediaPlayer?.pause()
        tvStatus.text = "Music paused."
        speak("Music paused.")
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.ENGLISH
            speak("Welcome to Adrian Radio Studio. Tap Go Live to start broadcasting.")
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET)
        val needed = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) ActivityCompat.requestPermissions(this, needed.toTypedArray(), 1001)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        mediaPlayer?.release()
        tts?.shutdown()
    }
}
