package com.example.demo1.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.demo1.service.Ros2WebSocketService

class WebSocketViewModel : ViewModel() {
    private val _connectionStatus = MutableLiveData<Ros2WebSocketService.ConnectionStatus>()
    val connectionStatus: LiveData<Ros2WebSocketService.ConnectionStatus> = _connectionStatus

    private val _receivedMessages = MutableLiveData<String>()
    val receivedMessages: LiveData<String> = _receivedMessages

    private val _error = MutableLiveData<Exception?>()
    val error: LiveData<Exception?> = _error

    private var webSocketService: Ros2WebSocketService? = null

    private val webSocketListener = object : Ros2WebSocketService.WebSocketListener {
        override fun onConnected() {
            _connectionStatus.postValue(Ros2WebSocketService.ConnectionStatus.CONNECTED)
        }

        override fun onDisconnected() {
            _connectionStatus.postValue(Ros2WebSocketService.ConnectionStatus.DISCONNECTED)
        }

        override fun onMessageReceived(message: String) {
            _receivedMessages.postValue(message)
        }

        override fun onError(error: Exception?) {
            _error.postValue(error)
        }
    }

    fun setWebSocketService(service: Ros2WebSocketService) {
        if (webSocketService != null) {
            webSocketService?.removeListener(webSocketListener)
        }
        webSocketService = service
        webSocketService?.addListener(webSocketListener)
        _connectionStatus.value = webSocketService?.getConnectionStatus()
    }

    fun sendMessage(message: String) {
        webSocketService?.sendMessage(message)
    }

    fun disconnect() {
        webSocketService?.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        webSocketService?.removeListener(webSocketListener)
    }
}    