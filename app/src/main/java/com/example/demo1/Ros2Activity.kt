package com.example.demo1

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.google.type.Date
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.time.Instant
import org.json.JSONObject

class Ros2Activity : AppCompatActivity() {
    private lateinit var webSocketClient: WebSocketClient
    private val gson = Gson()
    private val rosTopic = "/joy"
    private val messageType = "std_msgs/String"

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
        initWebSocket()

        // 发布消息按钮
        findViewById<Button>(R.id.btn_publish).setOnClickListener {
//            val message = findViewById<EditText>(R.id.et_message).text.toString()
            publishMessage(0)
        }
        // up
        findViewById<Button>(R.id.up).setOnClickListener {
//            val message = findViewById<EditText>(R.id._up).text.toString()
            publishMessage(1)
        }
        // down
        findViewById<Button>(R.id.down).setOnClickListener {
//            val message = findViewById<EditText>(R.id.down).text.toString()
            publishMessage(2)
        }
        // left
        findViewById<Button>(R.id.left).setOnClickListener {
//            val message = findViewById<EditText>(R.id.left).text.toString()
            publishMessage(3)
        }
        // right
        findViewById<Button>(R.id.right).setOnClickListener {
//            val message = findViewById<EditText>(R.id.right).text.toString()
            publishMessage(4)
        }

        // 订阅开关按钮
        findViewById<Button>(R.id.btn_toggle_subscribe).setOnClickListener {
            toggleSubscription()
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
              "stamp
              ": {
                "sec":  ${now.epochSecond},
                "nanosec": ${now.nano}
              },
              "frame_id": "joy"
            },
            "axes":  ${axes.asList().toString().replace("[", "").replace("]", "")},
            "buttons":  ${buttons.asList().toString().replace("[", "").replace("]", "")}
          }
        }
    """.trimIndent()
    }

    private fun initWebSocket() {
        val uri = URI("ws://192.168.1.16:9090") // 替换为你的ROS IP
        webSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                runOnUiThread { updateStatus("Connected to ROS") }
            }

            override fun onMessage(message: String?) {
                message?.let { handleRosMessage(it) }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                runOnUiThread { updateStatus("Disconnected: $reason") }
            }

            override fun onError(ex: Exception?) {
                runOnUiThread { updateStatus("Error: ${ex?.message}") }
            }
        }
        webSocketClient.connect()
    }

    fun sendMoveCommand(linearX: Double, angularZ: Double) {
        val cmdVelMsg = JSONObject().apply {
            put("op", "publish")
            put("topic", "/cmd_vel")
            put("msg", JSONObject().apply {
                // 构建符合 geometry_msgs/Twist 的数据结构
                put("linear", JSONObject().apply {
                    put("x", linearX)
                    put("y", 0.0)
                    put("z", 0.0)
                })
                put("angular", JSONObject().apply {
                    put("x", 0.0)
                    put("y", 0.0)
                    put("z", angularZ)
                })
            })
        }

        // 添加调试日志（实际发布时建议移除）
        Log.d("TAG", "Sending cmd_vel: ${cmdVelMsg.toString(2)}")
        webSocketClient.send(cmdVelMsg.toString())
    }

    private fun publishMessage(i: Int) {
        Log.d("TAG", "webSocketClient.isOpen: ${webSocketClient.isOpen}")

            when (i) {
                1 -> {
                    Log.d("TAG", "前进")
                    sendMoveCommand(linearX = 0.5, angularZ = 0.0)
                }
                2 -> {
                    Log.d("TAG", "后退")
                }
                3 -> {
                    Log.d("TAG", "左转")
                    sendMoveCommand(linearX = 0.2, angularZ = 0.5)
                }
                4 -> {
                    Log.d("TAG", "右转")
                }
                else -> {
                    Log.d("TAG", "x")
                    // 停止
                    sendMoveCommand(linearX = 0.0, angularZ = 0.0)
                }
            }
    }

    private var isSubscribed = false
    private fun toggleSubscription() {
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
        webSocketClient.send(subscribeMsg)
        findViewById<Button>(R.id.btn_toggle_subscribe).text =
            if (isSubscribed) "Unsubscribe" else "Subscribe"
    }

    private fun handleRosMessage(message: String) {
        try {
            val jsonObject = gson.fromJson(message, Map::class.java)
            if (jsonObject["topic"] == rosTopic) {
                val msgData = (jsonObject["msg"] as Map<*, *>)["data"] as String
                runOnUiThread {
                    findViewById<TextView>(R.id.tv_received).text = "Received: $msgData"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateStatus(text: String) {
        findViewById<TextView>(R.id.tv_status).text = text
    }

    override fun onDestroy() {
        webSocketClient.close()
        super.onDestroy()
    }
}