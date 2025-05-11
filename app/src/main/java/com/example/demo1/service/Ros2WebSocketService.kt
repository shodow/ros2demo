package com.example.demo1.service

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.demo1.R
import com.example.demo1.MainActivity
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.net.URISyntaxException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class Ros2WebSocketService : Service() {
    private val binder = LocalBinder()
    private var webSocketClient: WebSocketClient? = null
    private var executorService: ScheduledExecutorService? = null
    private var listeners = mutableListOf<WebSocketListener>()
    private var connectionStatus = ConnectionStatus.DISCONNECTED
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val reconnectDelayBase = 2000L // 2 seconds

    companion object {
        private const val TAG = "Ros2WebSocketService"
        private const val NOTIFICATION_CHANNEL_ID = "ros2_websocket_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        executorService = Executors.newSingleThreadScheduledExecutor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val serverUrl = intent?.getStringExtra("serverUrl") ?: ""
        if (serverUrl.isNotEmpty() && webSocketClient == null) {
            connectToWebSocket(serverUrl)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
        executorService?.shutdown()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "ROS2 WebSocket Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("ROS2 WebSocket")
            .setContentText("Connected")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun connectToWebSocket(serverUrl: String) {
        try {
            webSocketClient = object : WebSocketClient(URI(serverUrl)) {
                override fun onOpen(handshakedata: ServerHandshake?) {
                    Log.d(TAG, "WebSocket connection opened")
                    connectionStatus = ConnectionStatus.CONNECTED
                    reconnectAttempts = 0
                    listeners.forEach { it.onConnected() }
                }

                override fun onMessage(message: String?) {
                    Log.d(TAG, "Received message: $message")
                    message?.let { listeners.forEach { listener -> listener.onMessageReceived(it) } }
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    Log.d(TAG, "WebSocket connection closed: Code=$code, Reason=$reason")
                    connectionStatus = ConnectionStatus.DISCONNECTED
                    listeners.forEach { it.onDisconnected() }
                    scheduleReconnect()
                }

                override fun onError(ex: Exception?) {
                    Log.e(TAG, "WebSocket error: ${ex?.message}", ex)
                    connectionStatus = ConnectionStatus.ERROR
                    listeners.forEach { it.onError(ex) }
                    scheduleReconnect()
                }
            }
            webSocketClient?.connect()
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Invalid WebSocket URI: ${e.message}", e)
            listeners.forEach { it.onError(e) }
        }
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts < maxReconnectAttempts) {
            val delay = reconnectDelayBase * (1 shl reconnectAttempts)
            executorService?.schedule({
                if (connectionStatus == ConnectionStatus.DISCONNECTED || connectionStatus == ConnectionStatus.ERROR) {
                    Log.d(TAG, "Scheduling reconnect attempt ${reconnectAttempts + 1} in ${delay / 1000} seconds")
                    webSocketClient?.connect()
                    reconnectAttempts++
                }
            }, delay, TimeUnit.MILLISECONDS)
        } else {
            Log.d(TAG, "Max reconnect attempts reached")
        }
    }

    fun disconnect() {
        webSocketClient?.close()
        webSocketClient = null
        connectionStatus = ConnectionStatus.DISCONNECTED
    }

    fun sendMessage(message: String) {
        if (connectionStatus == ConnectionStatus.CONNECTED) {
            webSocketClient?.send(message)
        } else {
            Log.w(TAG, "Cannot send message. WebSocket is not connected.")
        }
    }

    fun addListener(listener: WebSocketListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: WebSocketListener) {
        listeners.remove(listener)
    }

    fun getConnectionStatus() = connectionStatus

    inner class LocalBinder : Binder() {
        val service: Ros2WebSocketService
            get() = this@Ros2WebSocketService
    }

    interface WebSocketListener {
        fun onConnected()
        fun onDisconnected()
        fun onMessageReceived(message: String)
        fun onError(error: Exception?)
    }

    enum class ConnectionStatus {
        CONNECTED, DISCONNECTED, CONNECTING, ERROR
    }

    // 定义ROS2消息格式
    fun createRos2Message(topic: String, messageType: String, data: Any): String {
        val jsonObject = JsonObject()
        jsonObject.addProperty("op", "publish")
        jsonObject.addProperty("topic", topic)
        
        val messageObj = JsonObject()
        val gson = Gson()
        val dataJson = gson.toJsonTree(data)
        messageObj.add("msg", dataJson)
        
        jsonObject.add("msg", messageObj)
        return jsonObject.toString()
    }

    // 加载地图指令
    fun sendLoadMapCommand() {
        val command = mapOf("command" to "load_map")
        val message = createRos2Message("/robot_command", "std_msgs/String", command)
        sendMessage(message)
    }

    // 设置起始位置指令
    fun sendSetInitialPoseCommand(x: Double, y: Double, z: Double, yaw: Double) {
        val pose = mapOf(
            "position" to mapOf("x" to x, "y" to y, "z" to z),
            "orientation" to mapOf("x" to 0.0, "y" to 0.0, "z" to Math.sin(yaw/2), "w" to Math.cos(yaw/2))
        )
        val command = mapOf("command" to "set_initial_pose", "pose" to pose)
        val message = createRos2Message("/robot_command", "std_msgs/String", command)
        sendMessage(message)
    }

    // 移动到指定点位指令
    fun sendMoveToPositionCommand(position: com.example.demo1.data.entity.Position) {
        val pose = mapOf(
            "position" to mapOf("x" to position.x, "y" to position.y, "z" to position.z),
            "orientation" to mapOf("x" to 0.0, "y" to 0.0, "z" to Math.sin(position.yaw/2), "w" to Math.cos(position.yaw/2))
        )
        val command = mapOf("command" to "move_to_position", "pose" to pose)
        val message = createRos2Message("/robot_command", "std_msgs/String", command)
        sendMessage(message)
    }

    // 回到起始位置指令
    fun sendReturnToHomeCommand() {
        val command = mapOf("command" to "return_to_home")
        val message = createRos2Message("/robot_command", "std_msgs/String", command)
        sendMessage(message)
    }
}    