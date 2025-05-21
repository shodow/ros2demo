package com.example.demo1.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.demo1.R
import com.example.demo1.databinding.ActivityMapBinding
import com.example.demo1.data.entity.Position
import com.example.demo1.service.Ros2WebSocketService
import com.example.demo1.ui.viewmodel.PositionViewModel
import com.example.demo1.ui.viewmodel.WebSocketViewModel
import dagger.hilt.android.AndroidEntryPoint
//import org.ros.android.MessageCallable
//import org.ros.android.view.RosImageView
//import org.ros.node.NodeConfiguration
//import org.ros.node.NodeMainExecutor
import java.net.URI

@AndroidEntryPoint
class MapActivity : AppCompatActivity() {
    private var TAG = "MapActivity"
    private lateinit var binding: ActivityMapBinding
    private lateinit var positionViewModel: PositionViewModel
//    private lateinit var webSocketViewModel: WebSocketViewModel
    private var currentX: Double = 0.0
    private var currentY: Double = 0.0
    private var currentYaw: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        positionViewModel = ViewModelProvider(this).get(PositionViewModel::class.java)
//        webSocketViewModel = ViewModelProvider(this).get(WebSocketViewModel::class.java)

        setupViews()
        setupMapView()

        positionViewModel.allPositions.observe(this, Observer { positions ->
            positions?.let {
                Log.d(TAG, "it = $it")
            }
        })
    }

    private fun setupViews() {
        binding.btnSavePosition.setOnClickListener { saveCurrentPosition() }
        binding.btnSetInitialPose.setOnClickListener { setInitialPose() }
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupMapView() {
        // 这里使用ROSImageView显示地图
        // 实际项目中需要根据具体的ROS2环境进行调整
        // 这里提供一个简化的示例

//        val rosImageView = binding.rosImageView
//        rosImageView.setTopicName("/map")
//        rosImageView.setMessageType("nav_msgs/OccupancyGrid")
//        rosImageView.setMessageToBitmapCallable(
//            MessageCallable { message ->
//                // 这里需要实现将OccupancyGrid消息转换为Bitmap的逻辑
//                // 简化示例，实际应用中需要根据具体情况实现
//                null
//            }
//        )
//
//        // 配置ROS节点
//        val nodeMainExecutor = NodeMainExecutor.newDefault()
//        val nodeConfiguration = NodeConfiguration.newPublic("android_client")
//        nodeConfiguration.setMasterUri(URI("http://192.168.1.100:11311")) // 替换为实际的ROS master地址
//
//        nodeMainExecutor.execute(rosImageView, nodeConfiguration)
    }

    private fun saveCurrentPosition() {
        val positionName = binding.etPositionName.text.toString()
        if (positionName.isEmpty()) {
            Toast.makeText(this, "请输入点位名称", Toast.LENGTH_SHORT).show()
            return
        }

        // 从地图视图获取当前位置（简化示例，实际应用中需要根据具体情况实现）
        val position = Position(
            name = positionName,
            x = currentX,
            y = currentY,
            z = 0.0,
            yaw = currentYaw,
            sequence = 1)

        positionViewModel.insert(position)
        Toast.makeText(this, "点位已保存", Toast.LENGTH_SHORT).show()
    }

    private fun setInitialPose() {
        // 获取当前位置（简化示例，实际应用中需要根据具体情况实现）
        val x = currentX
        val y = currentY
        val z = 0.0
        val yaw = currentYaw

//        // 发送设置初始位置指令
//        webSocketViewModel.sendMessage(
//            Ros2WebSocketService().createRos2Message(
//                "/initialpose",
//                "geometry_msgs/PoseWithCovarianceStamped",
//                mapOf(
//                    "header" to mapOf("frame_id" to "map"),
//                    "pose" to mapOf(
//                        "pose" to mapOf(
//                            "position" to mapOf("x" to x, "y" to y, "z" to z),
//                            "orientation" to mapOf("x" to 0.0, "y" to 0.0, "z" to Math.sin(yaw/2), "w" to Math.cos(yaw/2))
//                        ),
//                        "covariance" to DoubleArray(36) { if (it == 0 || it == 7 || it == 35) 0.001 else 0.0 }
//                    )
//                )
//            )
//        )
        Ros2WebSocketService.getInstance()?.sendInitialPose(-0.04101024825363886, 1.168558691943051, 0.0,
            -0.195191580889, -0.9807651333270637)

        Toast.makeText(this, "已设置初始位置", Toast.LENGTH_SHORT).show()
    }
}    