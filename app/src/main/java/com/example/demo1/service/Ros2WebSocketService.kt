package com.example.demo1.service

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.demo1.R
import com.example.demo1.MainActivity
import com.example.demo1.ROS2Manager
import com.example.demo1.ROS2Manager.Companion
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URISyntaxException
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class Ros2WebSocketService : Service() {
    private val TAG = "Ros2WebSocketService"
    private val binder = LocalBinder()
    private var webSocketClient: WebSocketClient? = null
    private var executorService: ScheduledExecutorService? = null
    private var listeners = mutableListOf<WebSocketListener>()
    private var connectionStatus = ConnectionStatus.DISCONNECTED
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val reconnectDelayBase = 2000L // 2 seconds
    // 单例实例的伴生对象
    companion object {
        private const val TAG = "Ros2WebSocketService"
        private const val NOTIFICATION_CHANNEL_ID = "ros2_websocket_channel"
        private const val NOTIFICATION_ID = 1
        private var instance: Ros2WebSocketService? = null

        // 获取单例实例的方法
        fun getInstance(): Ros2WebSocketService? {
            return instance
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        executorService = Executors.newSingleThreadScheduledExecutor()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val serverUrl = intent?.getStringExtra("serverUrl") ?: ""
        Log.d(TAG, "onStartCommand: $serverUrl")
        if (serverUrl.isNotEmpty() && webSocketClient == null) {
            connectToWebSocket(serverUrl)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "onBind")

        return binder
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")

        super.onDestroy()
        disconnect()
        executorService?.shutdown()
    }

    private fun createNotificationChannel() {
        Log.d(TAG, "createNotificationChannel")

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
        Log.d(TAG, "createNotification")

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
        Log.d(TAG, "connectToWebSocket serverUrl: $serverUrl")

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
                    val json = JSONObject(message)
                    when (json.optString("topic")) {
                        "/navigate_to_pose/_action/feedback" -> {
                            val feedback = json.getJSONObject("msg")
                            val distance = feedback.getJSONObject("current_pose").getJSONObject("pose").getDouble("distance")
                            Log.d(TAG, "剩余距离: $distance 米")
                        }
                        "/navigate_to_pose/_action/result" -> {
                            val result = json.getJSONObject("msg")
                            if (result.getBoolean("success")) {
                                Log.d(TAG, "导航成功！")
                            }
                        }
                    }
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
        Log.d(TAG, "scheduleReconnect")

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
        Log.d(TAG, "disconnect")

        webSocketClient?.close()
        webSocketClient = null
        connectionStatus = ConnectionStatus.DISCONNECTED
    }

    fun sendMessage(message: String) {
        Log.d(TAG, "sendMessage: $message")

        if (connectionStatus == ConnectionStatus.CONNECTED) {
            webSocketClient?.send(message)
        } else {
            Log.w(TAG, "Cannot send message. WebSocket is not connected.")
        }
    }

    fun addListener(listener: WebSocketListener) {
        Log.d(TAG, "addListener")

        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: WebSocketListener) {
        Log.d(TAG, "removeListener")

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
        Log.d(TAG, "createRos2Message, topic: $topic, messageType: $messageType, data: $data")

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
        Log.d(TAG, "sendLoadMapCommand")

        val command = mapOf("command" to "load_map")
        val message = createRos2Message("/robot_command", "std_msgs/String", command)
        sendMessage(message)
    }

    // 设置起始位置指令
    fun sendSetInitialPoseCommand(x: Double, y: Double, z: Double, yaw: Double) {
        Log.d(TAG, "sendSetInitialPoseCommand, X: $x, y: $y, z: $z, yaw: $yaw")

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
        Log.d(TAG, "sendMoveToPositionCommand, X: ${position.x}, y: ${position.y}, z: ${position.z}, yaw: ${position.yaw}")

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
        Log.d(TAG, "sendReturnToHomeCommand")

        val command = mapOf("command" to "return_to_home")
        val message = createRos2Message("/robot_command", "std_msgs/String", command)
        sendMessage(message)
    }

    // 打印方法
    fun print() {
        Log.d("SingletonService", "hello service")
        // 也可以使用 Toast 显示
         Toast.makeText(this, "hello service", Toast.LENGTH_SHORT).show()
    }

    fun sendInitialPose(x: Double, y: Double, z: Double, orientationZ: Double, orientationW: Double) {

        val now = Instant.now()
        val sec = now.epochSecond
        val nanosec = now.nano

        try {
            val initialPoseMsg = JSONObject().apply {
                put("op", "publish")
                put("topic", "/initialpose")
                put("type", "geometry_msgs/PoseWithCovarianceStamped")

                val msg = JSONObject().apply {
                    val header = JSONObject().apply {
                        val stamp = JSONObject().apply {
                            put("sec", sec)
                            put("nanosec", nanosec)
                        }
                        put("stamp", stamp)
                        put("frame_id", "map")
                    }

                    val poseWithCovariance = JSONObject().apply {
                        val pose = JSONObject().apply {
                            val position = JSONObject().apply {
                                put("x", x) // 初始位置x坐标，可根据需要修改
                                put("y", y) // 初始位置y坐标，可根据需要修改
                                put("z", z) // 二维导航中z为0
                            }

                            val orientation = JSONObject().apply {
                                put("x", 0.0)
                                put("y", 0.0)
                                put("z", orientationZ) // 初始朝向四元数z，可根据需要修改
                                put("w", orientationW) // 初始朝向四元数w，可根据需要修改
                            }

                            put("position", position)
                            put("orientation", orientation)
                        }

                        // 协方差矩阵
                        // 修正协方差矩阵格式，使用1x36数组
                        // 确保covariance是一个双精度浮点数数组
                        val covarianceArray = doubleArrayOf(
                            0.25, 0.0, 0.0, 0.0, 0.0, 0.0, // 对角元素 (0,0) 为 0.25，表示 x 方向的位置不确定性较大
                            0.0, 0.25, 0.0, 0.0, 0.0, 0.0, // 对角元素 (1,1) 为 0.25，表示 y 方向的位置不确定性较大
                            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, // 其余非对角元素为 0，表示各维度之间没有相关性
                            0.0, 0.0, 0.0, 0.0001, 0.0, 0.0,
                            0.0, 0.0, 0.0, 0.0, 0.0001, 0.0,
                            0.0, 0.0, 0.0, 0.0, 0.0, 0.06853891945200942 // 最后一个对角元素 (5,5) 为 0.06853891945200942，表示朝向 (z 轴旋转) 的不确定性较小
                        )

                        val covariance = JSONArray()
                        covarianceArray.forEach { covariance.put(it) }

                        put("pose", pose)
                        put("covariance", covariance)
                    }

                    put("header", header)
                    put("pose", poseWithCovariance)
                }

                put("msg", msg)
            }

            Log.d(TAG, "Sending initial pose: ${initialPoseMsg.toString(2)}")
            webSocketClient?.send(initialPoseMsg.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending initial pose: ${e.message}")
        }
    }

    // 发送 /navigate_to_pose Action 目标
    fun sendNavigateToPoseGoal(x: Double, y: Double, theta: Double) {
        val goalMsg = JSONObject().apply {
            put("op", "send_action_goal")
            put("action", "/navigate_to_pose")
            put("type", "nav2_msgs/action/NavigateToPose")
            put("args", JSONObject().apply {
                put("pose", JSONObject().apply {
                    put("header", JSONObject().apply {
                        put("frame_id", "map")
                    })
                    put("pose", JSONObject().apply {
                        put("position", JSONObject().apply {
                            put("x", x)
                            put("y", y)
                            put("z", 0.0)
                        })
                        put("orientation", JSONObject().apply {
                            put("z", Math.sin(theta / 2))  // 四元数转换
                            put("w", Math.cos(theta / 2))
                        })
                    })
                })
            })
        }
        Log.d(TAG, "goalMsg: ${goalMsg.toString()}")
        webSocketClient?.send(goalMsg.toString())
    }

    fun cancelGoal() {
        val cancelMsg = JSONObject().apply {
            put("op", "cancel_action_goal")
            put("action", "/navigate_to_pose")
            put("id", "目标ID")  // 需保存发送目标时返回的 ID
        }
        webSocketClient?.send(cancelMsg.toString())
    }
}    