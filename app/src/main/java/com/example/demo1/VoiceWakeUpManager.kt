package com.example.demo1

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.content.Intent
import android.util.Log
import java.util.Locale

/*
* 使用 Android 的SpeechRecognizer实现语音唤醒
* */
class VoiceWakeUpManager(private val context: Context) {
    private var TAG = "VoiceWakeUpManager" // 默认唤醒词
    private var speechRecognizer: SpeechRecognizer? = null
    private var wakeWord = "豆包" // 默认唤醒词

    // 初始化语音识别器
    fun init() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    TODO("Not yet implemented")
                }

                override fun onBeginningOfSpeech() {
                    TODO("Not yet implemented")
                }

                override fun onRmsChanged(rmsdB: Float) {
                    TODO("Not yet implemented")
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    TODO("Not yet implemented")
                }

                override fun onEndOfSpeech() {
                    TODO("Not yet implemented")
                }

                override fun onError(error: Int) {
                    TODO("Not yet implemented")
                }

                override fun onResults(results: Bundle) {

                    val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let { spokenText ->
                        Log.d(TAG, "得到结果：$spokenText")
                        if (spokenText.contains(wakeWord, ignoreCase = true)) {// 忽略大小写
                            // 唤醒成功，通知监听者
                            onWakeUpListener?.invoke()
                        }
                    }
                    // 继续监听
                    startListening()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    TODO("Not yet implemented")
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    TODO("Not yet implemented")
                }
            })
        }
        startListening()
    }

    // 开始监听语音
    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINA.toString())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        speechRecognizer?.startListening(intent)
        Log.d(TAG, "开始监听语音")
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        Log.d(TAG, "结束监听语音")
    }

    // 设置自定义唤醒词
    fun setWakeWord(word: String) {
        Log.d(TAG, "设置自定义唤醒词：$word")
        wakeWord = word
        // 保存到偏好设置
        val prefs = context.getSharedPreferences("DoubaoPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("WAKE_WORD", wakeWord).apply()
    }

    // 唤醒事件监听
    var onWakeUpListener: (() -> Unit)? = null

    // 释放资源
    fun release() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}