package com.example.demo1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/*
* 实现语音转文本并与豆包 API 交互
*
* */
class ConversationManager(private val context: Context) {
    private val TAG = "ConversationManager"
    private val speechRecognizer: SpeechRecognizer? = SpeechRecognizer.createSpeechRecognizer(context)
    private val textToSpeech: TextToSpeech by lazy {
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.CHINA
            }
        }
    }

    // 开始语音识别
    fun startListening(onResult: (String) -> Unit) {
        Log.d(TAG, "开始语音识别")

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "onReadyForSpeech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech")
                // 实现空方法体，避免抛出异常
            }

            override fun onRmsChanged(rmsdB: Float) {
                Log.d(TAG, "onRmsChanged")
                // 可以实现音量变化监听，这里暂时忽略
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                Log.d(TAG, "onBufferReceived")
                // 可以处理原始音频数据，这里暂时忽略
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech")
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "音频录制错误"
                    SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
                    SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                    SpeechRecognizer.ERROR_NO_MATCH -> "没有匹配结果"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器繁忙"
                    SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音超时"
                    else -> "未知错误: $error"
                }
                Log.e(TAG, "onError: $errorMessage")
            }

            override fun onResults(results: Bundle) {
                Log.d(TAG, "onResults")
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let(onResult)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                Log.d(TAG, "onPartialResults")
                // 可以处理部分结果，这里暂时忽略
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.d(TAG, "onEvent")
                // 可以处理自定义事件，这里暂时忽略
            }
        })

        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        Log.d(TAG, "结束语音识别")
    }

    // 发送消息到豆包API并获取回复
    suspend fun sendMessageToDoubao(message: String): String {
        Log.d(TAG, "发送消息到豆包API并获取回复 sendMessageToDoubao: $message")
        return try {
            val request = ChatRequest(messages = listOf(Message("user", message)))
            val response = ApiClient.doubaoService.sendMessage(request)
            if (response.isSuccessful) {
                response.body()?.choices?.firstOrNull()?.message?.content ?: "抱歉，没有获取到回复"
            } else {
                "请求失败: ${response.code()}"
            }
        } catch (e: Exception) {
            "发生错误: ${e.message}"
        }
    }

    // 朗读文本
    fun speakText(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    // 释放资源
    fun release() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        textToSpeech.shutdown()
    }
}