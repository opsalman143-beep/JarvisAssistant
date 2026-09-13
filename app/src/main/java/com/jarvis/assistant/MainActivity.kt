package com.jarvis.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var statusText: TextView
    private lateinit var responseText: TextView
    private val client = OkHttpClient()

    private val CLAUDE_API_KEY = "YOUR_API_KEY_HERE"

    companion object {
        const val SPEECH_REQUEST_CODE = 100
        const val MIC_PERMISSION_CODE = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        responseText = findViewById(R.id.responseText)
        val micButton: Button = findViewById(R.id.micButton)

        tts = TextToSpeech(this, this)

        micButton.setOnClickListener {
            checkPermissionAndListen()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    private fun checkPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO), MIC_PERMISSION_CODE
            )
        } else {
            startListening()
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Cheppu, Jarvis vintunnadu...")
        }
        try {
            statusText.text = "LISTENING..."
            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SPEECH_REQUEST_CODE && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: return
            responseText.text = "Nuvvu: $spokenText"
            statusText.text = "THINKING..."
            askClaude(spokenText)
        }
    }

    private fun askClaude(userMessage: String) {
        val json = JSONObject().apply {
            put("model", "claude-sonnet-4-6")
            put("max_tokens", 300)
            put("messages", JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                }
            ))
        }

        val body = RequestBody.create(
            MediaType.parse("application/json"), json.toString()
        )

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", CLAUDE_API_KEY)
            .addHeader("anthropic-version",
