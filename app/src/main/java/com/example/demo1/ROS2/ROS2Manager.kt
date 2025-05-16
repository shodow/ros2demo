package com.example.demo1

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.time.Instant
import java.util.function.Consumer

class ROS2Manager(
    private val rosBridgeUrl: String,
    private var rosTopic: String,
    private var messageType: String
) {
    companion object {
        private const val TAG = "ROS2Manager"
    }

    private var webSocketClient: WebSocketClient? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isSubscribed = false
    private var messageCallback: Consumer<String>? = null
    private var statusTextView: TextView? = null
    private var receivedTextView: TextView? = null

    fun setStatusTextView(textView: TextView) {
        this.statusTextView = textView
    }

    fun setReceivedTextView(textView: TextView) {
        this.receivedTextView = textView
    }

    fun setMessageCallback(callback: Consumer<String>) {
        this.messageCallback = callback
    }

    fun initWebSocket() {
        try {
            val uri = URI.create(rosBridgeUrl)
            webSocketClient = object : WebSocketClient(uri) {
                override fun onOpen(handshakedata: ServerHandshake?) {
                    updateStatus("Connected to ROS")
                }

                override fun onMessage(message: String?) {
                    message?.let { handleRosMessage(it) }
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    updateStatus("Disconnected: $reason")
                }

                override fun onError(ex: Exception?) {
                    updateStatus("Error: ${ex?.message}")
                }
            }
            webSocketClient?.connect()
        } catch (e: Exception) {
            Log.e(TAG, "WebSocket initialization error: ${e.message}")
        }
    }

    fun sendMoveCommand(linearX: Double, angularZ: Double) {
        if (!isWebSocketOpen()) return

        try {
            val cmdVelMsg = JSONObject().apply {
                put("op", "publish")
                put("topic", "/cmd_vel")

                val msg = JSONObject().apply {
                    val linear = JSONObject().apply {
                        put("x", linearX)
                        put("y", 0.0)
                        put("z", 0.0)
                    }

                    val angular = JSONObject().apply {
                        put("x", 0.0)
                        put("y", 0.0)
                        put("z", angularZ)
                    }

                    put("linear", linear)
                    put("angular", angular)
                }

                put("msg", msg)
            }

            Log.d(TAG, "Sending cmd_vel: ${cmdVelMsg.toString(2)}")
            webSocketClient?.send(cmdVelMsg.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending move command: ${e.message}")
        }
    }

    fun sendGoalPose(x: Double, y: Double, z: Double, orientationZ: Double, orientationW: Double) {
        if (!isWebSocketOpen()) return

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

            Log.d(TAG, "Sending goal_pose: ${goalPoseMsg.toString(2)}")
            webSocketClient?.send(goalPoseMsg.toString())
            updateStatus("Sent goal pose to /goal_pose")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending goal pose: ${e.message}")
        }
    }

    fun sendInitialPose(x: Double, y: Double, z: Double, orientationZ: Double, orientationW: Double) {
        if (!isWebSocketOpen()) return

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
            updateStatus("Sent initial pose to /initialpose")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending initial pose: ${e.message}")
        }
    }

    fun cancelNavigation() {
        if (!isWebSocketOpen()) {
            Log.e(TAG, "WebSocket is not open")
            return
        }

        try {
            val cancelGoalRequest = JSONObject().apply {
                put("op", "call_service")
                put("service", "/cancel_goal")
                put("type", "action_msgs/CancelGoal")

                val args = JSONObject().apply {
                    val goalId = JSONObject().apply {
                        put("uuid", ByteArray(16)) // 空UUID表示取消所有目标
                    }
                    put("goal_id", goalId)
                }

                put("args", args)
            }

            Log.d(TAG, "Sending cancel goal request to /cancel_goal")
            webSocketClient?.send(cancelGoalRequest.toString())
            updateStatus("Cancel request sent to Nav2")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending cancel navigation request: ${e.message}")
        }
    }

    fun toggleSubscription() {
        if (!isWebSocketOpen()) return

        isSubscribed = !isSubscribed
        val subscribeMsg = if (isSubscribed) {
            """
            {
                "op": "subscribe",
                "topic": "$rosTopic",
                "type": "$messageType"
            }
            """.trimIndent()
        } else {
            """
            {
                "op": "unsubscribe",
                "topic": "$rosTopic"
            }
            """.trimIndent()
        }

        webSocketClient?.send(subscribeMsg)
    }

    fun isSubscribed(): Boolean = isSubscribed

    fun isWebSocketOpen(): Boolean = webSocketClient?.isOpen ?: false

    fun closeConnection() {
        webSocketClient?.close()
    }

    private fun handleRosMessage(message: String) {
        try {
            // 这里使用简单的字符串处理，避免引入额外的依赖
            if (message.contains("\"topic\":\"$rosTopic\"")) {
                val dataStart = message.indexOf("\"data\":\"") + 8
                val dataEnd = message.indexOf("\"", dataStart)
                if (dataStart > 8 && dataEnd > dataStart) {
                    val msgData = message.substring(dataStart, dataEnd)
                    messageCallback?.let { callback ->
                        mainHandler.post { callback.accept(msgData) }
                    }
                    receivedTextView?.let { textView ->
                        mainHandler.post { textView.text = "Received: $msgData" }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling ROS message: ${e.message}")
        }
    }

    private fun updateStatus(text: String) {
        Log.d(TAG, text)
        statusTextView?.let { textView ->
            mainHandler.post { textView.text = text }
        }
    }

    // 创建 Joy 消息的 JSON 格式
    fun createJoyMessage(axes: FloatArray, buttons: IntArray): String {
        val now = Instant.now()
        return """
        {
          "op": "publish",
          "topic": "$rosTopic",
          "msg": {
            "header": {
              "stamp": {
                "sec":  ${now.epochSecond},
                "nanosec": ${now.nano}
              },
              "frame_id": "joy"
            },
            "axes":  ${axes.joinToString()},
            "buttons":  ${buttons.joinToString()}
          }
        }
        """.trimIndent()
    }
}