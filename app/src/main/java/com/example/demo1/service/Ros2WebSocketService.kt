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
import com.example.demo1.data.entity.Position
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
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.*

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
    private val currentNavUUID = AtomicReference("")
    private val isBusy = AtomicReference(false)
    private val lock = Any()
    private lateinit var currentPosition: PointData
    private var listener: MapDataListener? = null

    // 地图数据结构
    data class MapData(
        val resolution: Double,
        val width: Double,
        val height: Double,
        val originX: Double,
        val originY: Double,
        val data: ByteArray
    )

    fun setMapDataListener(listener: MapDataListener) {
        this.listener = listener
    }

    // 回调接口
    interface MapDataListener {
        fun onMapDataReceived(mapData: MapData)
        fun onScanDataReceived(scanData: String)
        fun onConnectionStatusChanged(isConnected: Boolean, message: String)
    }

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

    data class PointData(
        var x: Double,
        var y: Double,
        var z: Double,
        var yaw: Double
    )

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        executorService = Executors.newSingleThreadScheduledExecutor()
        instance = this
        currentPosition = PointData(
            x = 0.0,
            y = 0.0,
            z = 0.0,
            yaw = 0.0,
        )
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
//                    Log.d(TAG, "Received message: $message")
                    message?.let { listeners.forEach { listener -> listener.onMessageReceived(it) } }
                    val json = JSONObject(message)
                    when (json.optString("topic")) {
                        "/navigate_to_pose/_action/feedback" -> {
                            val feedback = json.getJSONObject("msg")
//                            val distance = feedback.getJSONObject("feedback")
//                                .getDouble("distance_remaining")
//                            Log.d(TAG, "剩余距离: $distance 米")
                        }
                        "/navigate_to_pose/_action/status" -> {
                            val result = json.getJSONObject("msg")
                            val status_lists = result.getJSONArray("status_list")
                            for (i in 0 until status_lists.length()) {
                                val item = status_lists.getJSONObject(i)
                                val status = item.getInt("status")
                                val goal_info = item.getJSONObject("goal_info")
                                val goal_id = goal_info.getJSONObject("goal_id")
                                val uuid = goal_id.getString("uuid")
                                when (status) {
                                    0 -> {
                                        Log.d(TAG, "$uuid : 未知状态（初始化或未定义）！")
                                    }
                                    1 -> {
                                        Log.d(TAG, "$uuid : 目标已接收，但尚未开始执行！")
                                    }
                                    2 -> {
                                        Log.d(TAG, "$uuid : 正在执行中！")
                                        currentNavUUID.set(uuid)
                                        isBusy.set(true)
                                    }
                                    3 -> {
                                        if (uuid == currentNavUUID.get()) {
                                            Log.d(TAG, "$uuid : 正在取消执行！")
                                        }
                                    }
                                    4 -> {
                                        if (uuid == currentNavUUID.get()) {
                                            Log.d(TAG, "$uuid : 导航成功！")
                                            isBusy.set(false)
                                            currentNavUUID.set("")
                                        }
                                    }
                                    5 -> {
                                        if (uuid == currentNavUUID.get()) {
                                            Log.d(TAG, "$uuid : 执行被用户取消！")
                                        }
                                    }
                                    6 -> {
                                        if (uuid == currentNavUUID.get()) {
                                            Log.d(TAG, "$uuid : 导航失败！")
                                            isBusy.set(false)
                                            currentNavUUID.set("")
                                        }
                                    }
                                }
                            }
//                            val status = result.getJSONObject().getInt("status")
//                            if (status == 4) {  // 4 表示成功
//                                Log.d(TAG, "导航成功！")
//                            }
                        }
                        "/robot_pose" -> {
                            val result = json.getJSONObject("msg")
                            val pose = result.getJSONObject("pose")
                            val position = pose.getJSONObject("position")
                            val orientation = pose.getJSONObject("orientation")
                            val x = position.getDouble("x")
                            val y = position.getDouble("y")

                            val z = orientation.getDouble("z")
                            val w = orientation.getDouble("w")
                            val yaw = quaternionToYaw(z, w)
                            synchronized(lock) {
                                currentPosition.x = x
                                currentPosition.y = y
                                currentPosition.yaw = yaw
                            }
                        }
                        "/map" -> {
                            Log.d(TAG, "Received /map message: $message")
                            val msg = json.getJSONObject("msg")
                            val info = msg.getJSONObject("info")

                            // 解析地图信息
                            val resolution = info.getDouble("resolution")
                            val width = info.getDouble("width")
                            val height = info.getDouble("height")

                            val origin = info.getJSONObject("origin").getJSONObject("position")
                            val originX = origin.getDouble("x")
                            val originY = origin.getDouble("y")

                            // 解析地图数据
                            val data = Gson().fromJson(msg.getString("data"), ByteArray::class.java)

                            // 创建地图数据对象
                            val mapData = MapData(resolution, width, height, originX, originY, data)

                            // 通知监听器
                            listener?.onMapDataReceived(mapData)
                        }
                        "/scan" -> {
                            Log.d(TAG, "Received /scan message: $message")
                            if (message != null) {
                                listener?.onScanDataReceived(message)
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

    fun getBusy(): Boolean = isBusy.get()

    fun getUUID(): String = currentNavUUID.get()

    fun getPosion(): PointData {
        synchronized(lock) {
            return currentPosition
        }
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

    fun sendGoalPose(x: Double, y: Double, z: Double, orientationZ: Double, orientationW: Double) {
        if (isBusy.get()) {
            Log.e(TAG, "当前任务未完成，等待完成")
            return
        }
        Log.d(TAG, "移动到位置：$x, $y")
        val now = Instant.now()
        val sec = now.epochSecond
        val nanosec = now.nano

        try {
            val goalPoseMsg = JSONObject().apply {
                put("op", "publish")
                put("topic", "/goal_pose")

                val msg = JSONObject().apply {
                    val header = JSONObject().apply {
                        val stamp = JSONObject().apply {
                            put("sec", sec)
                            put("nanosec", nanosec)
                        }
                        put("stamp", stamp)
                        put("frame_id", "map")
                    }

                    val pose = JSONObject().apply {
                        val position = JSONObject().apply {
                            put("x", x)
                            put("y", y)
                            put("z", z)
                        }

                        val orientation = JSONObject().apply {
                            put("x", 0.0)
                            put("y", 0.0)
                            put("z", orientationZ)
                            put("w", orientationW)
                        }

                        put("position", position)
                        put("orientation", orientation)
                    }

                    put("header", header)
                    put("pose", pose)
                }

                put("msg", msg)
            }

//            Log.d(TAG, "Sending goal_pose: ${goalPoseMsg.toString(2)}")
            webSocketClient?.send(goalPoseMsg.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending goal pose: ${e.message}")
        }
    }

    // 发送 /navigate_to_pose Action 目标
    fun sendNavigateToPoseGoal(x: Double, y: Double, theta: Double) {
        val now = Instant.now()
        val sec = now.epochSecond
        val nanosec = now.nano
        // 构造 Action Goal 消息
        val goalMsg = JSONObject().apply {
            put("op", "publish")
            put("topic", "/navigate_to_pose/_action/send_goal")
            put("msg", JSONObject().apply {
                put("goal", JSONObject().apply {
                    put("pose", JSONObject().apply {
                        put("header", JSONObject().apply {
                            put("stamp", JSONObject().apply {
                                put("sec", sec)
                                put("nanosec", nanosec)
                            })
                            put("frame_id", "map")
                        })
                        put("pose", JSONObject().apply {
                            put("position", JSONObject().apply {
                                put("x", x)
                                put("y", y)
                                put("z", 0.0)
                            })
                            put("orientation", JSONObject().apply {
                                put("x", 0.0)
                                put("y", 0.0)
                                put("z", Math.sin(theta / 2))
                                put("w", Math.cos(theta / 2))
                            })
                        })
                    })
                    put("behavior_tree", "")
                })
                put("uuid", JSONObject().apply {
                    // 生成唯一 ID（示例）
                    put("data", JSONArray().apply {
                        put(1)
                        put(2)
                        put(3)
                        put(4)
                    })
                })
            })
        }
        webSocketClient?.send(goalMsg.toString())
    }

    // 初始化时订阅反馈和结果
    fun subscribeToActionTopics() {
        Log.d(TAG, "subscribeToActionTopics")
//        val subscribeFeedback = JSONObject().apply {
//            put("op", "subscribe")
//            put("topic", "/navigate_to_pose/_action/feedback")
//            put("type", "nav2_msgs/action/NavigateToPose_FeedbackMessage")
//        }
//        webSocketClient?.send(subscribeFeedback.toString())

        val subscribeResult = JSONObject().apply {
            put("op", "subscribe")
            put("topic", "/navigate_to_pose/_action/status")
            put("type", "action_msgs/msg/GoalStatusArray")
        }
        webSocketClient?.send(subscribeResult.toString())

        val subscribeRobotPose = JSONObject().apply {
            put("op", "subscribe")
            put("topic", "/robot_pose")
            put("type", "geometry_msgs/msg/PoseStamped")
        }
        webSocketClient?.send(subscribeRobotPose.toString())
    }

    fun subscribeMapTopic() {
        val subscribeRobotMap = JSONObject().apply {
            put("op", "subscribe")
            put("topic", "/map")
            put("type", "nav_msgs/msg/OccupancyGrid")
        }
        webSocketClient?.send(subscribeRobotMap.toString())

        val subscribeRobotScan = JSONObject().apply {
            put("op", "subscribe")
            put("topic", "/scan")
            put("type", "sensor_msgs/msg/LaserScan")
        }
        webSocketClient?.send(subscribeRobotScan.toString())
    }

    fun cancelGoal() {
        val cancelMsg = JSONObject().apply {
            put("op", "cancel_action_goal")
            put("action", "/navigate_to_pose")
            put("id", "目标ID")  // 需保存发送目标时返回的 ID
        }
        webSocketClient?.send(cancelMsg.toString())
    }

    /**
     * 将四元数的 z 和 w 分量转换为 Yaw 角（弧度）
     * @param z 四元数的 z 分量
     * @param w 四元数的 w 分量
     * @return Yaw 角（弧度，范围 [-π, π]）
     */
    fun quaternionToYaw(z: Double, w: Double): Double {
        // 确保四元数归一化
        val norm = sqrt(z * z + w * w)
        val normalizedZ = z / norm
        val normalizedW = w / norm

        // 计算 Yaw
        return 2.0 * atan2(normalizedZ, normalizedW)
    }
}    