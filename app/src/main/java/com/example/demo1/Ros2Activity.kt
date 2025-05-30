package com.example.demo1

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Ros2Activity : AppCompatActivity() {
    private lateinit var ros2Manager: ROS2Manager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ros2)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 初始化WebSocket连接
//        initWebSocket()
        // 在Activity中初始化ROS2Manager后设置状态显示文本框
        ros2Manager = ROS2Manager("ws://192.168.1.16:9090", "/joy", "std_msgs/String")
        ros2Manager.setStatusTextView(findViewById(R.id.tv_status)) // tv_status是布局文件中的TextView ID
        // 在Activity中设置接收消息的文本框
        ros2Manager.setReceivedTextView(findViewById(R.id.tv_received)) // tv_received是布局文件中的TextView ID
        // 使用Lambda表达式设置回调（Kotlin推荐写法）
        ros2Manager.setMessageCallback { message ->
            // 处理接收到的消息
            Log.d("ROS_MSG", "Received: $message")

            // 示例：根据消息内容执行不同操作
            if (message.contains("emergency")) {
                // 执行紧急操作
            }
        }

        // 启动WebSocket连接
        ros2Manager.initWebSocket()

        // 订阅话题（可选）
//        ros2Manager.toggleSubscription()

        // 发布消息按钮
        findViewById<Button>(R.id.btn_publish).setOnClickListener {
            publishMessage(0)
        }
        // up
        findViewById<Button>(R.id.up).setOnClickListener {
            publishMessage(1)
        }
        // down
        findViewById<Button>(R.id.down).setOnClickListener {
            publishMessage(2)
        }
        // left
        findViewById<Button>(R.id.left).setOnClickListener {
            publishMessage(3)
        }
        // right
        findViewById<Button>(R.id.right).setOnClickListener {
            publishMessage(4)
        }

        // 新增：发送目标位姿按钮A
        findViewById<Button>(R.id.btn_send_goalA).setOnClickListener {
            sendGoalPoseA()
        }

        // 新增：发送目标位姿按钮B
        findViewById<Button>(R.id.btn_send_goalB).setOnClickListener {
            sendGoalPoseB()
        }

        // 新增：发送目标位姿按钮C
        findViewById<Button>(R.id.btn_send_goalC).setOnClickListener {
            sendGoalPoseC()
        }

        // 新增：发送取消移动
        findViewById<Button>(R.id.btn_cancel_goal).setOnClickListener {
            cancelNavigation()
        }

        // 新增：设置初始位置按钮
        findViewById<Button>(R.id.btn_set_initial_pose1).setOnClickListener {
            sendInitialPose()
        }

        // 订阅开关按钮
        findViewById<Button>(R.id.btn_toggle_subscribe).setOnClickListener {
            toggleSubscription()
        }
    }

    private fun publishMessage(i: Int) {
        when (i) {
            1 -> {
                Log.d("TAG", "前进")
                sendMoveCommand(linearX = 0.3, angularZ = 0.0)
            }
            2 -> {
                Log.d("TAG", "后退")
                sendMoveCommand(linearX = -0.3, angularZ = 0.0)
            }
            3 -> {
                Log.d("TAG", "左转")
                sendMoveCommand(linearX = 0.2, angularZ = 0.5)
            }
            4 -> {
                Log.d("TAG", "右转")
                sendMoveCommand(linearX = 0.5, angularZ = 0.2)
            }
            else -> {
                Log.d("TAG", "x")
                // 停止
                sendMoveCommand(linearX = 0.0, angularZ = 0.0)
            }
        }
    }

    // 取消导航
    // 在你的Android应用中添加以下函数
    private fun cancelNavigation() {
//        if (!webSocketClient.isOpen) {
//            Log.e("TAG", "WebSocket is not open")
//            return
//        }
//
//        // 构建CancelGoal服务请求
//        val cancelGoalRequest = JSONObject().apply {
//            put("op", "call_service")
//            put("service", "/cancel_goal")
//            put("type", "action_msgs/CancelGoal")
//            put("args", JSONObject().apply {
//                // 可以指定特定的goal_id，或者留空取消所有目标
//                put("goal_id", JSONObject().apply {
//                    put("uuid", ByteArray(16)) // 空UUID表示取消所有目标
//                })
//            })
//        }
//
//        // 发送服务请求
//        Log.d("TAG", "Sending cancel goal request to /cancel_goal")
//        webSocketClient.send(cancelGoalRequest.toString())
//        runOnUiThread { updateStatus("Cancel request sent to Nav2") }
    }

    // 创建 Joy 消息的 JSON 格式
    fun createJoyMessage(axes: FloatArray, buttons: IntArray): String {
//        val now = Instant.now()
//        return """
//        {
//          "op": "publish",
//          "topic": "$rosTopic",
//          "msg": {
//            "header": {
//              "stamp
//              ": {
//                "sec":  ${now.epochSecond},
//                "nanosec": ${now.nano}
//              },
//              "frame_id": "joy"
//            },
//            "axes":  ${axes.asList().toString().replace("[", "").replace("]", "")},
//            "buttons":  ${buttons.asList().toString().replace("[", "").replace("]", "")}
//          }
//        }
//    """.trimIndent()
        return ""
    }

    private fun initWebSocket() {
//        val uri = URI("ws://192.168.1.16:9090") // 替换为你的ROS IP
//        webSocketClient = object : WebSocketClient(uri) {
//            override fun onOpen(handshakedata: ServerHandshake?) {
//                runOnUiThread { updateStatus("Connected to ROS") }
//            }
//
//            override fun onMessage(message: String?) {
//                message?.let { handleRosMessage(it) }
//            }
//
//            override fun onClose(code: Int, reason: String?, remote: Boolean) {
//                runOnUiThread { updateStatus("Disconnected: $reason") }
//            }
//
//            override fun onError(ex: Exception?) {
//                runOnUiThread { updateStatus("Error: ${ex?.message}") }
//            }
//        }
//        webSocketClient.connect()
    }

    fun sendMoveCommand(linearX: Double, angularZ: Double) {
        ros2Manager.sendMoveCommand(linearX, angularZ)
//        val cmdVelMsg = JSONObject().apply {
//            put("op", "publish")
//            put("topic", "/cmd_vel")
//            put("msg", JSONObject().apply {
//                // 构建符合 geometry_msgs/Twist 的数据结构
//                put("linear", JSONObject().apply {
//                    put("x", linearX)
//                    put("y", 0.0)
//                    put("z", 0.0)
//                })
//                put("angular", JSONObject().apply {
//                    put("x", 0.0)
//                    put("y", 0.0)
//                    put("z", angularZ)
//                })
//            })
//        }
//
//        // 添加调试日志（实际发布时建议移除）
//        Log.d("TAG", "Sending cmd_vel: ${cmdVelMsg.toString(2)}")
//        webSocketClient.send(cmdVelMsg.toString())
    }

    // 新增：发送目标C点位姿的函数
    private fun sendGoalPoseA() {
        ros2Manager.sendGoalPose(-0.04101024825363886, 1.168558691943051, 0.0,
            -0.195191580889, -0.9807651333270637)
//        if (!webSocketClient.isOpen) {
//            Log.e("TAG", "WebSocket is not open")
//            return
//        }
//
//        // 获取当前时间戳
//        val now = Instant.now()
//        val sec = now.epochSecond
//        val nanosec = now.nano
//
//        // 构建 PoseStamped 消息
//        val goalPoseMsg = JSONObject().apply {
//            put("op", "publish")
//            put("topic", "/goal_pose")
//            put("msg", JSONObject().apply {
//                put("header", JSONObject().apply {
//                    put("stamp", JSONObject().apply {
//                        put("sec", sec)
//                        put("nanosec", nanosec)
//                    })
//                    put("frame_id", "map")
//                })
//                put("pose", JSONObject().apply {
//                    put("position", JSONObject().apply {
//                        put("x", -0.04101024825363886)  // 目标位置x坐标
//                        put("y", 1.168558691943051)  // 目标位置y坐标
//                        put("z", 0.0)  // 二维导航中z为0
//                    })
//                    put("orientation", JSONObject().apply {
//                        put("x", 0.0)
//                        put("y", 0.0)
//                        put("z", -0.195191580889)  // 目标朝向四元数z
//                        put("w", -0.9807651333270637)  // 目标朝向四元数w
//                    })
//                })
//            })
//        }
//
//        // 发送消息
//        Log.d("TAG", "Sending goal_pose: ${goalPoseMsg.toString(2)}")
//        webSocketClient.send(goalPoseMsg.toString())
//        runOnUiThread { updateStatus("Sent goal pose to /goal_pose") }
    }

    // 新增：发送目标C点位姿的函数
    private fun sendGoalPoseB() {
        ros2Manager.sendGoalPose(0.268054810215088, -0.12052429669809667, 0.0,
            0.2079909601537288, -0.9781307481591252)
//        if (!webSocketClient.isOpen) {
//            Log.e("TAG", "WebSocket is not open")
//            return
//        }
//
//        // 获取当前时间戳
//        val now = Instant.now()
//        val sec = now.epochSecond
//        val nanosec = now.nano
//
//        // 构建 PoseStamped 消息
//        val goalPoseMsg = JSONObject().apply {
//            put("op", "publish")
//            put("topic", "/goal_pose")
//            put("msg", JSONObject().apply {
//                put("header", JSONObject().apply {
//                    put("stamp", JSONObject().apply {
//                        put("sec", sec)
//                        put("nanosec", nanosec)
//                    })
//                    put("frame_id", "map")
//                })
//                put("pose", JSONObject().apply {
//                    put("position", JSONObject().apply {
//                        put("x", 0.268054810215088)  // 目标位置x坐标
//                        put("y", -0.12052429669809667)  // 目标位置y坐标
//                        put("z", 0.0)  // 二维导航中z为0
//                    })
//                    put("orientation", JSONObject().apply {
//                        put("x", 0.0)
//                        put("y", 0.0)
//                        put("z", 0.2079909601537288)  // 目标朝向四元数z
//                        put("w", -0.9781307481591252)  // 目标朝向四元数w
//                    })
//                })
//            })
//        }
//
//        // 发送消息
//        Log.d("TAG", "Sending goal_pose: ${goalPoseMsg.toString(2)}")
//        webSocketClient.send(goalPoseMsg.toString())
//        runOnUiThread { updateStatus("Sent goal pose to /goal_pose") }
    }

    // 新增：发送目标C点位姿的函数
    private fun sendGoalPoseC() {
        ros2Manager.sendGoalPose(3.727192178346432, 0.029695112267967026, 0.0,
            0.06306920804240382, -0.9980091557680741)
//        if (!webSocketClient.isOpen) {
//            Log.e("TAG", "WebSocket is not open")
//            return
//        }
//
//        // 获取当前时间戳
//        val now = Instant.now()
//        val sec = now.epochSecond
//        val nanosec = now.nano
//
//        // 构建 PoseStamped 消息
//        val goalPoseMsg = JSONObject().apply {
//            put("op", "publish")
//            put("topic", "/goal_pose")
//            put("msg", JSONObject().apply {
//                put("header", JSONObject().apply {
//                    put("stamp", JSONObject().apply {
//                        put("sec", sec)
//                        put("nanosec", nanosec)
//                    })
//                    put("frame_id", "map")
//                })
//                put("pose", JSONObject().apply {
//                    put("position", JSONObject().apply {
//                        put("x", 3.727192178346432)  // 目标位置x坐标
//                        put("y", 0.029695112267967026)  // 目标位置y坐标
//                        put("z", 0.0)  // 二维导航中z为0
//                    })
//                    put("orientation", JSONObject().apply {
//                        put("x", 0.0)
//                        put("y", 0.0)
//                        put("z", 0.06306920804240382)  // 目标朝向四元数z
//                        put("w", -0.9980091557680741)  // 目标朝向四元数w
//                    })
//                })
//            })
//        }
//
//        // 发送消息
//        Log.d("TAG", "Sending goal_pose: ${goalPoseMsg.toString(2)}")
//        webSocketClient.send(goalPoseMsg.toString())
//        runOnUiThread { updateStatus("Sent goal pose to /goal_pose") }
    }



    // 新增：初始化位姿的函数
    private fun sendInitialPose() {
        ros2Manager.sendInitialPose(-0.04101024825363886, 1.168558691943051, 0.0,
            -0.195191580889, -0.9807651333270637)
//        if (!webSocketClient.isOpen) {
//            Log.e("TAG", "WebSocket is not open")
//            return
//        }
//
//        // 获取当前时间戳
//        val now = Instant.now()
//        val sec = now.epochSecond
//        val nanosec = now.nano
//
//
//        // 构建 PoseWithCovarianceStamped 消息
//        val initialPoseMsg = JSONObject().apply {
//            put("op", "publish")
//            put("topic", "/initialpose")
//            put("type", "geometry_msgs/PoseWithCovarianceStamped") // 指定消息类型
//            put("msg", JSONObject().apply {
//                put("header", JSONObject().apply {
//                    put("stamp", JSONObject().apply {
//                        put("sec", sec)
//                        put("nanosec", nanosec)
//                    })
//                    put("frame_id", "map")
//                })
//                put("pose", JSONObject().apply {
//                    put("pose", JSONObject().apply {
//                        put("position", JSONObject().apply {
//                            put("x", -0.04101024825363886)
//                            put("y", 1.168558691943051)
//                            put("z", 0.0)
//                        })
//                        put("orientation", JSONObject().apply {
//                            put("x", 0.0)
//                            put("y", 0.0)
//                            put("z", -0.195191580889)
//                            put("w", -0.9807651333270637)
//                        })
//                    })
//                    // 修正协方差矩阵格式，使用1x36数组
//                    // 确保covariance是一个双精度浮点数数组
//                    val covarianceArray = doubleArrayOf(
//                        0.25, 0.0, 0.0, 0.0, 0.0, 0.0, // 对角元素 (0,0) 为 0.25，表示 x 方向的位置不确定性较大
//                        0.0, 0.25, 0.0, 0.0, 0.0, 0.0, // 对角元素 (1,1) 为 0.25，表示 y 方向的位置不确定性较大
//                        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, // 其余非对角元素为 0，表示各维度之间没有相关性
//                        0.0, 0.0, 0.0, 0.0001, 0.0, 0.0,
//                        0.0, 0.0, 0.0, 0.0, 0.0001, 0.0,
//                        0.0, 0.0, 0.0, 0.0, 0.0, 0.06853891945200942 // 最后一个对角元素 (5,5) 为 0.06853891945200942，表示朝向 (z 轴旋转) 的不确定性较小
//                    )
//                    put("covariance", JSONArray(covarianceArray.map { it }))
//                })
//            })
//        }
//        // 构建 PoseWithCovarianceStamped 消息
//        val initialPoseMsg = JSONObject().apply {
//            put("op", "publish")
//            put("topic", "/initialpose")
//            put("msg", JSONObject().apply {
//                put("header", JSONObject().apply {
//                    put("stamp", JSONObject().apply {
//                        put("sec", sec)
//                        put("nanosec", nanosec)
//                    })
//                    put("frame_id", "map")
//                })
//                put("pose", JSONObject().apply {
//                    put("pose", JSONObject().apply {
//                        put("position", JSONObject().apply {
//                            put("x", -0.04101024825363886)
//                            put("y", 1.168558691943051)
//                            put("z", 0.0)
//                        })
//                        put("orientation", JSONObject().apply {
//                            put("x", 0.0)
//                            put("y", 0.0)
//                            put("z", -0.195191580889)
//                            put("w", -0.9807651333270637)
//                        })
//                    })
//                    // 协方差矩阵，12x12矩阵的前6x6部分（简化表示）
//                    put("covariance", listOf(
//                        0.25, 0.0, 0.0, 0.0, 0.0, 0.0,
//                        0.0, 0.25, 0.0, 0.0, 0.0, 0.0,
//                        0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
//                        0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
//                        0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
//                        0.0, 0.0, 0.0, 0.0, 0.0, 0.06853891945200942
//                    ))
//                })
//            })
//        }
//        // 发送消息
//        Log.d("TAG", "Sending initial pose: ${initialPoseMsg.toString(2)}")
//        webSocketClient.send(initialPoseMsg.toString())
//        runOnUiThread { updateStatus("Sent initial pose to /initialpose") }
    }

    private var isSubscribed = false
    private fun toggleSubscription() {
//        isSubscribed = !isSubscribed
//        val subscribeMsg = if (isSubscribed) {
//            """
//            {
//                "op": "subscribe",
//                "topic": "$rosTopic",
//                "type": "$messageType"
//            }
//            """.trimIndent()
//        } else {
//            """
//            {
//                "op": "unsubscribe",
//                "topic": "$rosTopic"
//            }
//            """.trimIndent()
//        }
//        webSocketClient.send(subscribeMsg)
//        findViewById<Button>(R.id.btn_toggle_subscribe).text =
//            if (isSubscribed) "Unsubscribe" else "Subscribe"
    }

    private fun handleRosMessage(message: String) {
//        try {
//            val jsonObject = gson.fromJson(message, Map::class.java)
//            if (jsonObject["topic"] == rosTopic) {
//                val msgData = (jsonObject["msg"] as Map<*, *>)["data"] as String
//                runOnUiThread {
//                    findViewById<TextView>(R.id.tv_received).text = "Received: $msgData"
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
    }

    private fun updateStatus(text: String) {
        findViewById<TextView>(R.id.tv_status).text = text
    }

    override fun onDestroy() {
//        webSocketClient.close()
        ros2Manager.closeConnection()
        super.onDestroy()
    }
}