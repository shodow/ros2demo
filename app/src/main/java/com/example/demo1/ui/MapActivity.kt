package com.example.demo1.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import kotlin.math.max
import kotlin.math.min

@AndroidEntryPoint
class MapActivity : AppCompatActivity() {
    private var TAG = "MapActivity"
    private lateinit var binding: ActivityMapBinding
    private lateinit var positionViewModel: PositionViewModel
//    private lateinit var webSocketViewModel: WebSocketViewModel
    private var currentX: Double = 0.0
    private var currentY: Double = 0.0
    private var currentYaw: Double = 0.0

    private lateinit var mapImageView: ImageView

    // 地图数据结构
    private var mapResolution: Float = 0f
    private var mapWidth: Int = 0
    private var mapHeight: Int = 0
    private var mapOriginX: Double = 0.0
    private var mapOriginY: Double = 0.0
    private var mapData: ByteArray = byteArrayOf()

    // 位图相关
    private var mapBitmap: Bitmap? = null
    private var scaleFactor = 1.0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var posX = 0f
    private var posY = 0f
    private lateinit var scaleDetector: ScaleGestureDetector

    // 原点标记位置
    private var originXPixel = 0f
    private var originYPixel = 0f

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

        mapImageView = findViewById(R.id.mapImageView)

        // 初始化手势检测器
        scaleDetector = ScaleGestureDetector(this, ScaleListener())

        // 设置触摸监听器实现平移和缩放
        mapImageView.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!scaleDetector.isInProgress) {
                        val dx = event.x - lastTouchX
                        val dy = event.y - lastTouchY

                        // 限制平移范围，防止移出视图
                        val newPosX = posX + dx
                        val newPosY = posY + dy

                        // 计算边界
                        val scaledWidth = (mapWidth * scaleFactor)
                        val scaledHeight = (mapHeight * scaleFactor)
                        val maxX = max(0f, (scaledWidth - mapImageView.width) / 2)
                        val maxY = max(0f, (scaledHeight - mapImageView.height) / 2)

                        posX = newPosX.coerceIn(-maxX, maxX)
                        posY = newPosY.coerceIn(-maxY, maxY)

                        updateMapDisplay()

                        lastTouchX = event.x
                        lastTouchY = event.y
                    }
                }
            }
            true
        }
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
            yaw_z = Math.sin(currentYaw/2),
            yaw_w = Math.cos(currentYaw/2),
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

    private fun updateMapInfo() {
//        resolutionText.text = "Resolution: ${mapResolution} m/pixel"
//        dimensionsText.text = "Dimensions: ${mapWidth} x ${mapHeight} pixels"
//        originText.text = "Origin: (${"%.2f".format(mapOriginX)}, ${"%.2f".format(mapOriginY)})"
    }

    private fun createMapBitmap() {
        if (mapWidth == 0 || mapHeight == 0) return

        // 创建位图
        mapBitmap = Bitmap.createBitmap(mapWidth, mapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mapBitmap!!)

        // 设置背景为透明
        canvas.drawColor(Color.TRANSPARENT)

        // 创建画笔
        val paint = Paint().apply {
            isAntiAlias = false
        }

        // 绘制地图
        for (y in 0 until mapHeight) {
            for (x in 0 until mapWidth) {
                val index = y * mapWidth + x
                if (index < mapData.size) {
                    val value = mapData[index].toInt() and 0xFF // 转换为无符号值

                    // 设置颜色
                    when {
                        value == -1 -> paint.color = Color.argb(200, 128, 128, 128) // 未知区域（半透明灰）
                        value > 65 -> paint.color = Color.argb(220, 0, 0, 0)       // 占用区域（黑色）
                        else -> paint.color = Color.argb(180, 255, 255, 255)        // 空闲区域（半透明白）
                    }

                    // 绘制像素
                    canvas.drawPoint(x.toFloat(), (mapHeight - 1 - y).toFloat(), paint)
                }
            }
        }

        // 标记原点（红色）
        paint.color = Color.RED
        paint.strokeWidth = 4f
        canvas.drawCircle(originXPixel, mapHeight - 1 - originYPixel, 5f, paint)
    }

    private fun updateMapDisplay() {
        if (mapBitmap == null) return

        // 创建新的位图用于显示
        val displayBitmap = Bitmap.createBitmap(
            mapImageView.width,
            mapImageView.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(displayBitmap)
        canvas.drawColor(Color.LTGRAY) // 背景色

        // 计算缩放和平移后的位置
        val matrix = android.graphics.Matrix().apply {
            postScale(scaleFactor, scaleFactor)
            postTranslate(
                mapImageView.width / 2 + posX,
                mapImageView.height / 2 + posY
            )
        }

        // 绘制地图
        canvas.drawBitmap(mapBitmap!!, matrix, Paint())

        // 设置到ImageView
        mapImageView.setImageBitmap(displayBitmap)
    }

    private fun resetMapView() {
        scaleFactor = 1.0f
        posX = 0f
        posY = 0f
        updateMapDisplay()
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor

            // 限制缩放范围
            scaleFactor = max(0.1f, min(scaleFactor, 5.0f))

            updateMapDisplay()
            return true
        }
    }
}